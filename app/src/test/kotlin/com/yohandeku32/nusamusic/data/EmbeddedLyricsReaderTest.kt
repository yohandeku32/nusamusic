package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmbeddedLyricsReaderTest {
    private fun be(value: Int) = ByteBuffer.allocate(4).putInt(value).array()
    private fun le(value: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    private fun text(value: String) = value.toByteArray(Charsets.UTF_8)
    private fun zero(value: String) = text(value) + byteArrayOf(0)
    private fun safe(value: Int) = byteArrayOf((value ushr 21).toByte(), ((value ushr 14) and 127).toByte(), ((value ushr 7) and 127).toByte(), (value and 127).toByte())
    private fun frame(name: String, data: ByteArray, version: Int = 3) = text(name) + (if (version == 4) safe(data.size) else be(data.size)) + byteArrayOf(0, 0) + data
    private fun tag(vararg frames: ByteArray, version: Int = 3): ByteArray {
        val body = frames.fold(byteArrayOf()) { all, next -> all + next }
        return text("ID3") + byteArrayOf(version.toByte(), 0, 0) + safe(body.size) + body
    }
    private fun uslt(value: String) = frame("USLT", byteArrayOf(3) + text("eng") + zero("") + text(value))
    private fun sylt(vararg units: Pair<Int, String>, format: Int = 2): ByteArray = frame("SYLT",
        byteArrayOf(3) + text("eng") + byteArrayOf(format.toByte(), 1, 0) +
            units.fold(byteArrayOf()) { all, unit -> all + zero(unit.second) + be(unit.first) })
    private fun read(data: ByteArray): List<String> = EmbeddedLyricsReader(data.size.toLong()) { offset, count ->
        data.copyOfRange(offset.toInt(), minOf(data.size, offset.toInt() + count))
    }.read()
    private fun select(data: ByteArray) = EmbeddedLyricsSelector.select(read(data), 10_000)
    private fun comments(vararg fields: String) = le(0) + le(fields.size) + fields.fold(byteArrayOf()) { all, next -> all + le(text(next).size) + text(next) }
    private fun atom(name: String, body: ByteArray) = be(body.size + 8) + name.toByteArray(Charsets.ISO_8859_1) + body
    private fun oggPage(packet: ByteArray, serial: Int = 1, continued: Boolean = false, incomplete: Boolean = false): ByteArray {
        val laces = MutableList(packet.size / 255) { 255.toByte() }
        if (packet.size % 255 > 0 || !incomplete) laces += (packet.size % 255).toByte()
        val header = text("OggS") + byteArrayOf(0, if (continued) 1 else 2) + ByteArray(8) + le(serial) + le(0) + le(0) + byteArrayOf(laces.size.toByte())
        return header + laces.toByteArray() + packet
    }

    @Test fun readsPlainUsltAndDoesNotSynchroniseIt() {
        val result = select(tag(uslt("Hello\nworld")))
        assertEquals("Hello\nworld", result.stored.content)
        assertTrue(result.stored.plain)
        assertNull(result.synced)
    }
    @Test fun embeddedEnhancedLrcWinsOverLineAndPlainCandidates() {
        val result = select(tag(uslt("Plain"), uslt("[00:01]Whole line"), uslt("[00:01]<00:01>hel<00:02>lo<00:03>")))
        val line = result.synced!!.lines.single() as KaraokeLine
        assertEquals(listOf("hel", "lo"), line.syllables.map { it.content })
        assertEquals(listOf(1000, 2000), line.syllables.map { it.start })
    }
    @Test fun readsSyltSyllablesAndNewlinesWithoutSplittingWords() {
        val lines = select(tag(sylt(1000 to "ber", 1400 to "sa", 1800 to "ma", 3000 to "\nNext line"))).synced!!.lines
        assertEquals(2, lines.size)
        val first = lines[0] as KaraokeLine
        assertEquals(listOf("ber", "sa", "ma"), first.syllables.map { it.content })
        assertEquals(listOf(1000, 1400, 1800), first.syllables.map { it.start })
        assertEquals("Next line", (lines[1] as SyncedLine).content)
    }
    @Test fun readsSyltWordUnitsAndRepairsNonMonotonicOrder() {
        val result = select(tag(sylt(2000 to "world", 1000 to "Hello ")))
        val units = (result.synced!!.lines.single() as KaraokeLine).syllables
        assertEquals(listOf(1000, 2000), units.map { it.start })
        assertEquals("Hello world", units.joinToString("") { it.content })
    }
    @Test fun frameCountSyltNeverMasqueradesAsMilliseconds() {
        val result = select(tag(sylt(100 to "Timing", format = 1), uslt("Fallback")))
        assertTrue(result.stored.plain)
        assertEquals("Fallback", result.stored.content)
    }
    @Test fun utf16UsltAndV24UserTextAreSupported() {
        val utf16 = frame("USLT", byteArrayOf(1) + text("eng") + "".toByteArray(Charsets.UTF_16) + byteArrayOf(0, 0) + "Halo 世界".toByteArray(Charsets.UTF_16))
        assertEquals("Halo 世界", select(tag(utf16)).stored.content)
        val txxx = frame("TXXX", byteArrayOf(3) + zero("LYRICS") + text("[00:01]Halo"), 4)
        assertTrue(select(tag(txxx, version = 4)).synced!!.lines.single() is SyncedLine)
    }
    @Test fun flacVorbisLyricsAndMultipleFieldsAreSupported() {
        val data = comments("TITLE=Song", "LYRICS=Plain", "SYNCEDLYRICS=[00:01]Halo")
        val flac = text("fLaC") + byteArrayOf(0x84.toByte(), (data.size ushr 16).toByte(), (data.size ushr 8).toByte(), data.size.toByte()) + data
        assertEquals("Halo", (select(flac).synced!!.lines.single() as SyncedLine).content)
    }
    @Test fun readsVorbisAndOpusTagsIncludingSplitPackets() {
        val fields = comments("LYRICS=Embedded text")
        assertEquals("Embedded text", select(oggPage(byteArrayOf(3) + text("vorbis") + fields)).stored.content)
        assertEquals("Embedded text", select(oggPage(text("OpusTags") + fields)).stored.content)
        val packet = text("OpusTags") + comments("LYRICS=" + "x".repeat(600))
        val split = oggPage(packet.copyOfRange(0, 255), incomplete = true) + oggPage(packet.copyOfRange(255, packet.size), continued = true)
        assertEquals("x".repeat(600), select(split).stored.content)
    }
    @Test fun m4aLyricsAfterMdatAndFreeformEnhancedLrcAreSupported() {
        val value = atom("data", be(1) + be(0) + text("M4A lyrics"))
        val data = atom("ftyp", text("M4A ") + be(0)) + atom("mdat", ByteArray(1024)) +
            atom("moov", atom("udta", atom("meta", be(0) + atom("ilst", atom("©lyr", value)))))
        assertEquals("M4A lyrics", select(data).stored.content)
        val freeform = atom("----", atom("mean", be(0) + text("com.apple.iTunes")) +
            atom("name", be(0) + text("LYRICS")) + atom("data", be(1) + be(0) + text("[00:01]<00:01>Hi<00:02>")))
        val m4a = atom("ftyp", text("M4A ") + be(0)) + atom("moov", atom("meta", be(0) + atom("ilst", freeform)))
        assertTrue(select(m4a).synced!!.lines.single() is KaraokeLine)
    }
    @Test fun missingAndMalformedMetadataFailClosed() {
        assertEquals("", select(ByteArray(100)).stored.content)
        assertEquals("", select(text("ID3") + byteArrayOf(3, 0, 0, 127, 127, 127, 127)).stored.content)
        val invalid = frame("USLT", byteArrayOf(9, 0, 0, 0, 0))
        assertEquals("Valid", select(tag(invalid, uslt("Valid"))).stored.content)
        assertNull(EmbeddedLyricsSelector.select(listOf("[00:99]bad"), 5000).synced)
        assertEquals("", EmbeddedLyricsSelector.select(listOf("[00:99]bad"), 5000).stored.content)
    }
    @Test fun doesNotReadLargeMediaPayloadToReachTailMetadata() {
        val ftyp = atom("ftyp", text("M4A ") + be(0))
        val mdatHeader = be(100_000_000) + text("mdat")
        val moov = atom("moov", atom("udta", atom("meta", be(0) + atom("ilst", atom("©lyr", atom("data", be(1) + be(0) + text("Tail")))))))
        val tail = ftyp.size + 100_000_000L
        var totalRead = 0
        val result = EmbeddedLyricsReader(tail + moov.size) { offset, count ->
            totalRead += count
            ByteArray(count) { index ->
                val at = offset + index
                when {
                    at < ftyp.size -> ftyp[at.toInt()]
                    at < ftyp.size + 8 -> mdatHeader[(at - ftyp.size).toInt()]
                    at >= tail -> moov[(at - tail).toInt()]
                    else -> 0
                }
            }
        }.read()
        assertEquals(listOf("Tail"), result)
        assertTrue(totalRead < 1024)
    }
    @Test fun syltWithOneAuthoredTimestampPerLineUsesLineFocus() {
        val lines = select(tag(sylt(1000 to "First full line", 3000 to "\nSecond full line"))).synced!!.lines
        assertTrue(lines.all { it is SyncedLine })
        assertEquals(listOf(1000, 3000), lines.map { it.start })
    }
    @Test fun wavAndAiffId3ChunksAreReadWithoutAudioPayloadScanning() {
        val embedded = tag(uslt("Chunk lyrics"))
        val pad = if (embedded.size % 2 == 1) byteArrayOf(0) else byteArrayOf()
        val wav = text("RIFF") + le(embedded.size + pad.size + 12) + text("WAVE") + text("id3 ") + le(embedded.size) + embedded + pad
        val aiff = text("FORM") + be(embedded.size + pad.size + 12) + text("AIFF") + text("ID3 ") + be(embedded.size) + embedded + pad
        assertEquals("Chunk lyrics", select(wav).stored.content)
        assertEquals("Chunk lyrics", select(aiff).stored.content)
    }
    @Test fun id3v22LyricsAndV23UnsynchronisationAreSupported() {
        val payload = byteArrayOf(0) + text("eng") + byteArrayOf(0) + "Café".toByteArray(Charsets.ISO_8859_1)
        val ult = text("ULT") + byteArrayOf(0, 0, payload.size.toByte()) + payload
        val v22 = text("ID3") + byteArrayOf(2, 0, 0) + safe(ult.size) + ult
        assertEquals("Café", select(v22).stored.content)
        val withFf = byteArrayOf(0) + text("eng") + byteArrayOf(0) + byteArrayOf(0xFF.toByte(), 0xE0.toByte())
        val original = frame("USLT", withFf)
        val expanded = original.flatMap { if (it == 0xFF.toByte()) listOf(it, 0.toByte()) else listOf(it) }.toByteArray()
        val unsynced = text("ID3") + byteArrayOf(3, 0, 0x80.toByte()) + safe(expanded.size) + expanded
        assertEquals("ÿà", select(unsynced).stored.content)
    }

    private fun metadata(data: ByteArray) = EmbeddedLyricsReader(data.size.toLong()) { offset, count ->
        data.copyOfRange(offset.toInt(), minOf(data.size, offset.toInt() + count))
    }.readMetadata()

    @Test fun id3CreditsKeepComposerAndLyricistRolesSeparate() {
        val data = tag(uslt("Lyrics"),
            frame("TCOM", byteArrayOf(3) + text("First\u0000Second")),
            frame("TEXT", byteArrayOf(1) + "Penulis 世界".toByteArray(Charsets.UTF_16)),
            frame("TPE1", byteArrayOf(3) + text("Not a composer")))
        val result = metadata(data)
        assertEquals(listOf("First", "Second"), result.credits.composers)
        assertEquals(listOf("Penulis 世界"), result.credits.lyricists)
        assertEquals(listOf("Lyrics"), result.lyrics)
        assertEquals(result.credits, EmbeddedLyricsSelector.select(result.lyrics, 10000, result.credits).stored.credits)
    }
    @Test fun vorbisCreditsDeduplicateWithoutSplittingNamesOnSlashes() {
        val data = oggPage(text("OpusTags") + comments("COMPOSER=AC/DC", "COMPOSER=AC/DC",
            "COMPOSER=Other", "LYRICIST=Writer", "ARTIST=Performer"))
        assertEquals(SongCredits(listOf("AC/DC", "Other"), listOf("Writer")), metadata(data).credits)
    }
    @Test fun mp4ComposerAndFreeformLyricistAreReadAfterMediaData() {
        fun value(text: String) = atom("data", be(1) + be(0) + text(text))
        val freeform = atom("----", atom("name", be(0) + text("LYRICIST")) + value("Words"))
        val data = atom("ftyp", text("M4A ") + be(0)) + atom("mdat", ByteArray(1000)) +
            atom("moov", atom("meta", be(0) + atom("ilst", atom("©wrt", value("Music")) + freeform)))
        assertEquals(SongCredits(listOf("Music"), listOf("Words")), metadata(data).credits)
    }
    @Test fun absentBlankAndOversizedCreditsNeverInventNames() {
        assertTrue(metadata(tag(uslt("Lyrics"))).credits.isEmpty)
        val data = tag(frame("TCOM", byteArrayOf(3) + text("  ")),
            frame("TEXT", byteArrayOf(3) + text("x".repeat(9000))))
        assertTrue(metadata(data).credits.isEmpty)
    }
    @Test fun legacyId3v22ComposerAndTextWriterRemainSupported() {
        fun oldFrame(key: String, name: String): ByteArray {
            val payload = byteArrayOf(3) + text(name)
            return text(key) + byteArrayOf(0, 0, payload.size.toByte()) + payload
        }
        val body = oldFrame("TCM", "Music") + oldFrame("TXT", "Words")
        val data = text("ID3") + byteArrayOf(2, 0, 0) + safe(body.size) + body
        assertEquals(SongCredits(listOf("Music"), listOf("Words")), metadata(data).credits)
    }
    @Test fun m4aExternalCopyrightLyricsEditOverridesOldCustomKaraoke() {
        fun data(value: String) = atom("data", be(1) + be(0) + text(value))
        val originalSynced = "[00:01]<00:01>Old <00:02>version<00:04>"
        val ownTimed = atom("----",
            atom("mean", be(0) + text("com.nusamusic")) +
                atom("name", be(0) + text("LYRICS")) + data(originalSynced))
        // External editors change the user-visible ©lyr, but leave our freeform
        // timed copy in place. Other players see "New version" immediately.
        val meta = atom("meta", be(0) + atom("ilst",
            atom("©lyr", data("New version")) + ownTimed))
        val file = atom("ftyp", text("M4A ") + be(0)) +
            atom("mdat", ByteArray(20)) + atom("moov", atom("udta", meta))
        val parsed = metadata(file)
        assertEquals("New version", parsed.preferredPlainLyrics)
        assertEquals(2, parsed.lyrics.size)
        assertEquals("New version", selectLatestEmbeddedLyrics(parsed, 8_000).stored.content)
        assertTrue(selectLatestEmbeddedLyrics(parsed, 8_000).stored.plain)
    }

    @Test fun m4aMatchingCopyrightLyricsRetainSyncedTiming() {
        fun data(value: String) = atom("data", be(1) + be(0) + text(value))
        val timed = "[00:01]<00:01>Hello <00:02>world<00:04>"
        val own = atom("----", atom("name", be(0) + text("LYRICS")) + data(timed))
        val file = atom("ftyp", text("M4A ") + be(0)) +
            atom("moov", atom("udta", atom("meta", be(0) + atom("ilst",
                atom("©lyr", data("Hello world")) + own))))
        val selected = selectLatestEmbeddedLyrics(metadata(file), 8_000)
        assertEquals(timed, selected.stored.content)
        assertTrue(selected.synced!!.lines.single() is KaraokeLine)
    }

    @Test fun quickTimeStyleM4aMetaWithoutFullBoxHeaderReadsEditedLyrics() {
        val data = atom("data", be(1) + be(0) + text("Externally updated lyrics"))
        val meta = atom("meta", atom("ilst", atom("©lyr", data)))
        val file = atom("ftyp", text("M4A ") + be(0)) +
            atom("mdat", ByteArray(50)) + atom("moov", atom("udta", meta))
        assertEquals("Externally updated lyrics",
            selectLatestEmbeddedLyrics(metadata(file), 15_000).stored.content)
    }

    @Test fun m4aInPlaceMetadataRefreshReadsCurrentBytesInsteadOfFirstResult() {
        fun sample(lyric: String): ByteArray {
            val data = atom("data", be(1) + be(0) + text(lyric))
            return atom("ftyp", text("M4A ") + be(0)) +
                atom("moov", atom("udta", atom("meta", be(0) + atom("ilst",
                    atom("©lyr", data)))))
        }
        // Equivalent byte length: a tag editor can overwrite without changing
        // the MediaStore SIZE field or advancing its generation immediately.
        var bytes = sample("First lyrics")
        val originalSize = bytes.size
        fun fresh() = EmbeddedLyricsReader(bytes.size.toLong()) { offset, count ->
            bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + count))
        }.readMetadata()
        assertEquals("First lyrics", selectLatestEmbeddedLyrics(fresh(), 10_000).stored.content)
        bytes = sample("Other lyrics")
        assertEquals(originalSize, bytes.size)
        assertEquals("Other lyrics", selectLatestEmbeddedLyrics(fresh(), 10_000).stored.content)
    }

    @Test fun quickTimeUserDataCopyrightLyricsWithoutNestedDataAreReadable() {
        val lyric = text("Lyrics from external editor")
        val payload = byteArrayOf((lyric.size ushr 8).toByte(),
            lyric.size.toByte(), 0, 0) + lyric
        val file = atom("ftyp", text("M4A ") + be(0)) +
            atom("moov", atom("udta", atom("©lyr", payload)))
        val result = metadata(file)
        assertEquals("Lyrics from external editor", result.preferredPlainLyrics)
        assertEquals("Lyrics from external editor",
            selectLatestEmbeddedLyrics(result, 12_000).stored.content)
    }

}
