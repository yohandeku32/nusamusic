package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsWriteVerificationTest {
    @Test fun stagedFlacM4aAndId3LyricsAcceptReaderEnvelopeNormalisation() {
        val provider = "\uFEFF  [00:01.00]Hello\r\n[00:04.00]World  \n"
        val reader = "[00:01.00]Hello\n[00:04.00]World"
        assertTrue(stagedLyricsMatch(provider, listOf("Other lyrics", reader)))
    }

    @Test fun syllableTimeCodesMustBePreservedExactly() {
        val expected = "[00:01]<00:01>Hi <00:02>there<00:05>"
        assertTrue(stagedLyricsMatch(expected + "\r\n", listOf(expected)))
        assertFalse(stagedLyricsMatch(expected,
            listOf("[00:01]<00:01>Hi <00:03>there<00:05>")))
    }

    @Test fun absentMetadataMustNeverPassOrCommit() {
        assertFalse(stagedLyricsMatch("[00:02]Song", emptyList()))
        assertFalse(stagedLyricsMatch("[00:02]Song", listOf("Plain song")))
        assertFalse(stagedLyricsMatch(" \r\n", listOf("")))
    }
}
