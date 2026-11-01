package com.yohandeku32.nusamusic.ui

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerScalePreferenceTest {
    private val preferences get() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("player-scale-test", Context.MODE_PRIVATE)

    @Before fun clear() { preferences.edit().clear().commit() }

    @Test fun missingPreferenceKeepsOriginalSizeWithoutWriting() {
        assertEquals(1f, PlayerScalePreference.read(preferences))
        assertTrue(preferences.all.isEmpty())
    }

    @Test fun valuesAreBoundedAndSnapToFivePercentSteps() {
        assertEquals(1f, PlayerScalePreference.normalize(-1f))
        assertEquals(1.25f, PlayerScalePreference.normalize(8f))
        assertEquals(1.1f, PlayerScalePreference.normalize(1.11f))
        assertEquals(1f, PlayerScalePreference.normalize(Float.NaN))
        assertEquals(1f, PlayerScalePreference.normalize(Float.POSITIVE_INFINITY))
    }

    @Test fun selectionAndResetPersistAcrossReads() {
        PlayerScalePreference.write(preferences, 1.19f)
        assertEquals(1.2f, PlayerScalePreference.read(preferences))
        PlayerScalePreference.write(preferences, 1f)
        assertEquals(1f, PlayerScalePreference.read(preferences))
    }

    @Test fun incompatibleStoredValueFallsBackSafely() {
        preferences.edit().putString("immersive_player_scale", "invalid").commit()
        assertEquals(1f, PlayerScalePreference.read(preferences))
    }
}
