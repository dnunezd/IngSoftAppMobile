<#
.SYNOPSIS
  Automatiza la guía manual de perfilamiento de Vinilos (docs/performance/guia-manual.md).

.DESCRIPTION
  -Accion Validar   Revisa el dispositivo, el backend, los APK y Android Studio. No cambia nada.
  -Accion Preparar  Compila la app "perf" depurable de la versión pedida y el APK de recorridos, los
                    instala y deja listo adb reverse.
  -Accion Ejecutar  Por cada HU hace las dos grabaciones de la guía, cada una con la app reiniciada sin
                    datos: 1) Live Telemetry y Network Inspector; 2) Java/Kotlin Allocations. Maneja la
                    app con los pasos de la guía (una acción y 5 s), anota el segundo de la grabación en
                    que ocurre cada paso y se detiene con instrucciones cuando hay que hacer algo en el
                    Profiler: iniciar y detener las tareas y exportar lo grabado.

  -Version elige qué código se perfila: un tag o commit (p. ej. v2.1.0) o, sin ella, la rama actual.
  Para un tag, Preparar lo saca en build\perfilamiento\fuente-<versión> y, si le faltan, le aplica los
  dos parches de scripts\perfilamiento\parches: la compilación "perf" y los testTag visibles para
  UI Automator. No cambian la lógica de la app.

  Los resultados quedan en docs/performance/resultados/<propietario>-<dispositivo>/HUxx/<fase>/:
    live-telemetry.png  captura de View Live Telemetry (la toma el script)
    live-view.asdb      grabación de Live Telemetry (Past Recordings > Export recording)
    connections.json    peticiones del Network Inspector (Export Connections)
    memory.asdb         grabación de Java/Kotlin Allocations (Past Recordings > Export recording)
    analisis.md         pasos con su segundo, mediciones y lo que hay que analizar (lo arma el script)

.EXAMPLE
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar -Version v2.1.0
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Validar -Version v2.1.0
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Version v2.1.0 -Fase starter -Escenarios hu01
  .\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Version v2.1.0 -Escenarios hu03 -Grabaciones asignaciones
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Validar', 'Preparar', 'Ejecutar')]
    [string]$Accion,

    # Tag o commit a perfilar (p. ej. v2.1.0). Sin él, el código de la rama actual.
    [string]$Version,

    # Fase del perfilamiento: "starter" es la primera (línea base, sin micro-optimizaciones).
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Fase = 'starter',

    # Quién perfila: primera parte del nombre de la carpeta de resultados. Por defecto, el primer
    # nombre de git config user.name, en minúsculas y sin tildes.
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Propietario,

    # Serial de adb (adb devices). Obligatorio solo si hay más de un dispositivo conectado.
    [string]$Serial,

    # Carpeta del repositorio BackVynils (donde está su docker-compose.yml).
    [string]$RutaBackend,

    # HU a perfilar, en orden. Por defecto, las seis de la guía.
    [ValidateSet('hu01', 'hu02', 'hu03', 'hu04', 'hu07', 'hu10')]
    [string[]]$Escenarios = @('hu01', 'hu02', 'hu03', 'hu04', 'hu07', 'hu10'),

    # Grabaciones de cada HU: telemetria = 1 (Live Telemetry + Network Inspector);
    # asignaciones = 2 (Java/Kotlin Allocations).
    [ValidateSet('telemetria', 'asignaciones')]
    [string[]]$Grabaciones = @('telemetria', 'asignaciones'),

    # Ejecutar: sin pausas de Android Studio; solo maneja la app y anota los pasos (para probar).
    [switch]$SinStudio,

    # Ejecutar: carpeta raíz de los resultados. Por defecto, docs\performance\resultados de este repositorio.
    [string]$CarpetaResultados
)

$ErrorActionPreference = 'Stop'
# La salida de adb llega en UTF-8: sin esto, una consola con otra página de códigos la lee mal
# ("Ver álbumes" quedaba "Ver ├ílbumes" en analisis.md).
[Console]::OutputEncoding = New-Object Text.UTF8Encoding $false

$Raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $CarpetaResultados) { $CarpetaResultados = Join-Path $Raiz 'docs\performance\resultados' }
$Paquete = 'com.example.vinilos.perf'
$PaqueteRecorridos = 'com.example.vinilos.perfilamiento'
$UrlBackend = 'http://localhost:3000'
if (-not $RutaBackend) { $RutaBackend = Join-Path $Raiz '..\BackVynils' }
$Etiqueta = if ($Version) { $Version } else { 'rama' }
$CarpetaApk = Join-Path $Raiz 'build\perfilamiento'
$ApkApp = Join-Path $CarpetaApk "vinilos-$Etiqueta-perf-depurable.apk"
$ApkRecorridos = Join-Path $CarpetaApk 'vinilos-recorridos.apk'
$CarpetaParches = Join-Path $PSScriptRoot 'parches'
$Pausa = 5

function Como-Slug([string]$Texto) {
    # Minúsculas, sin tildes y con guiones: "Jeffrey Díaz" -> "jeffrey-diaz".
    $sinTildes = -join ($Texto.Normalize([Text.NormalizationForm]::FormD).ToCharArray() |
            Where-Object { [Globalization.CharUnicodeInfo]::GetUnicodeCategory($_) -ne 'NonSpacingMark' })
    ($sinTildes.ToLower() -replace '[^a-z0-9]+', '-').Trim('-')
}

if (-not $Propietario) {
    $usuario = (& git.exe -C $Raiz config user.name 2>$null)
    $Propietario = if ($usuario) { Como-Slug (($usuario -split '\s+')[0]) } else { Como-Slug $env:USERNAME }
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

function Git {
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $salida = (& git.exe @args 2>&1 | Out-String).Trim() } finally { $ErrorActionPreference = $anterior }
    if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') falló: $salida" }
    $salida
}

function Elegir-Dispositivo {
    if (-not (Get-Command adb.exe -ErrorAction SilentlyContinue)) {
        $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools'
        if (Test-Path (Join-Path $sdk 'adb.exe')) { $env:PATH = "$sdk;$env:PATH" }
        else { throw 'No se encuentra adb. Agrega <Android SDK>\platform-tools al PATH.' }
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
  Por USB:  activa "Depuración por USB", pon el USB en "Transferencia de archivos" y acepta la huella.
  Por Wi-Fi: Opciones para desarrolladores > Depuración inalámbrica > adb connect <IP>:<puerto>
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
    [pscustomobject]@{
        Fabricante = Adb shell getprop ro.product.manufacturer
        Modelo     = Adb shell getprop ro.product.model
        Procesador = $soc
        Abi        = Adb shell getprop ro.product.cpu.abi
        RamGb      = [math]::Round($memKb / 1MB, 1)
        Android    = Adb shell getprop ro.build.version.release
        Api        = [int](Adb shell getprop ro.build.version.sdk)
        Pantalla   = ((Adb shell wm size) -replace '.*:\s*', '' -split "`n")[-1].Trim()
        Serial     = $script:SerialActivo
    }
}

function Nombre-Carpeta($Dispositivo) {
    # <propietario>-<dispositivo>: "jeffrey-motorola-edge-30", "ana-samsung-sm-s938b". El fabricante
    # se agrega solo si el modelo no lo trae ya ("motorola edge 30").
    $modelo = Como-Slug $Dispositivo.Modelo
    $fabricante = Como-Slug $Dispositivo.Fabricante
    $equipo = if ($modelo.StartsWith($fabricante)) { $modelo } else { "$fabricante-$modelo" }
    "$Propietario-$equipo"
}

function Probar-Backend {
    try { (Invoke-WebRequest -Uri "$UrlBackend/albums" -UseBasicParsing -TimeoutSec 8).StatusCode -eq 200 }
    catch { $false }
}

function Info-Paquete([string]$Nombre) {
    $volcado = Adb shell dumpsys package $Nombre
    if ($volcado -notmatch 'versionName=(\S+)') { return $null }
    [pscustomobject]@{
        Version   = $Matches[1]
        Depurable = $volcado -match 'pkgFlags=\[[^\]]*DEBUGGABLE'
    }
}

function Milis-Dispositivo { [long](Adb shell 'date +%s%3N') }

# ------------------------------------------------------------------ validar

function Validar {
    $fallas = 0
    Escribir-Titulo 'Dispositivo'
    $d = Leer-Dispositivo
    Escribir-Resultado 'OK' ("{0} {1} · {2} ({3}) · {4} GB de RAM · Android {5} (API {6}) · {7}" -f `
            $d.Fabricante, $d.Modelo, $d.Procesador, $d.Abi, $d.RamGb, $d.Android, $d.Api, $d.Pantalla)
    if ($d.Serial -match ':\d+$|_adb-tls-connect') {
        Escribir-Resultado 'AVISO' 'Conectado por Wi-Fi: el Profiler en vivo se atrasa. Para medir, mejor USB.'
    }
    if ((Adb shell dumpsys power) -match 'mWakefulness=Awake') { Escribir-Resultado 'OK' 'Pantalla encendida.' }
    else { Escribir-Resultado 'FALLA' 'La pantalla está apagada: enciéndela y desbloquea el teléfono.'; $fallas++ }
    if ((Adb shell dumpsys window) -match 'mDreamingLockscreen=true|isStatusBarKeyguard=true|mShowingLockscreen=true') {
        Escribir-Resultado 'FALLA' 'El teléfono está bloqueado: desbloquéalo.'; $fallas++
    } else { Escribir-Resultado 'OK' 'Teléfono desbloqueado.' }
    if ((Adb shell settings get system user_rotation) -ne '0' -and (Adb shell settings get system accelerometer_rotation) -eq '0') {
        Escribir-Resultado 'AVISO' 'La rotación está bloqueada en horizontal: ponlo en vertical para que las grabaciones se comparen.'
    }
    if ((Adb shell dumpsys battery) -match 'temperature:\s*(\d+)') {
        $grados = [int]$Matches[1] / 10
        if ($grados -ge 40) { Escribir-Resultado 'AVISO' "Batería a $grados °C: deja enfriar el teléfono antes de medir." }
        else { Escribir-Resultado 'OK' "Batería a $grados °C." }
    }

    Escribir-Titulo "Aplicaciones ($Etiqueta)"
    $app = Info-Paquete $Paquete
    if (-not $app) { Escribir-Resultado 'FALLA' "$Paquete no está instalado: corre -Accion Preparar$(if ($Version) { " -Version $Version" })."; $fallas++ }
    elseif (-not $app.Depurable) { Escribir-Resultado 'FALLA' "$Paquete $($app.Version) no es depurable: Network Inspector y Allocations lo exigen. Corre -Accion Preparar."; $fallas++ }
    else { Escribir-Resultado 'OK' "$Paquete $($app.Version) (depurable)." }
    if ($app -and $Version -and $app.Version -notlike "$($Version.TrimStart('v'))*") {
        Escribir-Resultado 'AVISO' "La app instalada es la $($app.Version), no la $Version. Corre -Accion Preparar -Version $Version."
    }
    if (Info-Paquete $PaqueteRecorridos) { Escribir-Resultado 'OK' "$PaqueteRecorridos (recorridos) instalado." }
    else { Escribir-Resultado 'FALLA' "$PaqueteRecorridos no está instalado: corre -Accion Preparar."; $fallas++ }

    Escribir-Titulo 'Backend local'
    if (Probar-Backend) { Escribir-Resultado 'OK' "$UrlBackend/albums responde 200." }
    else { Escribir-Resultado 'FALLA' "$UrlBackend/albums no responde. Abre Docker Desktop y en BackVynils: docker compose up -d"; $fallas++ }
    if ((Adb reverse --list) -match 'tcp:3000') { Escribir-Resultado 'OK' 'adb reverse tcp:3000 activo.' }
    else { Escribir-Resultado 'AVISO' 'Falta adb reverse tcp:3000 tcp:3000 (Preparar y Ejecutar lo ponen).' }

    Escribir-Titulo 'Android Studio'
    $studio = Get-Process studio64 -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
    if ($studio) { Escribir-Resultado 'OK' "Abierto: $($studio.MainWindowTitle)" }
    else { Escribir-Resultado 'AVISO' 'Android Studio no está abierto (hace falta para las grabaciones y capturas).' }

    Write-Host ''
    if ($fallas -eq 0) { Write-Host 'Todo listo para perfilar.' -ForegroundColor Green }
    else { Write-Host "$fallas punto(s) por resolver antes de perfilar." -ForegroundColor Red }
    return $fallas
}

# ------------------------------------------------------------------ preparar

function Preparar-Fuente {
    # Devuelve la carpeta con el código a compilar. Para un tag, una copia aparte con los parches.
    if (-not $Version) { return $Raiz }
    $fuente = Join-Path $CarpetaApk "fuente-$Version"
    Git -C $Raiz rev-parse --verify --quiet "$Version^{commit}" | Out-Null
    if (Test-Path (Join-Path $fuente '.git')) {
        Git -C $fuente reset --hard --quiet | Out-Null
        Git -C $fuente checkout --detach --quiet $Version | Out-Null
    } else {
        Git -C $Raiz worktree prune | Out-Null
        Git -C $Raiz worktree add --detach --force $fuente $Version | Out-Null
    }
    Escribir-Resultado 'OK' "Código de $Version en $fuente."

    $gradle = Join-Path $fuente 'app\build.gradle.kts'
    if ((Get-Content $gradle -Raw) -notmatch 'create\("perf"\)') {
        Git -C $fuente apply --whitespace=nowarn (Join-Path $CarpetaParches 'compilacion-perf.patch') | Out-Null
        Escribir-Resultado 'OK' 'Parche aplicado: compilación "perf".'
    }
    $actividad = Join-Path $fuente 'app\src\main\java\com\example\vinilos\MainActivity.kt'
    if ((Get-Content $actividad -Raw) -notmatch 'testTagsAsResourceId') {
        Git -C $fuente apply --whitespace=nowarn (Join-Path $CarpetaParches 'tags-ui-automator.patch') | Out-Null
        Escribir-Resultado 'OK' 'Parche aplicado: testTag visibles para UI Automator.'
    }
    Copy-Item (Join-Path $Raiz 'local.properties') $fuente -Force -ErrorAction SilentlyContinue
    return $fuente
}

function Gradle([string]$Carpeta) {
    Push-Location $Carpeta
    try {
        & .\gradlew.bat @args "-PbaseUrl=$UrlBackend/" -PperfDepurable --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle falló en $Carpeta ($($args -join ' '))." }
    } finally { Pop-Location }
}

function Preparar {
    Escribir-Titulo "Compilar ($Etiqueta)"
    if (-not $env:JAVA_HOME) {
        $jbr = Join-Path $env:USERPROFILE '.jdks\jbr-21.0.11'
        if (Test-Path $jbr) { $env:JAVA_HOME = $jbr; Escribir-Resultado 'OK' "JAVA_HOME = $jbr" }
        else { Write-Host '  JAVA_HOME está vacío: Gradle 8.7 necesita un JDK 17 o 21.' -ForegroundColor Yellow }
    }
    New-Item -ItemType Directory -Force -Path $CarpetaApk | Out-Null
    $fuente = Preparar-Fuente
    Gradle $fuente ':app:assemblePerf'
    Copy-Item (Join-Path $fuente 'app\build\outputs\apk\perf\app-perf.apk') $ApkApp -Force
    Escribir-Resultado 'OK' "App: $ApkApp"
    # Los recorridos siempre salen de esta rama: solo manejan la app por sus testTag.
    Gradle $Raiz ':perfilamiento:assemblePerf'
    Copy-Item (Get-ChildItem (Join-Path $Raiz 'perfilamiento\build\outputs\apk\perf\*.apk') | Select-Object -First 1).FullName $ApkRecorridos -Force
    Escribir-Resultado 'OK' "Recorridos: $ApkRecorridos"

    Escribir-Titulo 'Instalar'
    foreach ($apk in @($ApkApp, $ApkRecorridos)) {
        $salida = Adb install -r $apk
        if ($salida -notmatch 'Success') { throw "No se pudo instalar ${apk}: $salida" }
        Escribir-Resultado 'OK' "$(Split-Path $apk -Leaf) instalado."
    }
    Adb reverse tcp:3000 tcp:3000 | Out-Null
    Escribir-Resultado 'OK' 'adb reverse tcp:3000 tcp:3000.'
    Escribir-Resultado 'OK' "Versión instalada: $((Info-Paquete $Paquete).Version), commit $(Git -C $fuente rev-parse --short HEAD)."
}

# ------------------------------------------------------------------ pausas guiadas

function Pausa([string]$Titulo, [string[]]$Contexto, [string[]]$Pasos, [string]$Confirmacion) {
    while ($true) {
        Escribir-Titulo "PAUSA · $Titulo"
        foreach ($linea in $Contexto) { Write-Host $linea }
        if ($Contexto) { Write-Host '' }
        Write-Host 'En Android Studio:' -ForegroundColor White
        $n = 1
        foreach ($paso in $Pasos) { Write-Host ("  {0}. {1}" -f $n, $paso); $n++ }
        Write-Host ''
        Write-Host $Confirmacion -ForegroundColor White
        $respuesta = Read-Host '[Enter] continuar · [r] repetir instrucciones · [s] saltar este paso · [a] abortar'
        if ($null -eq $respuesta) { throw 'Sin consola para responder: corre el script en una terminal interactiva o con -SinStudio.' }
        switch ($respuesta.Trim().ToLower()) {
            ''  { return $true }
            's' { return $false }
            'a' { throw 'Ejecución abortada por el usuario.' }
            default { }
        }
    }
}

function Leer-Cronometro {
    # Ancla los pasos al cronómetro de Studio: lo que marca "Recording" y la hora del teléfono en ese momento.
    while ($true) {
        $texto = Read-Host 'Escribe lo que marca ahora el cronómetro "Recording" (mm:ss) y presiona Enter'
        $milis = Milis-Dispositivo
        if ($null -eq $texto) { throw 'Sin consola para responder: corre el script en una terminal interactiva o con -SinStudio.' }
        if ($texto -match '^\s*(\d{1,2}):(\d{2})\s*$') {
            return [pscustomobject]@{ Segundos = [int]$Matches[1] * 60 + [int]$Matches[2]; Milis = $milis }
        }
        Write-Host '  Formato mm:ss, por ejemplo 00:12.' -ForegroundColor Yellow
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
    $imagen = New-Object System.Drawing.Bitmap ($r.Right - $r.Left), ($r.Bottom - $r.Top)
    $lienzo = [System.Drawing.Graphics]::FromImage($imagen)
    try {
        # PrintWindow dibuja solo la ventana de Studio, aunque otra la tape. 2 = PW_RENDERFULLCONTENT.
        $contexto = $lienzo.GetHdc()
        try { $dibujada = [Vinilos.Ventanas]::PrintWindow($ventana, $contexto, 2) } finally { $lienzo.ReleaseHdc($contexto) }
        if (-not $dibujada) { Escribir-Resultado 'AVISO' 'No se pudo capturar la ventana de Android Studio.'; return }
        $imagen.Save($Destino, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $lienzo.Dispose(); $imagen.Dispose() }
    Escribir-Resultado 'OK' "Captura: $Destino"
}

function Memoria-Mb {
    if ((Adb shell dumpsys meminfo $Paquete) -match 'TOTAL(?:\s+PSS:)?\s+(\d+)') { return [math]::Round([int]$Matches[1] / 1024, 1) }
    return $null
}

# ------------------------------------------------------------------ recorridos y datos

function Reiniciar-EnFrio {
    # El comando de la guía: detener, borrar los datos (caché fría) y abrir en el ingreso.
    Adb shell am force-stop $Paquete | Out-Null
    Adb shell pm clear $Paquete | Out-Null
    Adb shell am start -n "$Paquete/com.example.vinilos.MainActivity" | Out-Null
    Start-Sleep -Seconds 3
}

function Correr-Recorrido([string]$Metodo, [int]$Numero) {
    $salida = Adb shell am instrument -w -r -e grabacion $Numero -e class "$PaqueteRecorridos.Recorridos#$Metodo" "$PaqueteRecorridos/androidx.test.runner.AndroidJUnitRunner"
    $pasos = @([regex]::Matches($salida, 'INSTRUMENTATION_STATUS: paso=(\d+)\|(\d+)\|([^\r\n]*)') | ForEach-Object {
            [pscustomobject]@{ Numero = [int]$_.Groups[1].Value; Milis = [long]$_.Groups[2].Value; Accion = $_.Groups[3].Value }
        })
    $exito = $salida -match 'OK \(1 test\)'
    if (-not $exito) {
        # Solo el comienzo del error: el resto es la misma traza repetida.
        $detalle = ($salida -split "`n" | Where-Object { $_ -match 'stack=|Exception|Error|at com\.example' } | Select-Object -First 8) -join "`n"
        Write-Host $(if ($detalle) { $detalle } else { $salida }) -ForegroundColor Red
    }
    [pscustomobject]@{ Exito = $exito; Pasos = $pasos }
}

function Quitar-Favoritos {
    # HU10 necesita que Queen no sea favorito de Manolo Bellon (100). La app no deja quitarlos y la API
    # tampoco: DELETE /collectors/{id}/bands/{id} responde 204 sin borrar (deleteCollectorPerformer
    # filtra la lista pero no guarda el resultado). Se quitan en la base del BackVynils local.
    $archivo = Join-Path $RutaBackend 'docker-compose.yml'
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $contenedor = (& docker compose -f $archivo ps -q db 2>$null | Select-Object -First 1)
        if (-not $contenedor) { throw "No encuentro el contenedor db de BackVynils ($archivo)." }
        & docker exec $contenedor psql -U postgres -d vinyls -q -c 'DELETE FROM performer_collectors_collector WHERE "collectorId" = 100;' 2>&1 | Out-Null
    } finally { $ErrorActionPreference = $anterior }
    $favoritos = Invoke-RestMethod "$UrlBackend/collectors/100/performers"
    if (@($favoritos | Where-Object { $_.id -eq 101 }).Count) { throw 'Queen sigue como favorito del coleccionista 100: no se pudo quitar.' }
}

function Borrar-AlbumesDePrueba {
    # HU07 crea "Perf 1" y "Perf 2": se borran tras cada grabación para que ambas vean el mismo catálogo.
    # Invoke-RestMethod entrega la lista como un solo objeto. Encadenado directo a Where-Object (o
    # dentro de @()), el filtro la dejaría pasar entera y se intentaría borrar todo el catálogo: se
    # guarda primero en una variable, que sí se recorre elemento por elemento.
    $albumes = Invoke-RestMethod "$UrlBackend/albums"
    $dePrueba = @($albumes | Where-Object { $_.name -like 'Perf*' })
    if ($dePrueba.Count -ge @($albumes).Count -and @($albumes).Count -gt 0) {
        throw 'El filtro de álbumes "Perf" abarcó todo el catálogo: no se borra nada.'
    }
    foreach ($album in $dePrueba) {
        try { Invoke-RestMethod -Method Delete "$UrlBackend/albums/$($album.id)" | Out-Null } catch { }
    }
}

# ------------------------------------------------------------------ ejecutar

# Lo que la guía pide revisar en cada HU: queda como lista en analisis.md.
$Definiciones = [ordered]@{
    hu01 = @{ Carpeta = 'HU01'; Titulo = 'HU01 · Catálogo de álbumes'; Revisar = @(
            'Red: `GET /albums` y la descarga de cada portada; su tamaño y duración.',
            'CPU: el pico al llegar los datos y dibujar la lista, y el uso al desplazarla.',
            'Memoria: cuánto suben las portadas y si la memoria se queda arriba al volver al inicio.',
            'Asignaciones: las clases de álbum (`AlbumDto`, `AlbumResumen`) y los bitmaps de las portadas.') }
    hu02 = @{ Carpeta = 'HU02'; Titulo = 'HU02 · Detalle de álbum'; Revisar = @(
            'Red: `GET /albums/100` en la primera apertura, y si se repite en la segunda.',
            'CPU y memoria: primera apertura frente a la segunda.',
            'Asignaciones: clases del detalle (`AlbumDto`, `Album`) y objetos del parseo del JSON.') }
    hu03 = @{ Carpeta = 'HU03'; Titulo = 'HU03 · Listado y búsqueda de artistas'; Revisar = @(
            'Red: `GET /musicians` y `GET /bands` al entrar. Escribir en el buscador no debería generar peticiones.',
            'CPU: trabajo del hilo principal con cada letra (filtrar y redibujar la lista).',
            'Asignaciones: `String` y `StringBuilder` durante la búsqueda; clases de artista (`ArtistDto`, `Artist`).') }
    hu04 = @{ Carpeta = 'HU04'; Titulo = 'HU04 · Detalle de artista'; Revisar = @(
            'Red: `GET /musicians/100` y `GET /performerprizes` al abrir el artista (revisar el tamaño de `/performerprizes`), `GET /albums/100` al abrir el álbum, y si se repiten al volver.',
            'CPU y memoria: al abrir el detalle, que tiene imagen y varias secciones.',
            'Asignaciones: `ArtistaDetalle` y los objetos de las premiaciones.') }
    hu07 = @{ Carpeta = 'HU07'; Titulo = 'HU07 · Crear un álbum'; Revisar = @(
            'Red: `GET /collectors` al entrar y `POST /albums` (tamaño, código de respuesta y duración); después, el refresco del catálogo.',
            'CPU: al abrir el formulario, al cargar la vista previa de la portada y al enviar.',
            'Asignaciones: lo que se crea mientras se escribe en el formulario, y las clases `AlbumNuevo` y `OpcionesAlbum`.') }
    hu10 = @{ Carpeta = 'HU10'; Titulo = 'HU10 · Agregar artistas a favoritos'; Revisar = @(
            'Red: `GET /collectors/100/performers`, `POST /collectors/100/bands/101`, y si al reabrir vuelve a consultar.',
            'CPU y memoria: en el cambio de estado del botón.',
            'Asignaciones: las clases de favorito y de coleccionista.') }
}

$PasosElegirProceso = @(
    'Abre la ventana Profiler (View > Tool Windows > Profiler) y cierra con su X las pestañas de grabaciones anteriores.',
    'En la pestaña Home elige el teléfono y haz clic en el proceso com.example.vinilos.perf (Debuggable).',
    'IMPORTANTE: es un proceso nuevo tras el reinicio. Si Studio marca com.example.vinilos (Not running), vuelve a elegir com.example.vinilos.perf.'
)

function Tabla-Pasos($Pasos, $Ancla) {
    # Convierte la hora de cada paso en el segundo de la grabación (o relativo al reposo inicial).
    $base = if ($Ancla) { $Ancla.Milis - $Ancla.Segundos * 1000 } else { ($Pasos | Select-Object -First 1).Milis }
    $lineas = @('| # | Acción | Seg. |', '|---|---|---|')
    foreach ($p in $Pasos) {
        $seg = [math]::Round(($p.Milis - $base) / 1000)
        $lineas += "| $($p.Numero) | $($p.Accion) | {0:mm\:ss} ($seg s) |" -f [timespan]::FromSeconds($seg)
    }
    $lineas
}

function Tramo-Pasos($Pasos, $Ancla) {
    # Tramo de la grabación que cubre el recorrido, con margen, como "01:42 a 02:20".
    $base = $Ancla.Milis - $Ancla.Segundos * 1000
    $desde = [math]::Max(0, [math]::Floor((($Pasos | Select-Object -First 1).Milis - $base) / 1000) - 5)
    $hasta = [math]::Ceiling((($Pasos | Select-Object -Last 1).Milis - $base) / 1000) + $Pausa + 5
    '{0:mm\:ss} a {1:mm\:ss}' -f [timespan]::FromSeconds($desde), [timespan]::FromSeconds($hasta)
}

function Pedir-Archivo([string]$Titulo, [string[]]$Pasos, [string]$Destino) {
    # Guía una exportación de Studio y comprueba que el archivo quedó donde va.
    $carpeta = Split-Path $Destino
    $nombre = Split-Path $Destino -Leaf
    while ($true) {
        try { Set-Clipboard -Value $carpeta } catch { }
        $hecho = Pausa $Titulo @(
            "Carpeta (ya está copiada en el portapapeles): $carpeta",
            "Nombre del archivo: $nombre"
        ) ($Pasos + @("En el diálogo de guardar pega la carpeta (Ctrl+V), escribe el nombre $nombre y guarda.")) 'Cuando el archivo esté guardado, presiona Enter.'
        if (-not $hecho) { Escribir-Resultado 'AVISO' "Saltado: falta $nombre."; return }
        if (Test-Path $Destino) { Escribir-Resultado 'OK' "Guardado: $nombre"; return }
        Escribir-Resultado 'AVISO' "No encuentro $Destino. Revisa la carpeta y el nombre, y vuelve a exportarlo (o [s] para saltar)."
    }
}

function Escribir-Analisis($Definicion, [string]$Carpeta, [int]$Numero, [string[]]$Bloque, $Contexto) {
    # analisis.md se arma una vez y cada grabación reemplaza solo su bloque: lo escrito a mano se conserva.
    $archivo = Join-Path $Carpeta 'analisis.md'
    if (-not (Test-Path $archivo)) {
        $plantilla = @(
            "# $($Definicion.Titulo) · $Fase",
            '',
            "- **Dispositivo:** $($Contexto.Dispositivo)",
            "- **App:** $Paquete $($Contexto.Version) (depurable), fase $Fase, perfilada por $Propietario",
            "- **Pasos:** los de docs/performance/guia-manual.md: $Pausa s de reposo en el ingreso y luego una acción y $Pausa s; la app se reinicia sin datos antes de cada grabación.",
            '',
            '## Grabación 1 · Live Telemetry y Network Inspector',
            '',
            '<!-- grabacion-1 -->',
            '_Sin grabar todavía._',
            '<!-- /grabacion-1 -->',
            '',
            '## Grabación 2 · Java/Kotlin Allocations',
            '',
            '<!-- grabacion-2 -->',
            '_Sin grabar todavía._',
            '<!-- /grabacion-2 -->',
            '',
            '## Qué analizar',
            ''
        ) + ($Definicion.Revisar | ForEach-Object { "- [ ] $_" }) + @(
            '',
            '## Hallazgos',
            '',
            '### CPU (live-telemetry.png, live-view.asdb)',
            '',
            '_Uso de la app frente al resto del sistema, hilo que lleva la carga (principal, `RenderThread`, `OkHttp`) y número de hilos._',
            '',
            '### Memoria (live-telemetry.png, live-view.asdb)',
            '',
            '_Cuánto sube y en qué paso, qué capa crece (Java, nativa, gráficos) y marcas de recolección de basura._',
            '',
            '### Red (connections.json)',
            '',
            '_Petición por paso: tamaño, tipo, código de respuesta y duración; peticiones repetidas o innecesarias._',
            '',
            '### Asignaciones (memory.asdb)',
            '',
            '_Clases que más se crean, en cantidad y tamaño (filtro `com.example.vinilos`), y el método que más asigna (agrupado por pila de llamadas)._',
            '',
            '## Pasos con más carga',
            '',
            '| Paso | Seg. | CPU | Memoria | Red | Observación |',
            '|---|---|---|---|---|---|'
        )
        Set-Content -Path $archivo -Value $plantilla -Encoding UTF8
    }
    $texto = Get-Content $archivo -Raw
    $inicio = "<!-- grabacion-$Numero -->"
    $fin = "<!-- /grabacion-$Numero -->"
    $nuevo = $inicio + "`n" + ($Bloque -join "`n") + "`n" + $fin
    $texto = [regex]::Replace($texto, [regex]::Escape($inicio) + '[\s\S]*?' + [regex]::Escape($fin), { param($m) $nuevo })
    Set-Content -Path $archivo -Value $texto.TrimEnd() -Encoding UTF8
}

function Grabar($Definicion, [string]$Id, [string]$Carpeta, [string]$Grabacion, $Contexto) {
    $numero = if ($Grabacion -eq 'telemetria') { 1 } else { 2 }
    Escribir-Titulo "$($Definicion.Titulo) · grabación $numero de 2"
    if ($Id -eq 'hu10') { Quitar-Favoritos; Escribir-Resultado 'OK' 'Favoritos del coleccionista 100 quitados en la base local.' }
    Reiniciar-EnFrio
    Escribir-Resultado 'OK' 'App reiniciada sin datos, en el ingreso. NO toques el teléfono.'

    $conStudio = -not $SinStudio
    $ancla = $null
    if ($conStudio) {
        if ($numero -eq 1) {
            $listo = Pausa "$($Definicion.Titulo): iniciar Live Telemetry y Network Inspector" @() ($PasosElegirProceso + @(
                    'Selecciona "View Live Telemetry", en "Start profiler task from" elige "Now" y pulsa "Start profiler task".',
                    'Si Studio avisa "This task recommends the process to be profileable", pulsa "Start anyway": la guía usa la app depurable a propósito (Network Inspector y Allocations la exigen).',
                    'Abre App Inspection (View > Tool Windows > App Inspection), elige el mismo proceso y la pestaña "Network Inspector".',
                    'Vuelve a la pestaña "View Live Telemetry" y espera a ver "Recording: ..." con las gráficas corriendo.'
                )) 'Cuando las dos herramientas estén grabando, presiona Enter.'
        } else {
            $listo = Pausa "$($Definicion.Titulo): iniciar Java/Kotlin Allocations" @() ($PasosElegirProceso + @(
                    'Selecciona "Track Memory Consumption (Java/Kotlin Allocations)", con "Now". Si pregunta el modo, elige "Full".',
                    'Pulsa "Start profiler task" (o "Start anyway" si avisa que la app debería ser profileable) y espera a ver "Recording: ..." con el cronómetro.'
                )) 'Cuando el cronómetro esté corriendo, presiona Enter.'
        }
        if ($listo) { $ancla = Leer-Cronometro }
    }

    Adb shell dumpsys gfxinfo $Paquete reset | Out-Null
    $memoriaInicial = Memoria-Mb
    $reloj = [Diagnostics.Stopwatch]::StartNew()
    $resultado = Correr-Recorrido $Id $numero
    $reloj.Stop()
    $memoriaFinal = Memoria-Mb
    $fluidez = Adb shell dumpsys gfxinfo $Paquete
    $cuadros = if ($fluidez -match 'Total frames rendered:\s*(\d+)') { $Matches[1] } else { '—' }
    $lentos = if ($fluidez -match 'Janky frames:\s*(\d+\s*\([\d.,]+%\))') { $Matches[1] } else { '—' }
    $p90 = if ($fluidez -match '90th percentile:\s*(\d+ms)') { $Matches[1] } else { '—' }

    $tabla = Tabla-Pasos $resultado.Pasos $ancla
    Write-Host ''
    $tabla | ForEach-Object { Write-Host "  $_" }

    if (-not $resultado.Exito) {
        Escribir-Resultado 'FALLA' "El recorrido NO terminó: esta grabación no sirve. Detén la grabación en Studio, corrige la causa y repite con -Escenarios $Id -Grabaciones $Grabacion."
    } elseif ($conStudio -and $ancla) {
        if ($numero -eq 1) {
            $tramo = Tramo-Pasos $resultado.Pasos $ancla
            if (Pausa 'Capturar Live Telemetry' @(
                    'El recorrido terminó. La grabación sigue; no la detengas todavía.',
                    "El recorrido está entre $tramo de la grabación: ese tramo debe quedar en la captura."
                ) @(
                    'En "View Live Telemetry" pulsa el botón de pausar el seguimiento en vivo (a la derecha de las lupas) para que la vista no se siga moviendo.',
                    "Aleja con la lupa (-) y mueve la barra de desplazamiento de abajo hasta que la línea de tiempo muestre de $tramo completo.",
                    'Verifica que se vean las gráficas de CPU y de memoria, y aparta el ratón de ellas.'
                ) 'Presiona Enter y tomaré la captura como live-telemetry.png.') {
                Capturar-Studio (Join-Path $Carpeta 'live-telemetry.png')
            }
            Pedir-Archivo 'Exportar las peticiones del Network Inspector' @(
                'Abre App Inspection > Network Inspector (la grabación del Profiler sigue).',
                'Pulsa el botón "Export Connections" de la barra del Network Inspector.'
            ) (Join-Path $Carpeta 'connections.json')
            Pausa 'Detener la grabación 1' @() @('En el Profiler pulsa "Stop Recording" y espera a que termine.') 'Cuando se haya detenido, presiona Enter.' | Out-Null
            Pedir-Archivo 'Exportar la grabación de Live Telemetry' @(
                'En el Profiler abre la pestaña "Past Recordings".',
                'Selecciona la grabación de Live Telemetry que acabas de detener (la más reciente).',
                'Pulsa "Export recording".'
            ) (Join-Path $Carpeta 'live-view.asdb')
        } else {
            Pausa 'Detener Allocations' @() @(
                'Pulsa "Stop Recording" y espera a que cargue la tabla de clases.',
                'Revísala ahora si quieres: ordenada por "Allocations" y con el filtro com.example.vinilos.'
            ) 'Cuando se haya detenido, presiona Enter.' | Out-Null
            Pedir-Archivo 'Exportar la grabación de Allocations' @(
                'En el Profiler abre la pestaña "Past Recordings".',
                'Selecciona la grabación de Java/Kotlin Allocations que acabas de detener (la más reciente).',
                'Pulsa "Export recording".'
            ) (Join-Path $Carpeta 'memory.asdb')
        }
    }
    if ($Id -eq 'hu07') { Borrar-AlbumesDePrueba; Escribir-Resultado 'OK' 'Álbumes "Perf" borrados por la API.' }

    $archivos = if ($numero -eq 1) { '`live-telemetry.png`, `live-view.asdb`, `connections.json`' } else { '`memory.asdb`' }
    $origen = if ($ancla) { 'segundos del cronómetro "Recording" de Studio (±2 s)' } else { 'segundos desde el reposo inicial; sin cronómetro de Studio' }
    $termino = if ($resultado.Exito) { 'terminó' } else { '**no terminó: repetir**' }
    $bloque = @(
        "Grabada el $(Get-Date -Format 'yyyy-MM-dd HH:mm'); el recorrido $termino en $([math]::Round($reloj.Elapsed.TotalSeconds)) s. Archivos: $archivos.",
        '',
        "Columna Seg.: $origen.",
        ''
    ) + $tabla + @(
        '',
        "Medido por línea de comandos: memoria de la app (PSS) $memoriaInicial MB al empezar y $memoriaFinal MB al terminar; $cuadros cuadros dibujados, $lentos lentos, percentil 90 de $p90."
    )
    Escribir-Analisis $Definicion $Carpeta $numero $bloque $Contexto
    $resultado.Exito
}

function Ejecutar {
    if ((Validar) -gt 0) { throw 'Resuelve los puntos marcados como FALLA y vuelve a ejecutar.' }
    $d = Leer-Dispositivo
    $destino = Join-Path $CarpetaResultados (Nombre-Carpeta $d)
    Adb reverse tcp:3000 tcp:3000 | Out-Null
    $contexto = [pscustomobject]@{
        Dispositivo = "$($d.Fabricante) $($d.Modelo) · $($d.Procesador) ($($d.Abi)) · $($d.RamGb) GB de RAM · Android $($d.Android) (API $($d.Api)) · $($d.Pantalla)"
        Version     = (Info-Paquete $Paquete).Version
    }
    $fallidos = 0
    foreach ($id in $Escenarios) {
        $def = $Definiciones[$id]
        $carpeta = Join-Path $destino "$($def.Carpeta)\$Fase"
        New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
        foreach ($grabacion in (@('telemetria', 'asignaciones') | Where-Object { $Grabaciones -contains $_ })) {
            if (-not (Grabar $def $id $carpeta $grabacion $contexto)) { $fallidos++ }
        }
        Escribir-Titulo "$($def.Titulo) · archivos en $carpeta"
        foreach ($nombre in @('live-telemetry.png', 'live-view.asdb', 'connections.json', 'memory.asdb', 'analisis.md')) {
            if (Test-Path (Join-Path $carpeta $nombre)) { Escribir-Resultado 'OK' $nombre } else { Escribir-Resultado 'AVISO' "falta $nombre" }
        }
    }

    Escribir-Titulo 'Terminado'
    Write-Host "Resultados: $destino"
    if ($fallidos) { Write-Host "$fallidos grabación(es) con el recorrido incompleto: revisa la salida de arriba." -ForegroundColor Red }
    Write-Host 'Falta a mano: completar en cada analisis.md los hallazgos y los pasos con más carga.'
}

# ------------------------------------------------------------------ entrada

Elegir-Dispositivo
switch ($Accion) {
    'Validar' { if ((Validar) -gt 0) { exit 1 } }
    'Preparar' { Preparar }
    'Ejecutar' { Ejecutar }
}
