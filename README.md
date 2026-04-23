# TuneFlow

TuneFlow is a native Android TV / Fire TV Navidrome client focused on fast remote-first browsing and stable queue-based playback.

## Features

- Login with Navidrome URL + username + password
- Auto-login with locally persisted token session
- Browse albums with pagination
- Album detail with track list and one-click Play Album
- Browse playlists and play full list or from a selected track
- Debounced search across artists, albums, and tracks
- Queue-first playback with Media3 + MediaSessionService
- Resume queue/index/position after restart
- Dark high-contrast TV UI with visible focus states

## Requirements

- A running Navidrome server
- Android TV or Fire TV device (or emulator)
- Android SDK + Java 17 for local builds

## Install on Firestick (Downloader)

1. Build or download `tuneflow-tv.apk` from GitHub Releases.
2. Host APK at a reachable URL (or transfer locally).
3. Open Downloader on Firestick.
4. Enter the APK URL and install.
5. Launch TuneFlow from Apps.

Stable latest-release URL:

- `https://github.com/Venkatpandey/TuneFlow/releases/latest/download/tuneflow-tv.apk`

## Login

1. Open TuneFlow.
2. Enter your Navidrome base URL (example: `https://music.example.com`).
3. Enter username + password.
4. Press Login.

## Playback behavior

- Queue is the source of truth (not only ExoPlayer playlist state).
- Supports play/pause, next/prev, seek, and queue replacement.
- Resume restores queue, current index, and current position.

## Screenshots

Place TV screenshots under `docs/screenshots/` and update this section:

- `docs/screenshots/login.png`
- `docs/screenshots/albums.png`
- `docs/screenshots/now-playing.png`

## Troubleshooting

- Login fails: verify URL, credentials, and server SSL certificate.
- Empty library: verify your Navidrome content and account permissions.
- Playback issues: check server reachability and stream endpoint access.

## Privacy

- Credentials are not logged.
- Raw password is not persisted.
- Session token data is stored locally on-device only.

## CI/CD

- On PR/push: debug build, unit tests, lint, ktlint, and detekt.
- On tag `v*`: signed release APK build and GitHub Release publish.

## Release signing setup

GitHub Releases require these repository or organization secrets:

- `SIGNING_STORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Create `SIGNING_STORE_BASE64` from your keystore file:

```bash
base64 -i release.keystore | tr -d '\n'
```

Add the output as the secret value, then create the matching password and alias secrets in GitHub:

- Repository: `Settings -> Secrets and variables -> Actions`
- Or organization-level Actions secrets if this repo is allowed to use them

If `SIGNING_STORE_BASE64 is required` appears in the release workflow, GitHub Actions does not have access to that secret.

## Manual release checklist

- [ ] Login success and failure paths
- [ ] Albums pagination loads correctly
- [ ] Playlist playback works
- [ ] Search returns results and plays tracks
- [ ] Playback controls are stable
- [ ] Resume after restart restores state
- [ ] Remote navigation stays smooth with visible focus
