package com.yohandeku32.nusamusic.model

import org.junit.Assert.*
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun vinylCdAndImmersivePlayerPresentationsAreAvailable() {
        assertTrue(PlayerPresentationMode.CD.isAvailable)
        assertTrue(PlayerPresentationMode.IMMERSIVE_ARTWORK.isAvailable)
        assertTrue(PlayerPresentationMode.VINYL.isAvailable)
    }

    @Test
    fun enabledModesRestoreTheirSavedSelection() {
        for (mode in PlayerPresentationMode.entries) {
            assertEquals(mode, PlayerPresentationMode.fromPreference(mode.preferenceValue))
        }
    }

    @Test
    fun newInstallDefaultsToImmersive() {
        assertEquals(PlayerPresentationMode.IMMERSIVE_ARTWORK, PlayerPresentationMode.fromPreference(null))
    }

    @Test
    fun unknownPreferencesFallBackToImmersive() {
        for (value in listOf("", "unknown")) {
            assertEquals(PlayerPresentationMode.IMMERSIVE_ARTWORK, PlayerPresentationMode.fromPreference(value))
        }
    }
}
