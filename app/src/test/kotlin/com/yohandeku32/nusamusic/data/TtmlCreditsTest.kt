package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Test

class TtmlCreditsTest {
    private fun ttml(metadata: String, word: String = "Halo") = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:a="http://music.apple.com/lyric-ttml-internal"
            xmlns:m="http://www.w3.org/ns/ttml#metadata">
            <head><metadata>$metadata</metadata></head>
            <body><p begin="1s" end="3s"><span begin="1s" end="3s">$word</span></p></body>
        </tt>
    """.trimIndent()

    private val authors = "<a:iTunesMetadata><a:songwriters><a:songwriter>Writer 世界</a:songwriter>" +
        "<a:songwriter>Second</a:songwriter></a:songwriters></a:iTunesMetadata>"

    private fun select(raw: String, credits: SongCredits = SongCredits()) =
        EmbeddedLyricsSelector.select(listOf(raw), 10_000, credits)

    @Test fun suppliedAppleMetadataShapeRetainsAllFiveSongwritersAndOriginalTiming() {
        val metadata = """<iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal"><songwriters>
            <songwriter>Jesse Finkelstein</songwriter><songwriter>Ruben Christopher den Boer</songwriter>
            <songwriter>Sarah &quot;Solly&quot; Solovay</songwriter><songwriter>Victor Pool</songwriter>
            <songwriter>缪维霖</songwriter></songwriters></iTunesMetadata>"""
        val result = LyricsDecoder.decodeWithTiming(ttml(metadata), 10_000)
        assertEquals(listOf("Jesse Finkelstein", "Ruben Christopher den Boer", "Sarah \"Solly\" Solovay",
            "Victor Pool", "缪维霖"), result.credits.songwriters)
        assertTrue(result.credits.composers.isEmpty())
        assertTrue(result.credits.lyricists.isEmpty())
        assertEquals(1000, result.lyrics.lines.single().start)
        assertEquals(3000, result.lyrics.lines.single().end)
    }

    @Test fun namespacePrefixesAndSourceMetadataContainersDoNotChangeTheCredit() {
        val result = select(ttml(authors.replace("iTunesMetadata", "sourceMetadata")))
        assertEquals(listOf("Writer 世界", "Second"), result.stored.credits.songwriters)
    }

    @Test fun explicitComposerAndLyricistRolesStaySeparateFromGeneralSongwriters() {
        val result = select(ttml(authors + """<a:composers><a:composer>Music</a:composer></a:composers>
            <a:lyricists><a:lyricist>Words</a:lyricist></a:lyricists>"""))
        assertEquals(listOf("Music"), result.stored.credits.composers)
        assertEquals(listOf("Words"), result.stored.credits.lyricists)
        assertEquals(listOf("Writer 世界", "Second"), result.stored.credits.songwriters)
    }

    @Test fun fileComposerAndLyricistTagsTakePrecedenceOverTtml() {
        val tags = SongCredits(listOf("Tagged composer"), listOf("Tagged lyricist"))
        val result = select(ttml(authors + "<a:composers><a:composer>Other</a:composer></a:composers>"), tags)
        assertEquals(tags.composers, result.stored.credits.composers)
        assertEquals(tags.lyricists, result.stored.credits.lyricists)
        assertEquals(listOf("Writer 世界", "Second"), result.stored.credits.songwriters)
    }

    @Test fun singersEditorsAndSungCreditLinesCannotBecomeFallbackAuthors() {
        val xml = ttml("""<m:agent type="person" xml:id="v1"><m:name>Singer</m:name></m:agent>
            <a:meta key="ttmlAuthor">File editor</a:meta><m:copyright>Publisher</m:copyright>""",
            "Written by: ordinary sung text")
        val result = select(xml)
        assertTrue(result.stored.credits.isEmpty)
        assertEquals("Written by: ordinary sung text", visibleMainLyrics(result))
        val bodyMetadata = ttml("").replace("<body>", "<body><metadata>$authors</metadata>")
        assertTrue(select(bodyMetadata).stored.credits.isEmpty)
    }

    @Test fun duplicatesWhitespaceAndPlaceholdersAreCleanedWithoutSplittingNames() {
        val xml = ttml("""<a:songwriters><a:songwriter> AC/DC </a:songwriter><a:songwriter>ac/dc</a:songwriter>
            <a:songwriter> Writer   世界 </a:songwriter><a:songwriter>Unknown composer</a:songwriter>
            <a:songwriter> </a:songwriter><a:songwriter>n/a</a:songwriter></a:songwriters>""")
        assertEquals(listOf("AC/DC", "Writer 世界"), select(xml).stored.credits.songwriters)
    }

    @Test fun nameCountAndLengthAreBounded() {
        val entries = "<a:songwriter>${"x".repeat(2049)}</a:songwriter>" +
            (1..40).joinToString("") { "<a:songwriter>Writer $it</a:songwriter>" }
        val names = select(ttml("<a:songwriters>$entries</a:songwriters>")).stored.credits.songwriters
        assertEquals((1..32).map { "Writer $it" }, names)
    }

    @Test fun anOldRawTtmlCacheCanRecoverCreditsWithoutARescan() {
        val raw = ttml(authors)
        val old = StoredLyrics(raw, "embedded")
        assertEquals(listOf("Writer 世界", "Second"), select(old.content, old.credits).stored.credits.songwriters)
    }

    @Test fun matchedOnlineTtmlCanFillCreditsWithoutReplacingEqualQualityLocalTiming() {
        val local = select(ttml(""))
        val remote = select(ttml(authors))
        val result = preferLyrics(local, remote)
        assertSame(local.synced, result.synced)
        assertEquals(local.stored.content, result.stored.content)
        assertEquals(remote.stored.credits, result.stored.credits)
    }

    @Test fun aTimingUpgradeRetainsLocalTagsAndTheRemoteAuthorFallback() {
        val local = select("Halo", SongCredits(listOf("Tagged composer")))
        val remote = select(ttml(authors))
        val result = preferLyrics(local, remote)
        assertSame(remote.synced, result.synced)
        assertEquals(listOf("Tagged composer"), result.stored.credits.composers)
        assertEquals(remote.stored.credits.songwriters, result.stored.credits.songwriters)
    }

    @Test fun differentSongTextCannotSupplyCredits() {
        val local = select(ttml(""))
        assertEquals(local, preferLyrics(local, select(ttml(authors, "Different song"))))
    }

    @Test fun onlyTheSelectedTtmlCanSupplyCreditsAndAnEditedPlainLyricDoesNotInheritStaleOnes() {
        val old = ttml(authors)
        val replacement = ttml("", "Updated")
        val selected = EmbeddedLyricsSelector.select(listOf(replacement, old), 10_000)
        assertTrue(selected.stored.credits.isEmpty)
        val edited = selectLatestEmbeddedLyrics(EmbeddedLyricsMetadata(listOf(old), preferredPlainLyrics = "User edit"), 10_000)
        assertTrue(edited.stored.credits.isEmpty)
        assertEquals("User edit", edited.stored.content)
    }

    @Test fun invalidTtmlAndFilesWithoutAuthorsKeepCreditsEmpty() {
        assertTrue(select("<tt><head><metadata>$authors</metadata></head>").stored.credits.isEmpty)
        assertTrue(select("[00:01]Halo").stored.credits.isEmpty)
        assertTrue(select(ttml("")).stored.credits.isEmpty)
    }
}
