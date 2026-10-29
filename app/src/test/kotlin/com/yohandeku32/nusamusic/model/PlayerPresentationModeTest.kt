package com.yohandeku32.nusamusic.model

import org.junit.Assert.*
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun onlyCdAndImmersivePlayerPresentationsAreAvailable() {
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
    fun newInstallDefaultsToImmersive() {
        assertEquals(PlayerPresentationMode.IMMERSIVE_ARTWORK, PlayerPresentationMode.fromPreference(null))
    }

    @Test
    fun disabledVinylOrUnknownPreferencesFallBackToImmersive() {
        for (value in listOf("", "unknown", "vinyl", PlayerPresentationMode.VINYL.preferenceValue)) {
            assertEquals(PlayerPresentationMode.IMMERSIVE_ARTWORK, PlayerPresentationMode.fromPreference(value))
        }
    }
}
