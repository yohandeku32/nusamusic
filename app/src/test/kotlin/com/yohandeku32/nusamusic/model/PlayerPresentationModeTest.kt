package com.yohandeku32.nusamusic.model

import org.junit.Assert.*
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun cdAndImmersiveAreAvailableWhileVinylRemainsDisabled() {
        assertTrue(PlayerPresentationMode.CD.isAvailable)
        assertTrue(PlayerPresentationMode.IMMERSIVE_ARTWORK.isAvailable)
        assertFalse(PlayerPresentationMode.VINYL.isAvailable)
    }

    @Test
    fun enabledModesRestoreTheirSavedSelection() {
        for (mode in listOf(PlayerPresentationMode.CD, PlayerPresentationMode.IMMERSIVE_ARTWORK)) {
            assertEquals(mode, PlayerPresentationMode.fromPreference(mode.preferenceValue))
        }
    }

    @Test
    fun disabledAndUnknownPreferencesFallBackToCd() {
        for (value in listOf(null, "unknown", "vinyl")) {
            assertEquals(PlayerPresentationMode.CD, PlayerPresentationMode.fromPreference(value))
        }
    }
}
