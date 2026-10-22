package com.yohandeku32.nusamusic.model

import org.junit.Assert.*
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun allThreePlayerPresentationsAreAvailable() {
        assertTrue(PlayerPresentationMode.CD.isAvailable)
        assertTrue(PlayerPresentationMode.IMMERSIVE_ARTWORK.isAvailable)
        assertTrue(PlayerPresentationMode.VINYL.isAvailable)
    }

    @Test
    fun enabledModesRestoreTheirSavedSelection() {
        for (mode in listOf(PlayerPresentationMode.VINYL, PlayerPresentationMode.CD, PlayerPresentationMode.IMMERSIVE_ARTWORK)) {
            assertEquals(mode, PlayerPresentationMode.fromPreference(mode.preferenceValue))
        }
    }

    @Test
    fun unknownPreferencesFallBackToCd() {
        for (value in listOf(null, "unknown")) {
            assertEquals(PlayerPresentationMode.CD, PlayerPresentationMode.fromPreference(value))
        }
    }
}
