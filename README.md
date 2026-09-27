# CastBridge v2

CastBridge is a personal Android Chromecast sender. It does not use screen mirroring.

## v2 additions
- Built-in WebView browser with address bar, back/forward, reload and Cast button.
- Cast Video button attempts to detect an HTML5 video URL exposed by the current web page and sends it to the Chromecast Default Media Receiver.
- Direct media URL casting remains available from the home screen.
- Native Cast-enabled apps can continue to use their own official Cast controls.

## Important limitation
A browser page may use DRM, encrypted media, blob URLs, JavaScript players, login-gated streams, or other mechanisms that do not expose a castable direct media URL. CastBridge does not bypass DRM or extract protected streams. In those cases, use the service's own official Cast button/app.

## Build
GitHub Actions builds a debug APK using JDK 17, Gradle 8.9, Android API 35 and setup-android v4.
