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

## Screenshots

<table>
  <tr>
    <td><img src="docs/screenshots/home.png" alt="TuneFlow Home screen"></td>
    <td><img src="docs/screenshots/albums.png" alt="TuneFlow Albums screen"></td>
  </tr>
  <tr>
    <td align="center"><sub>Home</sub></td>
    <td align="center"><sub>Albums</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/now-playing-video.png" alt="TuneFlow Now Playing screen with video"></td>
    <td><img src="docs/screenshots/now-playing-lyrics.png" alt="TuneFlow Now Playing screen with synchronized lyrics"></td>
  </tr>
  <tr>
    <td align="center"><sub>Now Playing video</sub></td>
    <td align="center"><sub>Now Playing with lyrics</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/album-detail.png" alt="TuneFlow album detail screen"></td>
    <td><img src="docs/screenshots/search.png" alt="TuneFlow Search screen"></td>
  </tr>
  <tr>
    <td align="center"><sub>Album detail</sub></td>
    <td align="center"><sub>Search</sub></td>
  </tr>
</table>

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
- Reopen the 5 most recent videos from Home or show the last 20
- Press Back to keep the selected video playing inside the Now Playing rail widget while browsing TuneFlow
- Logout to switch users on the same TV

## YouTube Video Setup

The private video experiment supports YouTube through the SmartTube-native search and player integration. It starts every selected video from the beginning and pauses audio immediately. The remote play/pause key controls video while a video session is active and returns to audio control after video stops.

Native search works without a Google API key.

Video search starts only after the user selects it and accepts the one-time disclosure. TuneFlow ranks matches using title, artist, duration, category, publisher, and view count. The native player defaults to the highest supported quality, preserves the video's aspect ratio, and exposes quality and caption controls. Stopping, finishing, or failing video leaves audio paused and returns control to audio. Playback pauses when TuneFlow leaves the foreground.

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
