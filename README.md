![TuneFlow Banner](app/src/main/res/drawable-nodpi/tv_banner_brand.png)

# TuneFlow

[![Android CI](https://github.com/Venkatpandey/TuneFlow/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Venkatpandey/TuneFlow/actions/workflows/android-ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/Venkatpandey/TuneFlow)](https://github.com/Venkatpandey/TuneFlow/releases/latest)
[![License: MIT](https://img.shields.io/github/license/Venkatpandey/TuneFlow)](LICENSE)

TuneFlow is a native Android TV / Fire TV Navidrome client built for remote-first music browsing, large artwork, and smooth queue-based playback on the big screen.

## Why TuneFlow

- TV-first layout with visible focus states and D-pad-friendly navigation
- Album, playlist, artist, favorites, and search flows built around artwork
- Queue resume after restart
- Direct Navidrome streaming with no app-side bitrate or transcoding parameters added

## Install

### Download the latest APK

- Stable link: [tuneflow-tv.apk](https://github.com/Venkatpandey/TuneFlow/releases/latest/download/tuneflow-tv.apk)
- All releases: [GitHub Releases](https://github.com/Venkatpandey/TuneFlow/releases)

### Downloader Code

If you use the Downloader app on Fire TV / Android TV, enter code `1578499`.

- Downloader short link: [go.aftvnews.com/1578499](https://go.aftvnews.com/1578499)
- Direct APK: [tuneflow-tv.apk](https://github.com/Venkatpandey/TuneFlow/releases/latest/download/tuneflow-tv.apk)

### Fire TV / Android TV

1. Download `tuneflow-tv.apk`.
2. Open the APK from Downloader, a browser, or local file transfer.
3. Allow installs from unknown sources if your device asks.
4. Install and launch TuneFlow.

## Login

1. Open TuneFlow.
2. Enter your Navidrome URL.
   Examples:
   - `https://music.example.com`
   - `http://192.168.1.10:4533`
3. Enter your username and password.
4. Press `Login`.

## What You Can Do

- Continue listening from the saved queue
- Browse newest albums
- Open playlists with collage artwork
- Open artists and drill into their albums
- View read-only favorites from Navidrome starred items
- Search with recent queries and live suggestions
- Use play/pause/previous/next on the Now Playing screen
- Browse up to 25 ranked YouTube matches for the current song, choose one, and play it full screen
- Press Back to keep the selected video playing inside the Now Playing rail widget while browsing TuneFlow
- Logout to switch users on the same TV

## YouTube Video Setup

The first video release supports YouTube only and plays only the current TuneFlow queue item. It does not create or retain a separate video playlist.

1. Enable YouTube Data API v3 in a Google Cloud project.
2. Create an API key restricted to YouTube Data API v3.
3. Add Android application restrictions for package `com.tuneflow.tv` and each signing certificate SHA-1 used to build the app.
4. Supply the key at build time without committing it:

```bash
TUNEFLOW_YOUTUBE_API_KEY=your_restricted_key ./gradlew :app:assembleDebug
```

Without a configured key, the `Video` action is disabled. Video search starts only after the user selects it and accepts the one-time disclosure. Search uses the track title and artist, asks YouTube for the most-viewed matches, then combines view count with title, artist, duration, category, and publisher matching. Artist-channel matches remain ahead of otherwise equivalent fan uploads. TuneFlow presents ranked matches for manual selection, opens the selected video full screen, and moves it into the album-art area of the Now Playing rail widget when you return to browse. Search does not apply YouTube SafeSearch or local maturity filtering; TuneFlow still requires videos to be public, embeddable, and playable in the device region. TuneFlow uses the official YouTube Data API and IFrame Player, leaves adaptive quality, controls, branding, links, and ads intact, and never extracts or downloads media URLs. Stopping or finishing video leaves the audio track paused and returns to Now Playing. Playback pauses when TuneFlow leaves the foreground.

YouTube selects embedded playback quality from the player viewport, device capability, network conditions, and its adaptive-streaming policy. TuneFlow gives the iframe the full available 16:9 viewport in full-screen mode. The official IFrame API no longer provides a working quality setter or a supported way to force extra buffering, so TuneFlow cannot guarantee a fixed resolution without replacing or extracting the provider stream.

In full-screen video, TuneFlow gives Center, Left, Right, Up, and Down directly to the official YouTube player so its playback, captions, settings, and other exposed controls stay usable. Back returns to the mini-player and restores focus to TuneFlow's `Video` action. Remote media keys remain available for play, pause, seek, next, previous, and stop.

## Streaming Quality

TuneFlow requests playback from Navidrome using direct `stream.view` URLs and does not add `maxBitRate` or transcoding `format` parameters.

That means TuneFlow is designed to request the original stream as served by Navidrome. If you play FLAC and your Navidrome server is configured to serve the original file, TuneFlow will request that raw stream.

## Troubleshooting

- Login fails:
  Check the server URL, credentials, and whether the TV can reach your Navidrome server.
- Plain IP login fails:
  Make sure the server is reachable on your local network and the IP/port are correct.
- Empty library or missing items:
  Verify your Navidrome account has access to the content.
- Playback problems:
  Check server reachability and test the same track from another Navidrome client.

## Privacy

- Passwords are not stored directly.
- Session token data stays on-device.
- Search history is stored locally on the TV for convenience.
- YouTube search sends only current-song metadata. Navidrome credentials, server URL, queue contents, and listening history are not sent to YouTube.

## Developer Docs

Developer setup, local release signing, and CI/release workflow live in [scripts/README.md](scripts/README.md).

Remote navigation and Back behavior are documented in [docs/navigation.md](docs/navigation.md).
