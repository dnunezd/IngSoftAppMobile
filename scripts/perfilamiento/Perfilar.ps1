<#
.SYNOPSIS
  Automatiza el plan de pruebas de rendimiento de Vinilos (docs/performance/plan-pruebas-performance.md).

.DESCRIPTION
  -Accion Validar   Revisa el dispositivo, las herramientas, el backend y Android Studio. No cambia nada.
  -Accion Preparar  Compila las dos variantes del APK "perf" (profileable y depurable) y el APK de
                    recorridos, instala la profileable y deja listo adb reverse.
  -Accion Ejecutar  Corre el plan: maneja la app, captura datos y pantallas, y se detiene con
                    instrucciones exactas cuando hay que hacer algo a mano en el Profiler.

  Los resultados quedan en docs/performance/resultados/<dispositivo>/.

.EXAMPLE
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Validar
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Escenarios hu01
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Escenarios hu01 -Pasadas telemetria,asignaciones
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Validar', 'Preparar', 'Ejecutar')]
    [string]$Accion,

    # Serial de adb (adb devices). Obligatorio solo si hay más de un dispositivo conectado.
    [string]$Serial,

    # Carpeta del repositorio BackVynils (donde está su docker-compose.yml).
    [string]$RutaBackend,

    # Escenarios a correr, en orden. Por defecto, todos.
    [ValidateSet('hu01', 'hu02', 'hu03', 'hu04', 'hu07', 'hu10', 'caida', 'memoria')]
    [string[]]$Escenarios = @('hu01', 'hu02', 'hu03', 'hu04', 'hu07', 'hu10', 'caida', 'memoria'),

    # Ejecutar: pasadas de cada HU. telemetria = View Live Telemetry; traza = System Trace;
    # asignaciones = Java/Kotlin Allocations + Network Inspector (con el APK depurable).
    [ValidateSet('telemetria', 'traza', 'asignaciones')]
    [string[]]$Pasadas = @('telemetria', 'traza', 'asignaciones'),

    # Ejecutar: no borra los datos de la app al empezar (caché caliente). Queda anotado en el registro.
    [switch]$CacheCaliente,

    # Ejecutar: sin pausas de Android Studio; solo lo que se captura por línea de comandos.
    [switch]$SinStudio
)

$ErrorActionPreference = 'Stop'

$Raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Paquete = 'com.example.vinilos.perf'
$PaqueteRecorridos = 'com.example.vinilos.perfilamiento'
$UrlBackend = 'http://localhost:3000'
if (-not $RutaBackend) { $RutaBackend = Join-Path $Raiz '..\BackVynils' }
# Preparar deja aquí las dos variantes del APK; Ejecutar instala la que necesita cada pasada.
$CarpetaApk = Join-Path $Raiz 'build\perfilamiento'
$ApkVariante = @{
    profileable = Join-Path $CarpetaApk 'vinilos-perf-profileable.apk'
    depurable   = Join-Path $CarpetaApk 'vinilos-perf-depurable.apk'
}

# ------------------------------------------------------------------ utilidades

function Escribir-Titulo([string]$Texto) {
    Write-Host ''
    Write-Host ('━━━ ' + $Texto + ' ━━━') -ForegroundColor Cyan
}

function Escribir-Resultado([string]$Estado, [string]$Texto) {
    $color = @{ 'OK' = 'Green'; 'AVISO' = 'Yellow'; 'FALLA' = 'Red' }[$Estado]
    Write-Host ('  [{0,-5}] {1}' -f $Estado, $Texto) -ForegroundColor $color
}

function Adb {
    # Devuelve la salida como texto; no falla si el comando del dispositivo devuelve error.
    $argumentos = @()
    if ($script:SerialActivo) { $argumentos += @('-s', $script:SerialActivo) }
    $argumentos += $args
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { (& adb.exe @argumentos 2>&1 | Out-String).Trim() } finally { $ErrorActionPreference = $anterior }
}

function Elegir-Dispositivo {
    if (-not (Get-Command adb.exe -ErrorAction SilentlyContinue)) {
        throw 'No se encuentra adb. Agrega <Android SDK>\platform-tools al PATH.'
    }
    $conectados = @(& adb.exe devices | Select-Object -Skip 1 | Where-Object { $_ -match '\sdevice$' } |
        ForEach-Object { ($_ -split '\s+')[0] })
    if ($Serial) {
        if ($conectados -notcontains $Serial) { throw "El dispositivo $Serial no aparece en 'adb devices'." }
        $script:SerialActivo = $Serial
    } elseif ($conectados.Count -eq 1) {
        $script:SerialActivo = $conectados[0]
    } elseif ($conectados.Count -eq 0) {
        throw @"
adb no ve ningún dispositivo.
  Por USB:  activa "Depuración por USB" y acepta la huella en el teléfono.
  Por Wi-Fi: en el teléfono abre Opciones para desarrolladores > Depuración inalámbrica,
             mira "Dirección IP y puerto" y ejecuta:  adb connect <IP>:<puerto>
"@
    } else {
        throw "Hay varios dispositivos ($($conectados -join ', ')). Indica uno con -Serial."
    }
}

function Leer-Dispositivo {
    $memKb = [long]((Adb shell cat /proc/meminfo) -split "`n" | Where-Object { $_ -match '^MemTotal' } |
        ForEach-Object { ($_ -replace '\D', '') } | Select-Object -First 1)
    $soc = Adb shell getprop ro.soc.model
    if (-not $soc) { $soc = Adb shell getprop ro.board.platform }
    if (-not $soc) { $soc = Adb shell getprop ro.hardware }
    $esEmulador = (Adb shell getprop ro.kernel.qemu) -eq '1' -or (Adb shell getprop ro.boot.qemu) -eq '1'
    [pscustomobject]@{
        Fabricante = Adb shell getprop ro.product.manufacturer
        Modelo     = Adb shell getprop ro.product.model
        Procesador = $soc
        Abi        = Adb shell getprop ro.product.cpu.abi
        RamGb      = [math]::Round($memKb / 1MB, 1)
        Android    = Adb shell getprop ro.build.version.release
        Api        = [int](Adb shell getprop ro.build.version.sdk)
        Pantalla   = ((Adb shell wm size) -replace '.*:\s*', '' -split "`n")[-1].Trim()
        Emulador   = $esEmulador
        Serial     = $script:SerialActivo
    }
}

function Nombre-Carpeta($Dispositivo) {
    (("{0}-{1}" -f $Dispositivo.Fabricante, $Dispositivo.Modelo).ToLower() -replace '[^a-z0-9]+', '-').Trim('-')
}

function Probar-Backend {
    try {
        $respuesta = Invoke-WebRequest -Uri "$UrlBackend/albums" -UseBasicParsing -TimeoutSec 8
        return $respuesta.StatusCode -eq 200
    } catch { return $false }
}

function Esperar-Backend([int]$Segundos = 90) {
    $limite = (Get-Date).AddSeconds($Segundos)
    while ((Get-Date) -lt $limite) {
        if (Probar-Backend) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Compose {
    $archivo = Join-Path $RutaBackend 'docker-compose.yml'
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & docker compose -f $archivo @args 2>&1 | Out-Null } finally { $ErrorActionPreference = $anterior }
}

function Info-Paquete([string]$Nombre) {
    $volcado = Adb shell dumpsys package $Nombre
    if ($volcado -notmatch 'versionName=(\S+)') { return $null }
    [pscustomobject]@{
        Version   = $Matches[1]
        Depurable = $volcado -match 'pkgFlags=\[[^\]]*DEBUGGABLE'
    }
}

# ------------------------------------------------------------------ validar

function Validar {
    $fallas = 0
    Escribir-Titulo 'Dispositivo'
    $d = Leer-Dispositivo
    Escribir-Resultado 'OK' ("{0} {1} · {2} ({3}) · {4} GB de RAM · Android {5} (API {6}) · {7}" -f `
            $d.Fabricante, $d.Modelo, $d.Procesador, $d.Abi, $d.RamGb, $d.Android, $d.Api, $d.Pantalla)
    if ($d.Emulador) { Escribir-Resultado 'AVISO' 'Es un emulador: sus cifras de CPU y fluidez no se comparan con las de un teléfono.' }
    if ($d.Serial -match ':\d+$|_adb-tls-connect' -and -not $d.Emulador) {
        Escribir-Resultado 'AVISO' 'Conectado por Wi-Fi: el Profiler en vivo se atrasa. Para medir, mejor USB.'
    }

    $energia = Adb shell dumpsys power
    if ($energia -match 'mWakefulness=Awake') { Escribir-Resultado 'OK' 'Pantalla encendida.' }
    else { Escribir-Resultado 'FALLA' 'La pantalla está apagada: enciéndela y desbloquea el teléfono.'; $fallas++ }
    $ventanas = Adb shell dumpsys window
    if ($ventanas -match 'mDreamingLockscreen=true|isStatusBarKeyguard=true|mShowingLockscreen=true') {
        Escribir-Resultado 'FALLA' 'El teléfono está bloqueado: desbloquéalo.'; $fallas++
    } else { Escribir-Resultado 'OK' 'Teléfono desbloqueado.' }

    $bateria = Adb shell dumpsys battery
    if ($bateria -match 'temperature:\s*(\d+)') {
        $grados = [int]$Matches[1] / 10
        if ($grados -ge 40) { Escribir-Resultado 'AVISO' "Batería a $grados °C: deja enfriar el teléfono antes de medir." }
        else { Escribir-Resultado 'OK' "Batería a $grados °C." }
    }

    Escribir-Titulo 'Herramientas en el dispositivo'
    if ($d.Api -ge 29) { Escribir-Resultado 'OK' 'Admite apps "profileable" (Android 10 o superior).' }
    else { Escribir-Resultado 'AVISO' 'Android anterior a 10: el Profiler solo funciona con la variante depurable (-Pasadas asignaciones).' }
    if ((Adb shell 'command -v perfetto') -match 'perfetto') { Escribir-Resultado 'OK' 'perfetto disponible (trazas del sistema por línea de comandos).' }
    else { Escribir-Resultado 'AVISO' 'Sin perfetto: las trazas del sistema solo se podrán grabar desde Android Studio.' }

    Escribir-Titulo 'Aplicaciones instaladas'
    $app = Info-Paquete $Paquete
    if ($app) {
        $tipo = if ($app.Depurable) { 'depurable' } else { 'profileable' }
        Escribir-Resultado 'OK' "$Paquete $($app.Version) ($tipo)."
    } else { Escribir-Resultado 'FALLA' "$Paquete no está instalado: corre -Accion Preparar."; $fallas++ }
    if (Info-Paquete $PaqueteRecorridos) { Escribir-Resultado 'OK' "$PaqueteRecorridos (recorridos) instalado." }
    else { Escribir-Resultado 'FALLA' "$PaqueteRecorridos no está instalado: corre -Accion Preparar."; $fallas++ }
    foreach ($variante in @('profileable', 'depurable')) {
        if (Test-Path $ApkVariante[$variante]) { Escribir-Resultado 'OK' "APK $variante compilado." }
        else { Escribir-Resultado 'FALLA' "Falta el APK ${variante}: corre -Accion Preparar."; $fallas++ }
    }

    Escribir-Titulo 'Backend local'
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $anterior = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
        & docker info 2>&1 | Out-Null; $dockerArriba = $LASTEXITCODE -eq 0
        $ErrorActionPreference = $anterior
        if ($dockerArriba) { Escribir-Resultado 'OK' 'Docker está corriendo.' }
        else { Escribir-Resultado 'FALLA' 'Docker Desktop no está corriendo: ábrelo y espera a que termine de iniciar.'; $fallas++ }
    } else { Escribir-Resultado 'FALLA' 'No se encuentra docker en el PATH.'; $fallas++ }
    if (Test-Path (Join-Path $RutaBackend 'docker-compose.yml')) { Escribir-Resultado 'OK' "BackVynils en $((Resolve-Path $RutaBackend).Path)." }
    else { Escribir-Resultado 'FALLA' "No hay docker-compose.yml en $RutaBackend. Indica la carpeta con -RutaBackend."; $fallas++ }
    if (Probar-Backend) { Escribir-Resultado 'OK' "$UrlBackend/albums responde 200." }
    else { Escribir-Resultado 'FALLA' "$UrlBackend/albums no responde. En BackVynils: docker compose up -d --build"; $fallas++ }
    if ((Adb reverse --list) -match 'tcp:3000') { Escribir-Resultado 'OK' 'adb reverse tcp:3000 activo.' }
    else { Escribir-Resultado 'AVISO' 'Falta adb reverse tcp:3000 tcp:3000 (Preparar y Ejecutar lo ponen).' }

    Escribir-Titulo 'Android Studio'
    $studio = Get-Process studio64 -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
    if ($studio) { Escribir-Resultado 'OK' "Abierto: $($studio.MainWindowTitle)" }
    else { Escribir-Resultado 'AVISO' 'Android Studio no está abierto (hace falta para las capturas del Profiler).' }

    Write-Host ''
    if ($fallas -eq 0) { Write-Host 'Todo listo para ejecutar el plan.' -ForegroundColor Green }
    else { Write-Host "$fallas punto(s) por resolver antes de ejecutar el plan." -ForegroundColor Red }
    return $fallas
}

# ------------------------------------------------------------------ preparar

function Preparar {
    Escribir-Titulo 'Compilar'
    if (-not $env:JAVA_HOME) { Write-Host '  JAVA_HOME está vacío: Gradle 8.7 necesita un JDK 17 o 21.' -ForegroundColor Yellow }
    $salidaApp = Join-Path $Raiz 'app\build\outputs\apk\perf\app-perf.apk'
    New-Item -ItemType Directory -Force -Path $CarpetaApk | Out-Null
    Push-Location $Raiz
    try {
        # Las dos variantes salen al mismo archivo: se compila una, se copia, y luego la otra.
        & .\gradlew.bat :app:assemblePerf "-PbaseUrl=$UrlBackend/" -PperfDepurable --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'La compilación de la variante depurable falló.' }
        Copy-Item $salidaApp $ApkVariante.depurable -Force
        & .\gradlew.bat :app:assemblePerf :perfilamiento:assemblePerf "-PbaseUrl=$UrlBackend/" --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'La compilación de la variante profileable falló.' }
        Copy-Item $salidaApp $ApkVariante.profileable -Force
    } finally { Pop-Location }

    Escribir-Titulo 'Instalar'
    $apkApp = Get-Item $ApkVariante.profileable
    $apkRecorridos = Get-ChildItem (Join-Path $Raiz 'perfilamiento\build\outputs\apk\perf\*.apk') | Select-Object -First 1
    foreach ($apk in @($apkApp, $apkRecorridos)) {
        $salida = Adb install -r $apk.FullName
        if ($salida -notmatch 'Success') { throw "No se pudo instalar $($apk.Name): $salida" }
        Escribir-Resultado 'OK' "$($apk.Name) instalado."
    }
    Adb reverse tcp:3000 tcp:3000 | Out-Null
    Escribir-Resultado 'OK' 'adb reverse tcp:3000 tcp:3000.'
    $commit = (& git -C $Raiz rev-parse --short HEAD)
    $pendientes = if (& git -C $Raiz status --porcelain --untracked-files=no) { ' más cambios sin confirmar' } else { '' }
    Escribir-Resultado 'OK' "Compilado desde el commit $commit$pendientes."
}

# ------------------------------------------------------------------ pausas guiadas

function Pausa([string]$Titulo, [string[]]$Contexto, [string[]]$Pasos, [string]$Confirmacion) {
    while ($true) {
        Escribir-Titulo "PAUSA · $Titulo"
        foreach ($linea in $Contexto) { Write-Host $linea }
        Write-Host ''
        Write-Host 'En Android Studio:' -ForegroundColor White
        $n = 1
        foreach ($paso in $Pasos) { Write-Host ("  {0}. {1}" -f $n, $paso); $n++ }
        Write-Host ''
        Write-Host $Confirmacion -ForegroundColor White
        $respuesta = Read-Host '[Enter] continuar · [r] repetir instrucciones · [s] saltar este paso · [a] abortar'
        switch ($respuesta.Trim().ToLower()) {
            ''  { return $true }
            's' { return $false }
            'a' { throw 'Ejecución abortada por el usuario.' }
            default { }
        }
    }
}

# ------------------------------------------------------------------ capturas

function Preparar-CapturaDeVentanas {
    if ('Vinilos.Ventanas' -as [type]) { return }
    Add-Type -AssemblyName System.Drawing
    Add-Type -Namespace Vinilos -Name Ventanas -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
[DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
[DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
[DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
[DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
public struct RECT { public int Left, Top, Right, Bottom; }
'@
    [Vinilos.Ventanas]::SetProcessDPIAware() | Out-Null
}

function Capturar-Studio([string]$Destino) {
    Preparar-CapturaDeVentanas
    $studio = Get-Process studio64 -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
    if (-not $studio) { Escribir-Resultado 'AVISO' 'Android Studio no está abierto: no se tomó la captura.'; return }
    $ventana = $studio.MainWindowHandle
    if ([Vinilos.Ventanas]::IsIconic($ventana)) { [Vinilos.Ventanas]::ShowWindow($ventana, 9) | Out-Null }
    [Vinilos.Ventanas]::SetForegroundWindow($ventana) | Out-Null
    Start-Sleep -Milliseconds 700
    $r = New-Object Vinilos.Ventanas+RECT
    [Vinilos.Ventanas]::GetWindowRect($ventana, [ref]$r) | Out-Null
    $ancho = $r.Right - $r.Left; $alto = $r.Bottom - $r.Top
    $imagen = New-Object System.Drawing.Bitmap $ancho, $alto
    $lienzo = [System.Drawing.Graphics]::FromImage($imagen)
    try {
        # PrintWindow dibuja solo la ventana de Studio, aunque otra ventana la tape: así nunca
        # queda en la captura lo que haya en otras aplicaciones. 2 = PW_RENDERFULLCONTENT.
        $contexto = $lienzo.GetHdc()
        try { $dibujada = [Vinilos.Ventanas]::PrintWindow($ventana, $contexto, 2) } finally { $lienzo.ReleaseHdc($contexto) }
        if (-not $dibujada) { Escribir-Resultado 'AVISO' 'No se pudo capturar la ventana de Android Studio.'; return }
        $imagen.Save($Destino, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $lienzo.Dispose(); $imagen.Dispose() }
    Escribir-Resultado 'OK' "Captura de Android Studio: $Destino"
}

function Capturar-Telefono([string]$Destino) {
    Adb shell screencap -p /data/local/tmp/vinilos-captura.png | Out-Null
    Adb pull /data/local/tmp/vinilos-captura.png $Destino | Out-Null
}

function Memoria-Mb {
    $texto = Adb shell dumpsys meminfo $Paquete
    if ($texto -match 'TOTAL(?:\s+PSS:)?\s+(\d+)') { return [math]::Round([int]$Matches[1] / 1024, 1) }
    return $null
}

function Iniciar-Perfetto([string]$Nombre) {
    $ruta = "/data/misc/perfetto-traces/vinilos-$Nombre.perfetto-trace"
    $salida = Adb shell perfetto --background -o $ruta -t 180s -b 64mb -a $Paquete sched freq idle am wm gfx view binder_driver dalvik input res
    if ($salida -match '(\d+)\s*$') { return [pscustomobject]@{ Pid = $Matches[1]; Ruta = $ruta } }
    Escribir-Resultado 'AVISO' "No se pudo iniciar perfetto: $salida"
    return $null
}

function Detener-Perfetto($Traza, [string]$Destino) {
    if (-not $Traza) { return }
    Adb shell kill -TERM $Traza.Pid | Out-Null
    Start-Sleep -Seconds 3
    Adb pull $Traza.Ruta $Destino | Out-Null
    Adb shell rm $Traza.Ruta | Out-Null
    if (Test-Path $Destino) { Escribir-Resultado 'OK' "Traza del sistema: $Destino" }
}

function Correr-Recorrido([string]$Metodo) {
    $salida = Adb shell am instrument -w -e class "$PaqueteRecorridos.Recorridos#$Metodo" "$PaqueteRecorridos/androidx.test.runner.AndroidJUnitRunner"
    if ($salida -match 'OK \(1 test\)') { return $true }
    # Solo el comienzo del error: el resto es la misma traza repetida.
    Write-Host ((($salida -split "`n") | Select-Object -First 12) -join "`n") -ForegroundColor Red
    return $false
}

# ------------------------------------------------------------------ datos del backend

function Quitar-Favoritos {
    # HU10 necesita un artista que todavía no sea favorito: se quitan todos por la API.
    foreach ($coleccionista in (Invoke-RestMethod "$UrlBackend/collectors")) {
        foreach ($artista in (Invoke-RestMethod "$UrlBackend/collectors/$($coleccionista.id)/performers")) {
            foreach ($tipo in @('musicians', 'bands')) {
                try { Invoke-RestMethod -Method Delete "$UrlBackend/collectors/$($coleccionista.id)/$tipo/$($artista.id)" | Out-Null; break } catch { }
            }
        }
    }
}

function Borrar-AlbumesDePrueba {
    # HU07 crea un álbum "Perf <hora>": se borra para que el siguiente dispositivo vea los mismos datos.
    foreach ($album in (Invoke-RestMethod "$UrlBackend/albums" | Where-Object { $_.name -like 'Perf *' })) {
        try { Invoke-RestMethod -Method Delete "$UrlBackend/albums/$($album.id)" | Out-Null } catch { }
    }
}

# ------------------------------------------------------------------ ejecutar

$Definiciones = [ordered]@{
    hu01    = @{ Carpeta = 'HU01'; Titulo = 'HU01 Catálogo de álbumes';           Metodo = 'hu01'; TrazaEnStudio = $false }
    hu02    = @{ Carpeta = 'HU02'; Titulo = 'HU02 Detalle de álbum';              Metodo = 'hu02'; TrazaEnStudio = $false }
    hu03    = @{ Carpeta = 'HU03'; Titulo = 'HU03 Listado y búsqueda de artistas'; Metodo = 'hu03'; TrazaEnStudio = $true }
    hu04    = @{ Carpeta = 'HU04'; Titulo = 'HU04 Detalle de artista';            Metodo = 'hu04'; TrazaEnStudio = $false }
    hu07    = @{ Carpeta = 'HU07'; Titulo = 'HU07 Crear un álbum';                Metodo = 'hu07'; TrazaEnStudio = $true }
    hu10    = @{ Carpeta = 'HU10'; Titulo = 'HU10 Agregar artistas a favoritos';  Metodo = 'hu10'; TrazaEnStudio = $true }
}

function Medir-Escenario($Definicion, [string]$Carpeta, [string]$Pasada, [scriptblock]$Durante) {
    # Corre un recorrido midiendo memoria y fluidez; devuelve la fila para registro.md.
    Adb shell dumpsys gfxinfo $Paquete reset | Out-Null
    $inicial = Memoria-Mb
    $muestreo = Start-Job -ArgumentList $script:SerialActivo, $Paquete -ScriptBlock {
        param($serial, $paquete)
        $maximo = 0
        while ($true) {
            $texto = (& adb.exe -s $serial shell dumpsys meminfo $paquete 2>$null | Out-String)
            if ($texto -match 'TOTAL(?:\s+PSS:)?\s+(\d+)') { $maximo = [math]::Max($maximo, [int]$Matches[1]); Write-Output $maximo }
            Start-Sleep -Seconds 3
        }
    }
    $reloj = [Diagnostics.Stopwatch]::StartNew()
    $exito = & $Durante
    $reloj.Stop()
    $muestras = @(Receive-Job $muestreo); Stop-Job $muestreo; Remove-Job $muestreo
    $final = Memoria-Mb
    $maximo = if ($muestras.Count) { [math]::Round(($muestras | Measure-Object -Maximum).Maximum / 1024, 1) } else { $final }
    $maximo = [math]::Max([double]$maximo, [math]::Max([double]$inicial, [double]$final))

    $fluidez = Adb shell dumpsys gfxinfo $Paquete
    Set-Content -Path (Join-Path $Carpeta "$Pasada-gfxinfo.txt") -Value $fluidez -Encoding UTF8
    $cuadros = if ($fluidez -match 'Total frames rendered:\s*(\d+)') { $Matches[1] } else { '—' }
    $lentos = if ($fluidez -match 'Janky frames:\s*(\d+\s*\([\d.,]+%\))') { $Matches[1] } else { '—' }
    $p90 = if ($fluidez -match '90th percentile:\s*(\d+ms)') { $Matches[1] } else { '—' }
    Capturar-Telefono (Join-Path $Carpeta "$Pasada-telefono.png")

    [pscustomobject]@{
        Escenario = $Definicion.Titulo; Pasada = $Pasada; Exito = $exito
        Segundos = [math]::Round($reloj.Elapsed.TotalSeconds); Inicial = $inicial; Maximo = $maximo; Final = $final
        Cuadros = $cuadros; Lentos = $lentos; P90 = $p90
    }
}

function Preparar-Variante([string]$Variante) {
    # Instala la variante pedida (si no es la que ya está) y deja la app recién abierta.
    if (-not (Test-Path $ApkVariante[$Variante])) { throw "Falta el APK ${Variante}: corre -Accion Preparar." }
    $instalada = Info-Paquete $Paquete
    $actual = if ($instalada -and $instalada.Depurable) { 'depurable' } elseif ($instalada) { 'profileable' } else { '' }
    Escribir-Titulo "APK $Variante"
    if ($actual -ne $Variante) {
        $salida = Adb install -r $ApkVariante[$Variante]
        if ($salida -notmatch 'Success') { throw "No se pudo instalar la variante ${Variante}: $salida" }
        Escribir-Resultado 'OK' "Variante $Variante instalada."
    } else { Escribir-Resultado 'OK' "La variante $Variante ya estaba instalada." }
    Adb shell am force-stop $Paquete | Out-Null
    if (-not $CacheCaliente) { Adb shell pm clear $Paquete | Out-Null }
    Adb shell am start -n "$Paquete/com.example.vinilos.MainActivity" | Out-Null
    Start-Sleep -Seconds 3
}

function Ejecutar {
    if ((Validar) -gt 0) { throw 'Resuelve los puntos marcados como FALLA y vuelve a ejecutar.' }
    $d = Leer-Dispositivo
    $destino = Join-Path $Raiz ("docs\performance\resultados\" + (Nombre-Carpeta $d))
    New-Item -ItemType Directory -Force -Path $destino | Out-Null
    Adb reverse tcp:3000 tcp:3000 | Out-Null
    $registro = New-Object System.Collections.Generic.List[object]
    $variantesUsadas = New-Object System.Collections.Generic.List[string]
    $conStudio = -not $SinStudio
    $huSeleccionadas = @($Escenarios | Where-Object { $Definiciones.Contains($_) })
    $conTelemetria = $huSeleccionadas.Count -and $Pasadas -contains 'telemetria'
    $conTraza = $huSeleccionadas.Count -and $Pasadas -contains 'traza'
    $conAsignaciones = $huSeleccionadas.Count -and $Pasadas -contains 'asignaciones'

    # ---- parte 1, APK profileable: cifras representativas de CPU, memoria y fluidez
    if ($conTelemetria -or $conTraza -or $Escenarios -contains 'caida') {
        Preparar-Variante 'profileable'
        $variantesUsadas.Add('profileable')
    }

    $pasosPreparar = @(
        'Abre el proyecto Vinilos (si no lo está) y maximiza Android Studio.',
        'Abre la ventana Profiler (menú View > Tool Windows > Profiler) y deja que ocupe la mayor parte de la ventana.',
        'Si quedó abierta una pestaña de una grabación anterior, ciérrala con su X.',
        "En la pestaña Home elige el dispositivo: $($d.Fabricante) $($d.Modelo).",
        "En la lista de procesos haz clic en: $Paquete (columna Manifest Configuration: Profileable)."
    )

    # ---- pasada de telemetría: Live Telemetry en Studio + memoria y fluidez por línea de comandos
    if ($conTelemetria) {
        $telemetria = $false
        if ($conStudio) {
            $telemetria = Pausa 'Preparar el Profiler e iniciar Live Telemetry' @(
                "La app $Paquete ya está abierta en el teléfono. NO toques el teléfono durante la ejecución.",
                ('Voy a correr seguidos: ' + (($huSeleccionadas | ForEach-Object { $_.ToUpper() }) -join ', ') + '.')
            ) ($pasosPreparar + @(
                'En la lista de tareas (Tasks) selecciona "View Live Telemetry".',
                'Abajo, en "Start profiler task from", elige "Now (attaches to selected process)".',
                'Pulsa "Start profiler task" (abajo a la derecha).',
                'Se abre la pestaña "View Live Telemetry": espera a ver "Recording: ..." y las gráficas LIVE_CPU y MEMORY.',
                'Deja esa pestaña a la vista.'
            )) 'Cuando diga "Recording" y las gráficas estén corriendo, presiona Enter.'
        }
        # Tiempo desde el Enter anterior: aproxima el cronómetro "Recording" de Studio (que va unos segundos adelante).
        $relojTelemetria = [Diagnostics.Stopwatch]::StartNew()
        foreach ($id in $huSeleccionadas) {
            $def = $Definiciones[$id]
            $carpeta = Join-Path $destino $def.Carpeta
            New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
            Escribir-Titulo "$($def.Titulo) · pasada de telemetría"
            if ($id -eq 'hu10') { Quitar-Favoritos }
            $desde = $relojTelemetria.Elapsed
            $fila = Medir-Escenario $def $carpeta 'telemetria' { Correr-Recorrido $def.Metodo }
            $hasta = $relojTelemetria.Elapsed
            $registro.Add($fila)
            if (-not $fila.Exito) {
                Escribir-Resultado 'FALLA' "El recorrido de $($def.Titulo) NO terminó: sus mediciones no sirven y no se toma la captura. Corrige la causa y repite este escenario."
            } elseif ($telemetria) {
                $lista = Pausa "$($def.Titulo): ajustar la vista de Live Telemetry (el recorrido duró $($fila.Segundos) s)" @(
                    'El recorrido terminó. La grabación sigue; no la detengas.',
                    ("El recorrido ocurrió más o menos entre {0:mm\:ss} y {1:mm\:ss} de la grabación (pocos segundos después, según cuánto tardaste en presionar Enter)." -f $desde, $hasta)
                ) @(
                    'En la pestaña "View Live Telemetry" pulsa el botón de alejar (círculo con el signo menos, a la derecha de "Recording").',
                    'Repite hasta que la línea de tiempo (abajo) empiece en 00:00: así se ve la grabación completa, incluido el recorrido.',
                    'Verifica que se vean las gráficas LIVE_CPU y MEMORY.',
                    'No minimices la ventana de Android Studio.'
                ) "Presiona Enter y tomaré la captura como $($def.Carpeta)\live-telemetry.png."
                if ($lista) { Capturar-Studio (Join-Path $carpeta 'live-telemetry.png') }
            }
            if ($id -eq 'hu07') { Borrar-AlbumesDePrueba }
        }
        if ($telemetria) {
            Pausa 'Detener Live Telemetry' @() @(
                'En la pestaña "View Live Telemetry" pulsa "Stop Recording" (botón azul, arriba a la derecha).'
            ) 'Cuando la grabación se haya detenido, presiona Enter.' | Out-Null
        }
    }

    # ---- pasada de traza: System Trace (en Studio para HU03, HU07 y HU10; por línea de comandos el resto)
    if ($conTraza) {
        foreach ($id in $huSeleccionadas) {
            $def = $Definiciones[$id]
            $carpeta = Join-Path $destino $def.Carpeta
            New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
            Escribir-Titulo "$($def.Titulo) · pasada de traza del sistema"
            if ($id -eq 'hu10') { Quitar-Favoritos }
            $enStudio = $false
            if ($conStudio -and $def.TrazaEnStudio) {
                $enStudio = Pausa "$($def.Titulo): iniciar System Trace" @(
                    'La app queda como está. NO toques el teléfono.'
                ) @(
                    'Vuelve a la pestaña Home del Profiler.',
                    "Haz clic de nuevo en el proceso ${Paquete}: al volver a Home, Studio suele saltar a com.example.vinilos (Not running).",
                    'En la lista de tareas selecciona "Capture System Activities (System Trace)".',
                    'Abajo, en "Start profiler task from", elige "Now (attaches to selected process)".',
                    'Pulsa "Start profiler task".',
                    'Espera a que aparezca "Recording: ..." con el cronómetro.'
                ) 'Cuando el cronómetro esté corriendo, presiona Enter.'
            }
            $traza = if (-not $enStudio) { Iniciar-Perfetto $def.Carpeta } else { $null }
            $fila = Medir-Escenario $def $carpeta 'traza' { Correr-Recorrido $def.Metodo }
            $registro.Add($fila)
            Detener-Perfetto $traza (Join-Path $carpeta 'system-trace.perfetto-trace')
            if ($enStudio) {
                $lista = Pausa "$($def.Titulo): detener System Trace (duró $($fila.Segundos) s)" @() @(
                    'Pulsa "Stop Recording" (botón azul, arriba a la derecha).',
                    'Espera a que la traza termine de cargar (puede tardar hasta 30 s).',
                    "Deja visible la fila del hilo principal (main) de $Paquete.",
                    'No minimices ni tapes la ventana de Android Studio.'
                ) "Presiona Enter y tomaré la captura como $($def.Carpeta)\system-trace.png."
                if ($lista) { Capturar-Studio (Join-Path $carpeta 'system-trace.png') }
            }
            if ($id -eq 'hu07') { Borrar-AlbumesDePrueba }
        }
    }

    # ---- §11 caída y recuperación del backend
    if ($Escenarios -contains 'caida') {
        $carpeta = Join-Path $destino 'caida-backend'
        New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
        foreach ($variante in @('con-cache', 'sin-cache')) {
            Escribir-Titulo "Caída del backend · $variante"
            $def = @{ Titulo = "Caída del backend ($variante)" }
            $traza = Iniciar-Perfetto "caida-$variante"
            $fila = Medir-Escenario $def $carpeta $variante {
                $bien = Correr-Recorrido 'caidaFase1CargarCatalogo'
                Compose stop web
                Escribir-Resultado 'OK' 'Backend detenido; espero 10 s.'
                Start-Sleep -Seconds 10
                if ($variante -eq 'sin-cache') { Adb shell pm clear $Paquete | Out-Null }
                $bien = (Correr-Recorrido 'caidaFase2ConsultarSinBackend') -and $bien
                Capturar-Telefono (Join-Path $carpeta "$variante-sin-backend.png")
                Compose start web
                if (-not (Esperar-Backend)) { throw 'El backend no volvió a responder tras iniciarlo.' }
                Escribir-Resultado 'OK' 'Backend activo otra vez; espero 15 s.'
                Start-Sleep -Seconds 15
                (Correr-Recorrido 'caidaFase3Recuperar') -and $bien
            }
            $registro.Add($fila)
            Detener-Perfetto $traza (Join-Path $carpeta "$variante.perfetto-trace")
        }
    }

    # ---- parte 2, APK depurable: objetos creados, peticiones de red y Heap Dump
    if ($conAsignaciones -or $Escenarios -contains 'memoria') {
        Preparar-Variante 'depurable'
        $variantesUsadas.Add('depurable')
    }

    # ---- pasada de asignaciones: Java/Kotlin Allocations + Network Inspector
    if ($conAsignaciones) {
        $guiada = $false
        if ($conStudio) {
            $guiada = Pausa 'Preparar Allocations y Network Inspector (APK depurable)' @(
                "Instalé la variante depurable de $Paquete y la abrí en el teléfono. NO toques el teléfono.",
                'Sirve para ver objetos creados y peticiones de red; sus cifras de CPU y fluidez no son comparables.'
            ) @(
                'En el Profiler, cierra con su X las pestañas de grabaciones anteriores.',
                "En la pestaña Home haz clic en el proceso $Paquete (ahora la columna Manifest Configuration dice Debuggable).",
                'Abre la ventana App Inspection: menú View > Tool Windows > App Inspection.',
                "En App Inspection, en el selector de arriba, elige el dispositivo y el proceso $Paquete.",
                'Haz clic en la pestaña "Network Inspector" y espera a que aparezca su línea de tiempo.',
                'Vuelve a la ventana Profiler: menú View > Tool Windows > Profiler.'
            ) 'Cuando Network Inspector esté conectado y hayas vuelto al Profiler, presiona Enter.'
        }
        foreach ($id in $huSeleccionadas) {
            $def = $Definiciones[$id]
            $carpeta = Join-Path $destino $def.Carpeta
            New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
            Escribir-Titulo "$($def.Titulo) · pasada de asignaciones y red"
            if ($id -eq 'hu10') { Quitar-Favoritos }
            $grabando = $false
            if ($guiada) {
                $grabando = Pausa "$($def.Titulo): iniciar Java/Kotlin Allocations" @(
                    'La app queda como está. NO toques el teléfono.'
                ) @(
                    'En el Profiler, vuelve a la pestaña Home.',
                    "Haz clic de nuevo en el proceso ${Paquete}: al volver a Home, Studio suele saltar a com.example.vinilos (Not running).",
                    'En la lista de tareas selecciona "Track Memory Consumption (Java/Kotlin Allocations)".',
                    'Abajo, en "Start profiler task from", elige "Now (attaches to selected process)".',
                    'Pulsa "Start profiler task".',
                    'Espera a que aparezca "Recording: ..." con el cronómetro.'
                ) 'Cuando el cronómetro esté corriendo, presiona Enter.'
            }
            $fila = Medir-Escenario $def $carpeta 'asignaciones' { Correr-Recorrido $def.Metodo }
            $registro.Add($fila)
            if (-not $fila.Exito) {
                Escribir-Resultado 'FALLA' "El recorrido de $($def.Titulo) NO terminó: no se toman las capturas. Si hay una grabación en curso, deténla con ""Stop Recording""."
            } elseif ($guiada) {
                if ($grabando) {
                    $lista = Pausa "$($def.Titulo): detener Allocations (duró $($fila.Segundos) s)" @() @(
                        'Pulsa "Stop Recording" (botón azul, arriba a la derecha).',
                        'Espera a que cargue la tabla de clases con las columnas Allocations y Deallocations.',
                        'Haz clic en el encabezado "Allocations" para dejar arriba las clases con más objetos creados.',
                        'No minimices ni tapes la ventana de Android Studio.'
                    ) "Presiona Enter y tomaré la captura como $($def.Carpeta)\allocations.png."
                    if ($lista) { Capturar-Studio (Join-Path $carpeta 'allocations.png') }
                }
                $lista = Pausa "$($def.Titulo): reporte de red (Network Inspector)" @(
                    "Las peticiones del recorrido ocurrieron en los últimos $($fila.Segundos) s antes de detener la grabación."
                ) @(
                    'Abre la ventana App Inspection (View > Tool Windows > App Inspection) y la pestaña "Network Inspector".',
                    'En la línea de tiempo, arrastra el ratón para seleccionar el tramo del recorrido.',
                    'Verifica que abajo aparezca la tabla de peticiones (Connection View) con las llamadas a localhost:3000.',
                    'Deja App Inspection ocupando la mayor parte de la ventana.'
                ) "Presiona Enter y tomaré la captura como $($def.Carpeta)\network-inspector.png."
                if ($lista) { Capturar-Studio (Join-Path $carpeta 'network-inspector.png') }
            }
            if ($id -eq 'hu07') { Borrar-AlbumesDePrueba }
        }
    }

    # ---- §12 navegación y memoria
    if ($Escenarios -contains 'memoria') {
        $app = Info-Paquete $Paquete
        $carpeta = Join-Path $destino 'memoria-navegacion'
        New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
        $def = @{ Titulo = 'Navegación y memoria' }
        $pasosHeap = @(
            'Vuelve a la pestaña Home del Profiler.',
            "Haz clic de nuevo en el proceso ${Paquete}: al volver a Home, Studio suele saltar a com.example.vinilos (Not running).",
            'En la lista de tareas selecciona "Analyze Memory Usage (Heap Dump)".',
            'Abajo, en "Start profiler task from", elige "Now (attaches to selected process)".',
            'Pulsa "Start profiler task" y espera a que aparezca la tabla de clases.',
            'Ordena la tabla por "Retained Size" (clic en el encabezado de la columna).'
        )
        foreach ($momento in @('inicial', 'final')) {
            if ($momento -eq 'final') {
                Escribir-Titulo 'Navegación y memoria · tres vueltas'
                $registro.Add((Medir-Escenario $def $carpeta 'vueltas' { Correr-Recorrido 'memoriaNavegacion' }))
            } else {
                Correr-Recorrido 'comprobarManejo' | Out-Null
            }
            if ($app.Depurable) {
                Adb shell am dumpheap $Paquete "/data/local/tmp/vinilos-$momento.hprof" | Out-Null
                Start-Sleep -Seconds 6
                Adb pull "/data/local/tmp/vinilos-$momento.hprof" (Join-Path $carpeta "heap-$momento.hprof") | Out-Null
                Adb shell rm "/data/local/tmp/vinilos-$momento.hprof" | Out-Null
                Escribir-Resultado 'OK' "Heap Dump $momento guardado (heap-$momento.hprof; no se versiona)."
            }
            if ($conStudio) {
                $contexto = if ($app.Depurable) { @() } else {
                    @('La app instalada es "profileable": si la tarea Heap Dump aparece deshabilitada, salta este paso.')
                }
                $hecho = Pausa "Heap Dump $momento" $contexto $pasosHeap "Presiona Enter y tomaré la captura como memoria-navegacion\heap-$momento.png."
                if ($hecho) { Capturar-Studio (Join-Path $carpeta "heap-$momento.png") }
            }
        }
    }

    # ---- registro
    $commit = (& git -C $Raiz rev-parse --short HEAD)
    if (& git -C $Raiz status --porcelain --untracked-files=no) { $commit += ' más cambios sin confirmar' }
    $cache = if ($CacheCaliente) { 'caliente (no se borraron los datos de la app)' } else { 'fría (datos de la app borrados al empezar)' }
    $app = Info-Paquete $Paquete
    $tipo = 'variantes usadas: ' + ($variantesUsadas -join ' y ')
    $lineas = @(
        "# Registro de perfilamiento · $($d.Fabricante) $($d.Modelo)",
        '',
        "- **Fecha:** $(Get-Date -Format 'yyyy-MM-dd HH:mm')",
        "- **Dispositivo:** $($d.Fabricante) $($d.Modelo) · $($d.Procesador) ($($d.Abi)) · $($d.RamGb) GB de RAM · Android $($d.Android) (API $($d.Api)) · $($d.Pantalla)$(if ($d.Emulador) { ' · emulador' })",
        "- **App:** $Paquete $($app.Version -replace '-perf.*$', '') ($tipo), commit $commit",
        '- **Pasadas:** telemetria y traza se miden con el APK profileable; asignaciones y la prueba de memoria, con el depurable (sus cifras no son comparables con las otras).',
        "- **Caché al empezar:** $cache",
        '',
        '| Escenario | Pasada | Terminó | Duración (s) | Memoria inicial (MB) | Máxima (MB) | Final (MB) | Cuadros | Cuadros lentos | P90 |',
        '|---|---|---|---|---|---|---|---|---|---|'
    )
    foreach ($f in $registro) {
        $termino = if ($f.Exito) { 'sí' } else { '**no**' }
        $lineas += "| $($f.Escenario) | $($f.Pasada) | $termino | $($f.Segundos) | $($f.Inicial) | $($f.Maximo) | $($f.Final) | $($f.Cuadros) | $($f.Lentos) | $($f.P90) |"
    }
    $lineas += @('', '## CPU, hilo principal y hallazgos', '',
        '_Por completar a mano con lo observado en las capturas del Profiler (comportamiento de CPU, picos,',
        'operaciones largas en el hilo principal) y los hallazgos con su impacto y evidencia._')
    $archivo = Join-Path $destino ("registro-" + (Get-Date -Format 'yyyyMMdd-HHmm') + '.md')
    Set-Content -Path $archivo -Value $lineas -Encoding UTF8

    Escribir-Titulo 'Terminado'
    Write-Host "Resultados: $destino"
    Write-Host "Registro:   $archivo"
    $fallidos = @($registro | Where-Object { -not $_.Exito })
    if ($fallidos.Count) { Write-Host "$($fallidos.Count) recorrido(s) no terminaron: revisa la salida de arriba." -ForegroundColor Red }
    Write-Host 'Falta a mano: revisar las capturas y completar CPU, hilo principal y hallazgos en el registro.'
}

# ------------------------------------------------------------------ entrada

Elegir-Dispositivo
switch ($Accion) {
    'Validar' { if ((Validar) -gt 0) { exit 1 } }
    'Preparar' { Preparar }
    'Ejecutar' { Ejecutar }
}
