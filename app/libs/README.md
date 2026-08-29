# Media3 FLAC decoder

`media3-decoder-flac-1.4.1.aar` provides software FLAC decoding for devices
without a platform FLAC decoder, including Android API 25 devices.

Provenance:

- AndroidX Media3 tag `1.4.1`, commit
  `c35a9d62baec57118ea898e271ac66819399649b`.
- Media3 `libraries/decoder_flac` module, built with Android NDK r21.
- libFLAC `1.3.2`, downloaded from the official Xiph release mirror.
- AAR SHA-256:
  `53adef08e931f4a40357f7da62ff5518ed18113d4ec11e22c42f3c48d3a90ecb`.
- Included ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.

Media3 is licensed under Apache License 2.0; see `LICENSE-media3.txt`. libFLAC
is distributed under the license in `LICENSE-libFLAC.txt`. Build and usage instructions are in the
[Media3 decoder_flac README](https://github.com/androidx/media/tree/1.4.1/libraries/decoder_flac).
