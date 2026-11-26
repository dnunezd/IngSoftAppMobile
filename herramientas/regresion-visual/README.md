# Regresión visual entre versiones

Captura las mismas pantallas en varias versiones de la app y las compara. El resultado de la última corrida está en [`docs/pruebas/sprint-3/regresion`](../../docs/pruebas/sprint-3/regresion).

| Archivo | Qué hace |
|---|---|
| `pantalla.py` | Maneja la app desde afuera con adb: lee la pantalla (`uiautomator dump`), toca por texto, desliza, escribe y captura. |
| `capturar.py` | Recorre las pantallas de HU00 a HU04, HU07 y HU10 y guarda una captura de cada una. Omite las que no existían en la versión. |
| `comparar.py` | Compara dos carpetas de capturas: imágenes con las diferencias en rojo y `resultado.csv`. |
| `explorar.py` | Ayuda para trazar un recorrido nuevo: ejecuta acciones y muestra los textos de la pantalla. |

## Requisitos

- Python 3 con Pillow (`pip install pillow`).
- Un dispositivo o emulador con Android 13 o superior (para fijar el idioma de la app) y BackVynils local.
- Para cada versión, un APK compilado con `-PbaseUrl=http://localhost:3000/`.

## Pasos

```bash
# 1. Un APK por versión (cada una en su propia carpeta, sin tocar la rama actual)
git worktree add ../vinilos-v1.1.0 v1.1.0
cd ../vinilos-v1.1.0 && ./gradlew assembleDebug -PbaseUrl=http://localhost:3000/
# ...lo mismo con v2.2.0 y con develop

# 2. Backend y dispositivo
adb reverse tcp:3000 tcp:3000

# 3. Capturas: todas seguidas, en el mismo dispositivo y contra el mismo estado del backend
python capturar.py emulator-5554 vinilos-v1.1.0.apk 1 capturas/v1.1.0
python capturar.py emulator-5554 vinilos-v2.2.0.apk 2 capturas/v2.2.0
python capturar.py emulator-5554 vinilos-develop.apk 3 capturas/develop

# 4. Ruido: una segunda corrida de cada versión, comparada consigo misma, debe dar 0 %
python capturar.py emulator-5554 vinilos-v2.2.0.apk 2 capturas/v2.2.0-repeticion
python comparar.py capturas/v2.2.0 capturas/v2.2.0-repeticion comparacion/ruido-v2.2.0

# 5. Comparación entre versiones
python comparar.py capturas/v1.1.0 capturas/v2.2.0 comparacion/v1.1.0-a-v2.2.0
python comparar.py capturas/v2.2.0 capturas/develop comparacion/v2.2.0-a-develop
```

El tercer argumento de `capturar.py` es el número de la iteración (1, 2 o 3): decide qué pantallas existen. Para una versión nueva, agrega sus pantallas a `capturar.py` con `desde=4` y usa `explorar.py` para encontrar los textos por los que navegar.

`capturar.py` desinstala la app antes de instalar cada versión y deja el dispositivo sin animaciones, sin rotación automática y con la barra de estado en modo demo.
