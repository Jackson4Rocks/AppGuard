# 🛡️ AppGuard

<p align="center">
  <img
    src="https://raw.githubusercontent.com/Jackson4Rocks/AppGuard/main/docs/appguard_icon.svg"
    width="180"
    alt="AppGuard icon"
    style="border-radius: 34px;"
  >
</p>

<p align="center">
  <b>A simple, modern Android app locker built for privacy.</b><br>
  Lock selected apps behind a PIN or your device's fingerprint / face authentication.
</p>

<p align="center">
  <a href="https://github.com/Jackson4Rocks/AppGuard/actions/workflows/android-build.yml">
    <img src="https://github.com/Jackson4Rocks/AppGuard/actions/workflows/android-build.yml/badge.svg" alt="Android Build">
  </a>
</p>

## ✨ Features

- 🔐 **App locking** — choose which installed apps should require authentication.
- 🔢 **PIN protection** — create and change your own AppGuard PIN.
- 👆 **Biometric unlock** — use fingerprint, face authentication, or your device credential where supported.
- 🎨 **Material 3 UI** — roomy Material 3 dashboard with Home, Apps, Security, and More navigation.
- 👨‍💻 **Project Maintainer** — More includes Project Maintainer information and a direct GitHub profile button.
- ⚡ **Lightweight design** — no network permission is required by the app.
- 🧩 **Accessibility-based lock detection** — AppGuard watches for protected apps opening and places its authentication screen over them.
- 🛡️ **Least-privilege event scope** — the service only listens for window-state events from the apps you explicitly protect.
- ⏸️ **Pause protection** — manually disable AppGuard's Accessibility service when you need to use a sensitive app without the service enabled.

## 🚀 Getting started

1. Install AppGuard on your Android phone.
2. Open AppGuard and create a PIN.
3. Turn on **Fingerprint / face unlock** if your phone supports it.
4. Open **Accessibility settings** and enable **AppGuard App Lock**.
5. Select the apps you want to protect.
6. Open one of those apps — AppGuard will ask for authentication before letting you continue.

On recent Android versions, sideloaded apps that expose sensitive capabilities such as Accessibility Services may receive an additional system security confirmation. AppGuard does not bypass that protection. The app includes an explicit accessibility disclosure and a manual Pause protection control.

### 🔑 Unlocking

You can unlock a protected app with your AppGuard PIN. When biometric authentication is enabled, you can also use the biometric prompt provided by Android.

## 🛠️ Building from source

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Android 8.0 (API 26) or newer for running the app

Clone the repository:

```bash
git clone https://github.com/Jackson4Rocks/AppGuard.git
cd AppGuard
```

Then open the project in Android Studio and let Gradle sync.

To build a debug APK locally:

```bash
gradle --no-daemon :app:assembleDebug
```

The APK will be generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## 🔒 Privacy & permissions

AppGuard is designed to keep the implementation small and local.

The current app does **not** request internet access. The Accessibility Service is used specifically to detect when a protected app becomes the foreground app and to display the lock overlay.

Because Accessibility Services are powerful Android features, AppGuard requires you to explicitly enable its service in system settings.

## 🛡️ Repository security

GitHub Actions now checks the repository with CodeQL, Gitleaks, Trivy filesystem scanning, and dependency review. These checks are intended to catch common code, secret, configuration, and dependency security problems before they reach a release.

## 🧭 Roadmap

- 🔒 Core app-lock improvements and stronger lock-state handling
- 🧑‍💻 More polished settings and onboarding
- 🏠 Optional launcher-based **Hide Apps** mode
- 📱 Better support for OEM-specific Android behavior
- 🧪 More device testing and automated checks

> **Note:** Hide Apps is intentionally separate from the current app-lock system. A regular third-party Android app cannot universally hide another app from every OEM launcher, so that feature will be handled as its own launcher mode.

## 🤝 Contributing

Found a bug or have an idea? Open an issue or pull request on GitHub.

AppGuard is an open-source project by **Jackson4Rocks**.

---

<p align="center">
  Made with ❤️ for Android privacy.
</p>
