# Regresión entre versiones

Compara cada versión de la app con la anterior, con dos métodos. Aquí solo están las herramientas y los comandos: el resultado de cada corrida, sus imágenes y la evaluación del método se publican en la wiki ([Regresión entre versiones](https://github.com/dnunezd/IngSoftAppMobile/wiki/Sprint-3-Regresion), página del Sprint 3), no en el repositorio. Las capturas y comparaciones se generan en carpetas locales que no se versionan.

| Archivo | Qué hace |
|---|---|
| `correr_e2e.py` | **Método 1.** Instala una versión, corre sus E2E, guarda el resultado de cada prueba (`pruebas.csv`) y trae las capturas de `EvidenciaE2E` (una carpeta por escenario). |
| `comparar_e2e.py` | Compara dos corridas de `correr_e2e.py` escenario por escenario y paso por paso, marca como inestables los pasos que cambian entre dos corridas de la misma versión y cruza el estado de cada prueba. |
| `pantalla.py` | **Método 2.** Maneja la app desde afuera con adb: lee la pantalla (`uiautomator dump`), toca por texto, desliza, escribe y captura. |
| `capturar.py` | Recorre las pantallas de HU00 a HU04, HU07 y HU10 y guarda una captura de cada una. Omite las que no existían en la versión. |
| `comparar.py` | Compara dos carpetas de capturas (alinea verticalmente y marca las diferencias en rojo). Lo usan los dos métodos. |
| `explorar.py` | Ayuda para trazar un recorrido nuevo del método 2: ejecuta acciones y muestra los textos de la pantalla. |

## Requisitos

- Python 3 con Pillow (`pip install pillow`).
- Un dispositivo o emulador y BackVynils local con `adb reverse tcp:3000 tcp:3000`. El método 2 necesita Android 13 o superior para fijar el idioma de la app.
- De cada versión, la app y sus pruebas compiladas con `-PbaseUrl=http://localhost:3000/`, cada una en su propia carpeta para no tocar la rama actual:

```bash
git worktree add ../vinilos-v1.1.0 v1.1.0
cd ../vinilos-v1.1.0 && ./gradlew assembleDebug assembleDebugAndroidTest -PbaseUrl=http://localhost:3000/
# ...lo mismo con v2.2.0 y con develop
```

Todas las versiones se corren seguidas, en el mismo dispositivo y contra el mismo estado del backend.

## Método 1: E2E de cada versión

```bash
C1=IngresoE2ETest,CatalogoE2ETest,AlbumDetalleE2ETest,ArtistasE2ETest,navigation.AlbumDetalleBackendRealTest
C2=$C1,ArtistaDetalleE2ETest,ArtistasFavoritosE2ETest,CrearAlbumE2ETest,ArtistasSinConexionE2ETest,CatalogoSinConexionE2ETest,ColeccionistasSinConexionE2ETest,DetalleAlbumSinConexionE2ETest,FavoritosSinConexionE2ETest
R=androidx.test.runner.AndroidJUnitRunner        # desde develop: com.example.vinilos.VinilosTestRunner

# Dos corridas por versión (a y b) para medir el ruido
python correr_e2e.py <serial> v1.1.0-app.apk v1.1.0-test.apk $R e2e/v1.1.0-a $C1
python correr_e2e.py <serial> v2.2.0-app.apk v2.2.0-test.apk $R e2e/v2.2.0-a $C2
# ...y las corridas b, y develop con su runner

# Ruido: cada versión consigo misma
python comparar_e2e.py e2e/v1.1.0-a e2e/v1.1.0-b cmp/ruido-v1.1.0 v1.1.0 "v1.1.0 (2.ª)"

# Entre versiones, sin contar los pasos inestables de ninguna de las dos
python comparar_e2e.py e2e/v1.1.0-a e2e/v2.2.0-a cmp/v1.1.0-a-v2.2.0 v1.1.0 v2.2.0 cmp/ruido-v1.1.0/resultado.csv,cmp/ruido-v2.2.0/resultado.csv
```

Cada versión corre su propia copia de las pruebas: las de una versión anterior no compilan contra el código de la siguiente. Antes de comparar, revisa con `git diff <etiqueta> <etiqueta> -- app/src/androidTest` que los escenarios y los pasos con captura sigan siendo los mismos.

## Método 2: recorrido por adb

```bash
python capturar.py <serial> vinilos-v1.1.0.apk 1 capturas/v1.1.0
python capturar.py <serial> vinilos-v2.2.0.apk 2 capturas/v2.2.0
python capturar.py <serial> vinilos-develop.apk 3 capturas/develop

# Ruido: una segunda corrida de cada versión, comparada consigo misma
python capturar.py <serial> vinilos-v2.2.0.apk 2 capturas/v2.2.0-repeticion
python comparar.py capturas/v2.2.0 capturas/v2.2.0-repeticion comparacion/ruido-v2.2.0

python comparar.py capturas/v1.1.0 capturas/v2.2.0 comparacion/v1.1.0-a-v2.2.0
python comparar.py capturas/v2.2.0 capturas/develop comparacion/v2.2.0-a-develop
```

El tercer argumento de `capturar.py` es el número de la iteración (1, 2 o 3) y decide qué pantallas existen. Para una versión nueva, agrega sus pantallas con `desde=4` y usa `explorar.py` para encontrar los textos por los que navegar. `capturar.py` desinstala la app antes de cada versión y deja el dispositivo sin animaciones, sin rotación automática y con la barra de estado en modo demo.
