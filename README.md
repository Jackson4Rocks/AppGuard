# AppGuard

A privacy-first Android app locker with a Material 3 UI.

## Features

- PIN-protected app locking
- Fingerprint / face authentication through AndroidX BiometricPrompt
- Accessibility-service based foreground app detection
- Material 3 Compose interface with dynamic color
- App allowlist/locklist management
- No network permissions

## Build

Open the project in Android Studio and let Gradle sync the AndroidX dependencies.

The app targets Android 16 / API 36 and supports Android 8.0 / API 26 and newer.

## Accessibility

AppGuard needs its accessibility service enabled manually in Android Settings. The service only observes foreground/window changes needed for the app-lock feature.

## Notes

The standalone "Hide apps" launcher mode is planned separately. A normal third-party app cannot universally hide another app from every OEM launcher.

Biometric authentication uses AndroidX BiometricPrompt, with device-credential fallback on Android 11+ where supported.
