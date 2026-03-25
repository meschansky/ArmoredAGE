# Repository Guidelines

## Project Structure & Module Organization
This repository contains a single Android app module, `app/`. Kotlin sources live under `app/src/main/java/com/example/armoredage/` and are split by concern: `ui/` for Compose screens and view models, `data/` for local storage managers, and `crypto/` for AGE integration. Android resources are in `app/src/main/res/`, and CI configuration lives in `.github/workflows/android-apk.yml`.

## Build, Test, and Development Commands
Use a local Gradle installation; a Gradle wrapper is not committed.

```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
gradle :app:lint
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

`assembleDebug` builds the local APK. `assembleRelease` matches the CI release build. `lint` checks Android and Compose issues. Unit and instrumented test commands are listed for future coverage; they may fail until corresponding test sources are added.

## Coding Style & Naming Conventions
Follow Kotlin defaults: 4-space indentation, trailing commas only where already used, and concise expression-bodied functions for simple state updates. Keep package names lowercase (`com.example.armoredage.*`), classes in `PascalCase`, functions and properties in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Match existing file grouping, for example `ui/AgeViewModel.kt` or `data/KeyManager.kt`. No formatter configuration is checked in, so keep style aligned with current source files and Android Studio defaults.

## Testing Guidelines
There are currently no committed tests. Add JVM tests under `app/src/test/...` for pure Kotlin logic and `app/src/androidTest/...` for device or Compose UI behavior. Name test files after the subject under test, such as `AgeViewModelTest.kt`, and write focused test names that describe the expected behavior.

## Commit & Pull Request Guidelines
Recent commits use short imperative subjects, for example `Scaffold armored AGE Android app with CI APK build`. Keep that pattern: one clear sentence describing the change. Pull requests should include a concise summary, note any crypto, storage, or CI impact, link related issues, and attach screenshots when UI behavior changes.

## Security & Configuration Tips
Do not commit real signing keys or secrets. CI generates a temporary keystore, and local key material is stored through `EncryptedSharedPreferences`; keep any changes to key handling explicit and reviewable.
