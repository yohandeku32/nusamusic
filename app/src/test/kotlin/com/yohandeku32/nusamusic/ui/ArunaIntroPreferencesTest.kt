package com.yohandeku32.nusamusic.ui

import android.content.Context
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArunaIntroPreferencesTest {
    private val preferences get() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("intro-policy-test", Context.MODE_PRIVATE)

    @Before fun clear() { preferences.edit().clear().commit() }

    @Test fun freshInstallShowsWelcome() {
        assertTrue(ArunaIntroPreferences.prepare(preferences, existingUse = false))
    }

    @Test fun unfinishedWelcomeSurvivesStartupCachesAndProcessReopening() {
        assertTrue(ArunaIntroPreferences.prepare(preferences, false))
        preferences.edit().putFloat("title_font_size", 34f).commit()
        assertTrue(ArunaIntroPreferences.prepare(preferences, existingUse = true))
    }

    @Test fun completionPersistsForLaterLaunches() {
        ArunaIntroPreferences.prepare(preferences, false)
        ArunaIntroPreferences.complete(preferences)
        assertFalse(ArunaIntroPreferences.prepare(preferences, existingUse = false))
    }

    @Test fun existingSettingsSkipWelcomeWithoutChangingTheirValues() {
        preferences.edit().putString("player_presentation_mode", "immersive_artwork").commit()
        assertFalse(ArunaIntroPreferences.prepare(preferences, existingUse = false))
        assertEquals("immersive_artwork", preferences.getString("player_presentation_mode", null))
        assertFalse(ArunaIntroPreferences.prepare(preferences, false))
    }

    @Test fun existingLibraryOrPlaybackHistoryMigratesWithoutWelcome() {
        assertFalse(ArunaIntroPreferences.prepare(preferences, existingUse = true))
        assertFalse(ArunaIntroPreferences.prepare(preferences, existingUse = false))
    }
}
