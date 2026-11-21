# Insumos para Google Play — Route58

- `icono-512.png`: ícono de tienda 512×512 (fondo #0D0C0A + marca Route58).
- `feature-graphic-1024x500.png`: gráfico de portada con la paleta de marca (ocre #EE7C1F, magenta #FB4B6E).
- motrack/: ícono 512 y feature graphic de Motrack (verde neón #39FF14).
- **Faltan las capturas** (mínimo 2 de teléfono, 1080×1920 o similar): tomarlas de la app real mostrando mapa, lista de dispositivos y notificaciones.

## Build del AAB (Google Play)

El AAB se compila fuera de Google Drive (Drive rompe Gradle) y con el SDK 36:

```powershell
robocopy "C:\Users\admin\Mi unidad\Work\Fusion58\route58-manager-android" C:\dev\route58-play /E /XD build .gradle .git .idea .kotlin
C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-36" "build-tools;36.0.0"
```

En la copia: AGP `8.10.1`, `compileSdk 36`, `targetSdkVersion 36`, `versionCode 43`, `versionName '4.7'`.
`keystore.properties` (ignorado por git) con `storeFile` en barras normales y ruta absoluta al `.jks` de `SyncIA/secrets/android/`.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
.\gradlew.bat --no-daemon --project-cache-dir C:\dev\gradle-cache-play bundleRoute58GoogleRelease
```

Salida: `app/build/outputs/bundle/route58GoogleRelease/app-route58-google-release.aab`.
El AAB **no se versiona** (es binario de entrega); se sube al Play Console y se anota su SHA-256 en el issue #569.

## Ramas

`origin/master` es el espejo del Traccar Manager original (`org.traccar.manager`); **no** es la app de Fusión58. La app brandeada vive en `origin/brand-estandar-ocre-58` (colores de marca) y de ahí sale la rama de release `rel/play-4.7` con los cambios de API 36/versión.

