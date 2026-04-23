# TuneFlow Developer Guide

This file is for local development, signing, and release workflow details.

## Local Setup

Requirements:

- Java 17
- Android SDK
- Android platform tools / build tools

Typical local checks:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:detekt
./gradlew :app:ktlintCheck
```

## Local Signed Release APK

Use the bundled script:

```bash
./scripts/build-local-release.sh \
  --keystore /full/path/to/release.keystore \
  --store-password 'your-store-password' \
  --key-alias tuneflow \
  --key-password 'your-key-password'
```

If you prefer env vars:

```bash
export SIGNING_STORE_FILE=/full/path/to/release.keystore
export SIGNING_STORE_PASSWORD='your-store-password'
export SIGNING_KEY_ALIAS='tuneflow'
export SIGNING_KEY_PASSWORD='your-key-password'
./scripts/build-local-release.sh
```

Output:

- `dist/tuneflow-tv.apk`

## GitHub Secrets For Release

Repository secrets required by `.github/workflows/release.yml`:

- `SIGNING_STORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

`SIGNING_STORE_BASE64` is the base64-encoded keystore file.

Example:

```bash
base64 -i release.keystore | tr -d '\n'
```

## CI

Workflow:

- `.github/workflows/android-ci.yml`

Checks:

- debug APK build
- unit tests
- Android lint
- ktlint
- detekt

## Release

Workflow:

- `.github/workflows/release.yml`

Behavior:

- builds signed release APK
- verifies APK signature with `apksigner`
- publishes fixed asset name `tuneflow-tv.apk`

Stable download URL:

- [https://github.com/Venkatpandey/TuneFlow/releases/latest/download/tuneflow-tv.apk](https://github.com/Venkatpandey/TuneFlow/releases/latest/download/tuneflow-tv.apk)

## Troubleshooting

- `SIGNING_STORE_BASE64 is required`
  Missing repo secret in GitHub Actions.
- `parse error` on Fire TV
  Check min SDK, signing, and that the installed APK is the signed release APK.
- plain IP login fails
  Verify device-to-server network access and server URL.
- build fails locally because SDK is missing
  Add `local.properties` with `sdk.dir=...` or export `ANDROID_HOME` / `ANDROID_SDK_ROOT`.
