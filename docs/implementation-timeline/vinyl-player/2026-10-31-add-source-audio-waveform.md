# Add a source-audio waveform to Vinyl

> Date: 2026-10-31
> Status: verified locally; physical-device decoding verification pending
> Change type: playback presentation and local audio analysis
> Scope: Vinyl seek track; existing transport, metadata geometry, and other player modes remain intact

## Behavior

Replace the straight seek track with a 24 dp symmetric amplitude envelope derived from the selected local song. Android MediaExtractor and MediaCodec produce PCM on an IO dispatcher; raw PCM tracks bypass the codec. Timestamped frames map into 256 temporal peak bins, combining channel magnitudes so opposite-phase stereo cannot cancel the signal. Signed 16/24/32-bit, unsigned 8-bit, and floating-point PCM are supported. Rendering normalizes the track's maximum and applies a square-root display curve, preserving silence and relative dynamics rather than inventing decorative oscillation.

The played portion is dark, the remaining envelope gray, and a thin cursor shows position. The gray has 3.31:1 contrast against the cream page; dark ink has 15.84:1. The existing 48 dp Material Slider, timestamps, touch/keyboard/accessibility seeking, and 36 dp alignment are retained. An explicit invisible 4 dp thumb makes the 2 dp track inset deterministic; drawing compensates that inset independently of waveform height. Loading, unavailable, and ready states have localized accessibility descriptions. Loading, silence, or unsupported formats retain a usable straight seek track. No microphone permission, upload, source-file write, new dependency, or FFmpeg binary is introduced.

Analysis runs only for the selected Vinyl player page. A track-keyed composition state immediately clears the previous song's envelope. Changing track, player mode, or selected page cancels pending work. One mutex serializes decoding and merges cacheable simultaneous consumers. Decoding has a cooperative 45-second budget and accepts local file/content URIs and durations through 24 hours. Native/provider calls can delay cancellation until they return. Only the current codec buffer and fixed peak array are retained, rather than full-song PCM.

## Cache and failures

The versioned private cache uses URI, source size, modification time, and song duration. Providers without a reliable modification stamp bypass caching. Up to 32 results remain in memory and 256 on disk. Each disk entry is 1,044 bytes: magic/version, duration/count, and 256 finite peak floats. Invalid/truncated entries recompute; failed, cancelled, or incomplete decoding is not published or persisted. Temporary files are cleaned and only waveform files are pruned. Cache I/O failure does not disable playback or seeking.

FFmpeg is optional for playback in this repository, but its Media3 renderer is not used by this analyzer. A file playable only through that extension may fall back to the straight track. Android codec/container support varies by device. The first visit needs to read/decode the song; later valid cache hits reuse its peaks.

## Verification

All 44 selected tests pass: 10 PCM, 7 cache, 2 composition lifecycle, 17 native player UI, 1 native artwork, 3 lyrics integration, and 4 presentation preference checks. PCM cases cover temporal dynamics, silence, timestamps, unsigned and signed PCM encodings, nonfinite floats, opposite-phase stereo, empty output, and incomplete frames. Cache checks prove memory/process-restored reuse, source invalidation, unknown-revision bypass, corrupt-entry recovery, single flight, cancellation, and bounded eviction.

Composition tests prove immediate clearing on track changes, rejection of late cancelled results, and no analysis while the page is inactive. They synchronize through observable UI states rather than a wall-clock polling loop. Native UI tests render peaks produced by the production accumulator from an explicitly labelled synthetic PCM fixture: quiet/loud/silent/loud sections. Pixel checks distinguish amplitudes, silence, played/unplayed color, and exact content-edge alignment. Semantic seeking reaches 3 seconds in its 4-second duration; touch, keyboard, loading fallback, all earlier compact/wide layouts, and control actions remain covered.

The waveform fixture render was inspected at 411 x 914 dp. Its placeholder album/profile and test PCM are not claimed as a commercial song or live Apple image. Final debug assembly, Android Lint (zero errors), APK v2 signature, whitespace, and documentation links pass. Physical-device decoding of compressed user songs, live content-provider behavior, startup latency on a phone, and TalkBack require device verification; no Android device is attached in this session. No first-decode speed guarantee is claimed.

## Sources

- [PCM accumulator](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AudioWaveform.kt)
- [Cache](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AudioWaveformCache.kt)
- [Android decoder](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AudioWaveformLoader.kt)
- [Envelope and composition lifecycle](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylWaveform.kt)
- [Native UI regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
- [PCM checks](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/data/AudioWaveformTest.kt)
- [Cache checks](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/data/AudioWaveformCacheTest.kt)
- [Composition checks](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylWaveformStateTest.kt)
- [Android supported formats](https://developer.android.com/media/platform/supported-formats)
- [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec)
