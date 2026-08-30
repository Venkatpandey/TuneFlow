# SmartTube-native YouTube experiment

This module provides SmartTube-native YouTube search and playback for normal debug and release variants. This integration has not been validated for public distribution.

The public boundary is intentionally narrow: `YouTubeNativeSearchClient`, `YouTubeNativePlayer`, `YouTubeNativePlayerState`, `YouTubeQuality`, and `YouTubeVideoFormat`. The native implementation uses SmartTube's Innertube format resolver and its separate ExoPlayer 2.10 fork. It does not interact with TuneFlow's Media3 audio player.

## Pinned upstream revisions

- SmartTube: `489953341eb44b43d5fe23e359321d8554469133`
- MediaServiceCore: `041b5974e0fc29fd17c53d91c4be4644a3e4a603`
- SharedModules: `00fb0fc1a4753bab1892d43ed96fe1b3acbdfe5d`

Reference paths used during extraction:

- Search: `common/.../SearchPresenter.java`, `MediaServiceCore/youtubeapi/.../SearchService2.kt`
- Resolution: `MediaServiceCore/youtubeapi/.../YouTubeMediaItemService.java`
- Source selection: `common/.../VideoLoaderController.java`
- Playback sources: `common/.../ExoMediaSourceFactory.java`
- Track selection: `common/.../TrackSelectorManager.java`

The integration packages only locally built artifacts from the pinned revisions. It has no absolute sibling-repository dependency at configuration or runtime.

## Rebuilding local artifacts

From the pinned SmartTube checkout, initialize submodules and use JDK 11:

```sh
git submodule update --init --recursive
bash gradlew \
  :youtubeapi:assembleStbetaDebug \
  :mediaserviceinterfaces:assembleDebug \
  :sharedutils:assembleDebug \
  :commons-io-2.8.0:assembleDebug \
  :j2v8:assembleDebug \
  :exoplayer-library-core:assembleDebug \
  :exoplayer-library-dash:assembleStbetaDebug \
  :exoplayer-library-sabr:assembleStbetaDebug \
  :exoplayer-library-hls:assembleDebug
```

Copy the resulting AARs into `libs/` using the filenames below. Expected SHA-256:

| Artifact | SHA-256 |
| --- | --- |
| `commons-io-2.8.0-debug.aar` | `063c9ccc0fdfe194d579b25993f306d5b9f87af522be3552dfe21e26d600bfbe` |
| `exoplayer-library-core-debug.aar` | `34c638f578f8b85f4643eeb33b175c923316e51844ecfa0e2a00fde553dcf3da` |
| `exoplayer-library-dash-stbeta-debug.aar` | `5cb67e81ada5a24932d7d042adc92092f49c78ab40a601616f678dfe2506df05` |
| `exoplayer-library-hls-debug.aar` | `151a7c09f5a013489969389823f45971fc6a5369fdef48f3dcf1b1e34856f6ca` |
| `exoplayer-library-sabr-stbeta-debug.aar` | `175fa7d307f5bbc886964feb2558773f6738e73c79386bcb941364d09886f144` |
| `j2v8-debug.aar` | `ebc248ac50109e0dddc928daa3995acbef3f8d50021f67ce01cb3ee70e03bb13` |
| `mediaserviceinterfaces-debug.aar` | `705df62b2859303436e1f57484bd85892252995c91dfac2e505a67f226d96320` |
| `sharedutils-debug.aar` | `fb32169a0e9a280d655dd5bbe00b0c48da4ddc7fe596616d8508972f48bc9b3e` |
| `youtubeapi-stbeta-debug.aar` | `59c0c020e43cf2de22667ffaf754098f769133e11bef37b65c560a19ade3ada6` |

## Scope and policy

Included: video-only Innertube search, candidate metadata, signature/PO-token resolution supplied by the pinned resolver, DASH, SABR, HLS/direct fallback, native aspect-fit surface, hardware-aware quality selection, captions, and player state/errors.

Excluded: accounts, sign-in, browse UI, history, subscriptions, suggestions, playlists, SponsorBlock, downloads, caching, background playback, updates, analytics, and Leanback UI. This integration does not block, remove, rewrite, or manipulate advertisements.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for license notes.
