# Lyrics in Aruna

Lyrics are available in the Immersive player. Tap **Lyrics** in the player controls to switch from artwork to lyrics, then tap it again to return. The lyric view occupies the player foreground, with playback controls still available.

In **Settings > Lyrics**, change the font size, toggle online lookup, or reload the current song's lyrics. A timed row seeks to its authored start when tapped. Manual browsing suspends automatic following until scrolling stops and the browsing grace period has passed.

## Sources and synchronization

`LyricsRepository` reads the local audio URI first. Fully timed embedded karaoke takes priority. When online lyrics are enabled and the local version is missing or less precisely synchronized, Aruna tries:

1. lrc.red matching by title/artist and available album/duration metadata, followed by the ISRC-based `.ttml` download.
2. lrc.red structured `.song.json` or `.lrc` data when needed.
3. A bounded LRCLIB search for line-synced lyrics if lrc.red cannot provide a suitable result.

Local lyrics can appear while online lookup runs. Only a better synchronized version replaces them. Network failures keep the available local lyrics. Reload bypasses lookup caches. Public providers receive matching metadata; these requests do not upload audio or require an Apple Music/Spotify login.

The online and embedded caches are separate. Parsed lyrics are retained for up to eight songs in memory, and the bounded disk stores retain up to 120 entries each. Confirmed misses expire after six hours; network errors are not recorded as confirmed misses. Files saved in Aruna's private pending queue are separate from the provider caches.

### Embedded metadata

| Container | Readable lyric metadata |
| --- | --- |
| MP3 and ID3-prefixed audio | ID3v2.2/2.3/2.4 USLT/ULT, millisecond SYLT/SLT, lyric TXXX/TXX frames. |
| FLAC | Vorbis lyric comments, including plain and synchronized lyric fields. |
| OGG Vorbis / Opus | Vorbis comments or OpusTags, including continued packets. |
| M4A / MP4 | `ilst` lyrics, freeform lyric fields and ID32; `moov` may follow `mdat`. |
| WAV / AIFF | Embedded ID3 chunks. |

Fields can contain plain text, LRC, Enhanced LRC or TTML. The decoder prefers authored word/syllable timing, then line timing, then plain text. It does not split a line to invent word timestamps. Start-only LRC and inherited TTML container ends remain distinguishable from reliable authored vocal ends.

Readers use bounded metadata reads and skip audio payloads where seeking is available. Unsupported compression/encryption, malformed fields, unsupported SYLT time formats and files outside the reader limits may be ignored. Supported metadata does not by itself guarantee that the audio container can be played on a given device.

### Translations and pronunciation

Available translations and pronunciation (including pinyin) appear underneath the original lyric. Karaoke and line-synced rows share this behavior. Companion text belongs to the same measured, tappable row: its height participates in automatic following, it moves with the original lyric, and tapping either text seeks to the original's start. Duet companions follow their own singer's alignment; backing companions follow their backing-vocal visibility window.

For Chinese and Japanese word-timed lyrics, pronunciation is centered directly beneath its corresponding character or kanji group. Source and reading intervals must share authored outer boundaries; a reading may cover multiple kanji, or several reading spans may cover one source group. Both texts share one layout and line break. Each cell reserves the larger text width, preventing adjacent pinyin/romaji from overlapping. A right-aligned duet shifts both text levels together, and paired height participates in lyric following. Visual spacing added for readings does not create word breaks in the source animation.

Non-CJK lyrics, missing/ambiguous timing, unsupported bidirectional mixtures, and an indivisible group wider than the viewport retain the existing separate reading row. Aruna does not guess character associations from text length or repeated words. Sentence translations retain their separate line below the paired source/pronunciation block.

The TTML decoder supports inline `ttm:role="x-translation"` and `x-roman` (also `x-romanization` and `x-transliteration`) spans. It also reads iTunes metadata `translations/translation/text` and `transliterations/transliteration/text`, resolving each `for` to the corresponding lyric `itunes:key` (or XML ID). Namespace prefixes may vary. Inline values take precedence; if metadata supplies several languages for one key, the first nonempty authored variant is used. No language selection control or automatic translation service is added.

Timed pronunciation spans retain their own `begin`/`end` or `dur`. Adjacent pinyin spans receive readable spaces. Fully timed pronunciation uses the main lyric renderer: progressive fill, connected letter waves and held-note projection/halos, drawn at the companion's smaller scale. The bold text retains its 58% white base and full-white highlight. The main engine's existing rapid-word and reliable-hold rules still apply.

The sentence translation (the third text level) keeps its own baseline and text size while the original and pronunciation animate. It does not receive the source word's lift, vertical swell or halo pulse. It retains line-level activation from 68% white to full white and the existing tap-to-seek target. Source/pronunciation animation and shared scrolling, sentence following and backing-row visibility retain their existing owners. No translation word timestamps are guessed. Pause holds playback-driven effects; seeking evaluates the destination's line activation.

Text layout and row height remain stable during expression. Setting the platform animation duration scale to zero removes pronunciation's additional geometry and the source-pose follower while keeping readable text and timing-based highlighting. Partial or untimed pronunciation retains its complete shaped-text fallback and any valid highlight ranges; in karaoke rows it can follow the source's whole-line pose. Missing timing never receives inferred vocal intervals. Companion spans cannot enter sung words, extend vocal ends, or determine duet alignment. LRC translations at the same timestamp retain their existing behavior.

These are supplied lyric annotations. Aruna does not generate translations or pronunciation when a source has none. Original TTML remains in the stored lyric and existing saved metadata. See the [companion-text change record](implementation-timeline/lyrics/2026-10-28-add-ttml-translations-and-pronunciation.md).

## Character motion and following

All lyric motion samples the playback clock. Pausing holds the current pose; a real seek evaluates the destination pose. Changing songs replaces the song's lyric state. The active frame publisher follows the activity lifecycle and stops publishing playback frames when it is not started.

- Adjacent, reliably timed units share a soft highlight cursor around word boundaries. A measured white shoulder, up to 12dp on each side, anticipates/releases by at most 90ms; short units use a smaller window. The cursor matches sweep position and velocity at the transition edges, including measured reading-cell padding and wrapped paths. Only fill crosses a boundary: held-note lift, scale and projection remain owned by the authored unit. Overlaps, gaps beyond 45ms, excessive spacing, RTL and inferred timing retain the existing fill. Reduced pronunciation motion removes geometry while retaining the same timed highlight.
- Rapid words up to 650 ms avoid the extra character wave.
- Reliable short pulls longer than 650 ms use a crest that travels across measured character centers. Rise and release use the word's duration instead of a fixed short cycle.
- Nearby short pulls with gaps up to 120 ms share one continuous crest along the measured text path, with matching speed and acceleration at word joins. The small visual wave may anticipate or trail a word by up to 180 ms; karaoke timestamps remain authored, with the bounded fill shoulder described above. Real rests, rapid words, unreliable timing and sustained notes break this connection. Short runs that cannot separate graphemes keep their baseline anchored.
- An authored held unit of at least 1,300 ms enables the separate sustained-vocal effect. Characters rise, project in 3D and release in sequence over the hold. The short-word handoff does not change that animation.
- For Han, hiragana and katakana, each authored timing unit owns its animation interval even when there are no spaces. A held unit cannot pass its lift/projection to preceding or following units. A multi-character kanji word explicitly timed as one unit retains its internal character animation. Latin word fragments can still join as before; pronunciation timestamps and the paired reading layout are unchanged.

The painter caches complete grapheme layouts, gradients, projection resources and glow palettes. Combining accents stay with their base letter. Joining scripts, RTL runs and unsupported sequences retain their shaped text runs. Fill and halo share the same position; the painter does not cut moving strips through letter strokes.

Duet alignment follows the TTML voice information, including right-aligned wrapped rows for the secondary voice. Backing vocals follow their own authored windows. Available translations and composer/lyricist credits display with the lyrics. Instrumental countdowns use authored gaps and relinquish their layout space when they finish.

Automatic following yields to manual scrolling and resumes after the browsing grace period. Each visible sentence also has a bounded visual follower: the active sentence leads and upcoming sentences catch up more gradually. Its main lyric, translation and attached backing move together; simultaneous duet onsets share a phase. The extra offset is capped at 35% of the shortest visible row height, within 8–32dp, to preserve spacing. Countdown layout stays separate.

Sentence followers retain velocity when the active cue changes, hold their pose on pause, and reset for seeks, visibility/viewport changes, reverse rejoining or manual browsing. Zero platform animation duration scale removes the additional sentence motion, including while paused. Word and syllable timing and the existing character effects are unchanged.

The view retains measurements and updates drawing/layers from playback time. Tests of timing mathematics do not establish physical-device smoothness or visual equivalence to another player. See the [sentence-motion change record](implementation-timeline/lyrics/2026-10-27-add-flowing-sentence-motion.md) for ownership and verification.

## Save lyrics to a song

A **Save** button appears when downloaded lyrics improve on the file's existing embedded lyrics, including an upgrade from plain or line-synced text to authored word/syllable timing. Original-file embedding supports **MP3, FLAC and M4A**. Other containers remain read-only for embedding.

1. On first use without a suitable stored grant, select the music folder using Android's folder picker. Grant access to a parent folder containing the songs, including any Artist/Album subfolders.
2. Aruna stores the requested lyrics in its private pending queue and refreshes the displayed lyrics from that copy. Playback continues.
3. A **clock icon** means the metadata write is pending, not completed. Switch songs so the player can release the original file. The durable queue is a draft, not evidence that another player can already read the lyric.
4. Once the song is no longer the active playback source, Aruna resolves the original inside the authorized folder, verifies its identity, prepares the tagged copy and commits the metadata update.

The folder grant persists for later songs in that tree. A different folder may need another grant. Android can restrict which storage roots may be selected. The app does not require a separate file picker for every song.

### Metadata writers

| Format | Written fields and preservation |
| --- | --- |
| MP3 | Updates canonical readable `USLT` and timed `TXXX:LYRICS`; legacy owned lyric descriptions are replaced. Described alternate translations, unrelated tags, artwork and MPEG frames are retained. |
| FLAC | Updates ordinary `LYRICS` and timed `ARUNA_LYRICS`; legacy owned lyric fields are migrated. Unrelated metadata blocks, artwork and audio frames are retained. |
| M4A | Updates ordinary `©lyr` and the owned `com.aruna:LYRICS` freeform atom. Legacy owned freeforms are migrated; audio offsets and unrelated atoms are retained. |

The reader still accepts legacy owned fields in existing files. Only an explicit Save updates the selected song; existing files are not bulk-retagged. Saving does not convert or re-encode the audio.

Before writing, Aruna compares the authorized file with the playing URI using a whole-file SHA-256 digest. It creates a backup and staged output, then verifies that `EmbeddedLyricsReader` can read the staged timing. After committing, it verifies the original against the staged output. A failed write attempts and verifies rollback; if rollback fails, the backup remains in Aruna's data folder, separate from disposable caches. Unsupported formats are rejected before queuing; unsafe metadata layouts are rejected rather than reported as successful writes. A failed Save offers a folder-access review on retry.

Queued lyrics and completed file embedding are separate states. Verify completion before expecting another music player or tag editor to read the newly embedded metadata.

## Verification

From the repository root, use JDK 21 and the configured Android SDK:

~~~sh
./gradlew :app:testDebugUnitTest
~~~

On Windows PowerShell, use `.\gradlew.bat :app:testDebugUnitTest`. This command runs checks without packaging an APK.

Fixtures cover supported metadata containers, malformed input, lyric selection, saved timing, cache behavior and file-writer round trips. Motion tests cover pause/seek behavior, short-word handoffs, sustained characters, backing windows and simulated 60/90/120 Hz samples. Robolectric/Compose tests cover selected UI behavior.

On an Android device, verify:

- Online lookup enabled/disabled, offline fallback and reload after retagging.
- Short pulls, long holds, adjacent words, duet sides and backing vocals with a known TTML track.
- Tap-to-seek, pause/resume, manual scrolling, track changes and large lyric fonts.
- Save from a nested authorized folder for each supported write format, including app restart while queued and completion after changing tracks.
- Independent tag-editor readback, unchanged audio/artwork and storage-provider error handling.
- Frame pacing and touch behavior with animated artwork enabled on the actual device.

For the implementation, start with [LyricsRepository](../app/src/main/kotlin/com/yohandeku32/nusamusic/data/LyricsRepository.kt), [SyllableLyricsPainter](../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/SyllableLyricsPainter.kt) and [AudioLyricsEmbedder](../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AudioLyricsEmbedder.kt). Return to the [main README](../README.md) for setup and build commands.
