# Morrow

Morrow is an Android-first personal app blocker. Choose distracting apps, set a daily allowance for each, and get a 15-second pause before opening them. When an allowance runs out, Morrow covers that app until local midnight. Usage counters stay on the device.

## Build and test

Requirements: JDK 17 and Android SDK Platform 37. Configure `JAVA_HOME` and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) for your machine, then run from the repository root:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install on a connected Android phone or a running emulator:

```powershell
.\gradlew.bat installDebug
```

Android Studio is optional; the Android Emulator can be installed and run separately. Hardware virtualization is recommended for usable emulator performance.

## First launch

Choose apps, set their daily allowances, then turn on **Morrow app blocker** in Android Accessibility settings. Android requires this access to interrupt selected apps. Morrow checks app-window changes only; its service does not read screen text or taps. This build is intended for personal sideloading.