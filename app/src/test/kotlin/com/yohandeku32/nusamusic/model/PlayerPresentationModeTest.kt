package com.yohandeku32.nusamusic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPresentationModeTest {
    @Test
    fun missingOrUnknownPreferenceDefaultsToVinyl() {
        assertEquals(PlayerPresentationMode.VINYL, PlayerPresentationMode.fromPreference(null))
        assertEquals(PlayerPresentationMode.VINYL, PlayerPresentationMode.fromPreference("unknown"))
    }

    @Test
    fun immersiveArtworkPreferenceRestoresImmersiveMode() {
        assertEquals(
            PlayerPresentationMode.IMMERSIVE_ARTWORK,
            PlayerPresentationMode.fromPreference("immersive_artwork")
        )
    }
}
