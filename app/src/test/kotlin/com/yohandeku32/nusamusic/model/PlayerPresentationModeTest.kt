package com.yohandeku32.nusamusic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun onlyCdIsCurrentlyEnabled() {
        assertTrue(PlayerPresentationMode.CD_ONLY_MODE)
    }

    @Test
    fun oldVinylAndImmersivePreferencesLoadCdWhileDisabled() {
        assertEquals(
            PlayerPresentationMode.CD,
            PlayerPresentationMode.fromPreference(PlayerPresentationMode.VINYL.preferenceValue)
        )
        assertEquals(
            PlayerPresentationMode.CD,
            PlayerPresentationMode.fromPreference(PlayerPresentationMode.IMMERSIVE_ARTWORK.preferenceValue)
        )
    }

    @Test
    fun cdAndUnknownPreferencesAlsoLoadCd() {
        assertEquals(PlayerPresentationMode.CD, PlayerPresentationMode.fromPreference("cd"))
        assertEquals(PlayerPresentationMode.CD, PlayerPresentationMode.fromPreference(null))
        assertEquals(PlayerPresentationMode.CD, PlayerPresentationMode.fromPreference("unknown"))
    }

    @Test
    fun inactiveModesKeepTheirPreferenceIdentifiersForLater() {
        assertEquals("vinyl", PlayerPresentationMode.VINYL.preferenceValue)
        assertEquals("immersive_artwork", PlayerPresentationMode.IMMERSIVE_ARTWORK.preferenceValue)
    }
}
