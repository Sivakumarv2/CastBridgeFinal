# CastBridge

Personal Android sender app for Google Chromecast.

## What it does
- Uses the Google Cast Android Sender SDK.
- Does not use screen mirroring.
- Lets the phone remain usable while the Chromecast plays.
- Provides a Cast button, direct-media URL playback, stop casting, expanded controller, and Cast notification/lock-screen controls through the Cast framework.
- Uses Google's Default Media Receiver (`CC1AD845`) for compatible direct media URLs.

## Important limitation
CastBridge cannot force third-party apps to expose their private playback streams and cannot bypass DRM. YouTube, Netflix, Prime Video, JioHotstar, SonyLIV, etc. should be cast using their own official Cast support when available.

## Build
GitHub Actions is included at `.github/workflows/build-apk.yml`.

The workflow uses:
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- JDK 17
- Android API 35
- Google Cast Framework 22.2.0

After uploading the project to a GitHub repository, open **Actions → Build CastBridge APK → Run workflow**. The successful run produces the `CastBridge-debug-apk` artifact.
