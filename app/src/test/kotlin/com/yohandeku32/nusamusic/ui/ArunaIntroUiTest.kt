package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class ArunaIntroUiTest {
    private val durationScale = object : MotionDurationScale {
        var factor = 1f
        override val scaleFactor get() = factor
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = durationScale)
    private val previousLocale = Locale.getDefault()
    private var folders = 0
    private var devices = 0
    private lateinit var inputModeManager: InputModeManager
    private val preferences get() = compose.activity.getSharedPreferences("intro-ui-test", Context.MODE_PRIVATE)

    @Before fun prepare() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        preferences.edit().clear().commit()
    }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun show(width: Int = 411, height: Int = 800, scale: Float = 1f, initially: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                inputModeManager = LocalInputModeManager.current
                Box(Modifier.requiredSize(width.dp, height.dp).testTag("intro-preview")) {
                    ArunaIntroHost(preferences, initially, { folders++ }, { devices++ }) { library ->
                        Column {
                            Text(if (library) "Library" else "Player")
                            if (library) MusicImportActions({ folders++ }, { devices++ })
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun frame(name: String): Bitmap {
        val bounds = compose.onNodeWithTag("intro-preview").getUnclippedBoundsInRoot()
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            val dir = File("build/reports/intro-preview").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }

    private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    @Test fun welcomeWaitsForUserAndFolderChoiceOpensLibraryWithRetry() {
        show()
        compose.onNodeWithText("Selamat datang\ndi Aruna.").assertIsDisplayed()
        compose.onNodeWithText("Library").assertDoesNotExist()
        compose.onNodeWithText("Player").assertDoesNotExist()
        assertEquals(0, folders + devices)
        compose.onNodeWithText("Pilih folder musik").performScrollTo().performClick()
        compose.onNodeWithText("Library").assertIsDisplayed()
        assertEquals(1, folders)
        assertEquals(0, devices)
        assertFalse(ArunaIntroPreferences.prepare(preferences, false))
        // No picker result simulates cancellation: the library retains a retry.
        compose.onNodeWithText("Pilih folder musik").performClick()
        assertEquals(2, folders)
        compose.onNodeWithText("Selamat datang\ndi Aruna.").assertDoesNotExist()
    }

    @Test fun deviceChoiceRequestsOnlyAfterClickAndDenialLeavesFolderRecovery() {
        show()
        assertEquals(0, devices)
        compose.onNodeWithText("Cari musik di perangkat").performScrollTo().performClick()
        assertEquals(1, devices)
        compose.onNodeWithText("Library").assertIsDisplayed()
        // No permission result is treated as no access; recovery remains usable.
        compose.onNodeWithText("Pilih folder musik").performClick()
        assertEquals(1, folders)
        assertEquals(1, devices)
    }

    @Test fun notNowEntersPlayerWithoutAnyAccessRequest() {
        show()
        compose.onNodeWithText("Nanti saja").performClick()
        compose.onNodeWithText("Player").assertIsDisplayed()
        assertEquals(0, folders + devices)
        assertFalse(ArunaIntroPreferences.prepare(preferences, false))
    }

    @Test fun anExistingInstallationGoesStraightToPlayer() {
        preferences.edit().putFloat("title_font_size", 34f).commit()
        show(initially = ArunaIntroPreferences.prepare(preferences, false))
        compose.onNodeWithText("Player").assertIsDisplayed()
        compose.onNodeWithText("Nanti saja").assertDoesNotExist()
        assertEquals(0, folders + devices)
    }

    @Test fun importDestinationSurvivesSavedStateRestorationWithoutRepeatingTheRequest() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ArunaIntroHost(preferences, true, { folders++ }, { devices++ }) { library ->
                Text(if (library) "Library" else "Player")
            }
        }
        compose.onNodeWithText("Pilih folder musik").performScrollTo().performClick()
        compose.onNodeWithText("Library").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Library").assertIsDisplayed()
        compose.onNodeWithText("Nanti saja").assertDoesNotExist()
        assertEquals(1, folders)
    }

    @Test fun portraitWelcomeUsesExistingBrandAndStopsItsEntranceMotion() {
        compose.mainClock.autoAdvance = false
        show()
        for (step in 0..25) {
            compose.mainClock.advanceTimeBy(20)
            compose.waitForIdle()
            frame("entry-%03d".format(step)).recycle()
        }
        val settled = pixels(frame("welcome-portrait"))
        compose.mainClock.advanceTimeBy(600)
        assertArrayEquals(settled, pixels(frame("welcome-settled")))
        compose.onNodeWithText("Pilih folder musik").assertIsDisplayed()
        compose.onNodeWithText("Cari musik di perangkat").assertIsDisplayed()
    }

    @Test fun narrowPhoneAtDoubleTextSizeKeepsEveryActionReachable() {
        show(width = 320, height = 568, scale = 2f)
        compose.onNodeWithText("Nanti saja").assertIsDisplayed()
        compose.onNodeWithText("Pilih folder musik").performScrollTo().assertIsDisplayed()
        frame("welcome-narrow-large-text").recycle()
        compose.onNodeWithText("Cari musik di perangkat").performScrollTo().assertIsDisplayed()
        val bounds = compose.onNodeWithText("Cari musik di perangkat").getUnclippedBoundsInRoot()
        val root = compose.onNodeWithTag("intro-preview").getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= root.left && bounds.right <= root.right)
        compose.onNodeWithText("Cari musik di perangkat").performClick()
        assertEquals(1, devices)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h480dp")
    fun landscapeReflowsAndKeepsImportActionsReachable() {
        show(width = 800, height = 400)
        compose.onNodeWithText("Pilih folder musik").performScrollTo().assertIsDisplayed()
        frame("welcome-landscape").recycle()
        compose.onNodeWithText("Cari musik di perangkat").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nanti saja").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w768dp-h1100dp")
    fun tabletKeepsTheWelcomeContentAtAReadableWidth() {
        show(width = 768, height = 1024)
        compose.onNodeWithText("Pilih folder musik").assertIsDisplayed()
        frame("welcome-tablet").recycle()
        val bounds = compose.onNodeWithText("Pilih folder musik").getUnclippedBoundsInRoot()
        assertTrue((bounds.right - bounds.left).value < 480f)
    }

    @Test fun englishLabelsAndKeyboardCanChooseTheFolder() {
        Locale.setDefault(Locale.ENGLISH)
        show()
        compose.onNodeWithText("Welcome\nto Aruna.").assertIsDisplayed()
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = compose.onNodeWithText("Choose a music folder")
        button.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused()
        frame("welcome-keyboard-focus").recycle()
        button.performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, folders)
        compose.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test fun reducedMotionKeepsActionsImmediatelyUsable() {
        durationScale.factor = 0f
        show()
        frame("welcome-reduced-motion").recycle()
        compose.onNodeWithText("Pilih folder musik").assertIsDisplayed()
        compose.onNodeWithText("Pilih folder musik").performClick()
        assertEquals(1, folders)
    }
}
