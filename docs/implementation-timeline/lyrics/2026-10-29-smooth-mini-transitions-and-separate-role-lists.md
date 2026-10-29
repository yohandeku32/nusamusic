# Smooth mini lyric changes and separate TTML role lists

Date: 2026-10-29

## Report and diagnosis

The listener reported abrupt mini lyric line changes and occasional second-row annotations appearing inside the main line. The mini view replaced its keyed line immediately with no boundary transition. A line-synced native regression confirmed identical pixels 30ms and 240ms after a new line began, rather than an entrance.

The TTML adapter compared the entire role attribute to a single annotation role. TTML permits a whitespace-separated list of roles. A controlled paragraph with `lyrics x-roman` and `x-provider-note x-translation` decoded its source as `晚安Selamat malam` instead of `晚安`. The original user-reported songs have not been identified; this is a reproduced format-level cause, not confirmation of every source file on the device.

Reference: [W3C TTML role syntax](https://www.w3.org/TR/ttml1/Overview.html#metadata-attribute-role).

## Repair

MiniLyricsTimeline prepares visibility windows from main-line starts/ends and long instrumental gaps. A sorted boundary sweep preserves the existing newest-main/first-authored duet choice, including returning to an overlapping voice and resuming after a long gap. Each window has a playback-driven quintic entrance up to 180ms and exit up to 120ms, shortened for brief windows. One row fades and moves at most 4dp in / 3dp out inside a clip. It does not retain an outgoing lyric or mount a future lyric. Row height and title/transport layout stay stable.

The transition samples the same playback position as the words. Pause freezes both; seek evaluates the destination without a wall-clock animation or stale outgoing row. Zero system animation scale removes the additional fade/slide. Timed words still use the main syllable painter, and line-synced text receives the same row transition without invented word timing.

TtmlCompanionReader now recognizes complete role tokens, including XML whitespace separators, for translation, pronunciation and backing. Companion discovery traverses neutral wrappers and stops at backing boundaries, so backing annotations do not leak into the lead. The normalizer excludes recognized annotations from sung text and canonicalizes backing-role lists only in its adapter XML for the upstream parser. Stored original TTML is untouched. Unknown role prefixes remain ordinary source content; no annotations are guessed from language or text length.

Main layout ownership is unchanged: a sentence translation remains below the main/pronunciation block, paired CJK pronunciation stays under its characters, and the fallback pronunciation row stays separate. The third translation row retains its stable geometry. On-disk lyrics retain raw content and are decoded through the updated reader; no cache version reset or file metadata write is needed.

## Verification

The original role-list merge and missing mini entrance failed before the repair and passed afterward. Additional coverage checks neutral annotation wrappers/backing isolation, whole-token matching, tab-separated roles and backing identity, native second/third-row placement throughout a held note, brief windows, duet return/gap resumption, 60/90/120Hz transition samples, pause and exact backward-seek pixel restoration.

Android compilation, 513 focused lyric/metadata/writer/player-scale tests and lintDebug passed, with zero test failures/errors/skips and zero lint errors. Native previews use synthetic lyrics, not the listener's unidentified files. Physical-device frame pacing and the listener's exact affected tracks remain acceptance checks. No APK is assembled.
