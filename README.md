# ArmoredAge (Android/Kotlin)

Mobile app for **encrypting/decrypting ASCII-armored AGE messages** with local key management.

## Features

- Generates and stores AGE identities on-device (encrypted preferences).
- Stores known recipient public keys for encryption.
- Encrypts plaintext to armored AGE payloads.
- Decrypts only armored AGE payloads.
- GitHub Actions CI builds a self-signed release APK artifact.

## Notes

- Crypto wiring is implemented through a small reflection bridge over `kage` to reduce breakage when API signatures change across versions.
- The app intentionally rejects non-armored payloads for decryption.

## Build locally

```bash
gradle :app:assembleDebug
```

## CI APK

Workflow: `.github/workflows/android-apk.yml`

Each run generates a temporary self-signed keystore and builds `:app:assembleRelease`, then uploads the APK artifact.
