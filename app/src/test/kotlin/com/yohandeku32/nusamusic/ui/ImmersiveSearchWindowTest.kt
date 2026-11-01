package com.yohandeku32.nusamusic.ui

import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImmersiveSearchWindowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val previousLocale = Locale.getDefault()
    private val open = mutableStateOf(true)
    private var dismissedQuery: String? = null
    private val songs = listOf(Song(1, "Track one", "Artist", "Album", "content://music/1", 180000, 1))

    @Before fun english() {
        Locale.setDefault(Locale.ENGLISH)
        compose.runOnUiThread {
            compose.activity.window.decorView.windowInsetsController!!.hide(WindowInsets.Type.statusBars())
        }
    }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                if (open.value) ImmersiveSearchDialog(songs, "", onDismiss = {
                    dismissedQuery = it
                    open.value = false
                }, onPlay = { _, _ -> })
            }
        }
        compose.waitForIdle()
    }

    private fun window(): Window = requireNotNull(ShadowDialog.getLatestDialog().window)

    private fun visibleTypes(window: Window): Int = ReflectionHelpers.callInstanceMethod(
        requireNotNull(window.decorView.windowInsetsController), "getRequestedVisibleTypes")

    private fun assertOnlyStatusHidden(window: Window) {
        val requested = visibleTypes(window)
        assertEquals(0, requested and WindowInsets.Type.statusBars())
        assertNotEquals(0, requested and WindowInsets.Type.navigationBars())
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Test fun searchHidesStatusBarWithoutHidingNavigationOrBreakingInput() {
        show()
        compose.runOnIdle { assertOnlyStatusHidden(window()) }
        compose.onNode(hasSetTextAction()).performTextInput("Track")
        compose.onNode(hasSetTextAction()).assertTextContains("Track")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertOnlyStatusHidden(window()) }
        compose.onNodeWithContentDescription("Close search").performClick()
        assertEquals("Track", dismissedQuery)
    }

    @Test fun focusReturnReassertsTheSearchWindowPolicy() {
        show()
        compose.runOnIdle {
            val window = window()
            val observer = window.decorView.viewTreeObserver
            ReflectionHelpers.callInstanceMethod<Any>(observer, "dispatchOnWindowFocusChange",
                ClassParameter.from(Boolean::class.javaPrimitiveType!!, false))
            window.decorView.windowInsetsController!!.show(WindowInsets.Type.statusBars())
            assertNotEquals(0, visibleTypes(window) and WindowInsets.Type.statusBars())
            ReflectionHelpers.callInstanceMethod<Any>(observer, "dispatchOnWindowFocusChange",
                ClassParameter.from(Boolean::class.javaPrimitiveType!!, true))
            assertOnlyStatusHidden(window)
        }
    }

    @Test fun reopeningConfiguresTheNewDialogAndLeavesTheActivityWindowAlone() {
        var activityTypes = 0
        compose.runOnUiThread { activityTypes = visibleTypes(compose.activity.window) }
        show()
        val first = window()
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.runOnIdle {
            assertEquals(activityTypes, visibleTypes(compose.activity.window))
            open.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNotSame(first, window())
            assertOnlyStatusHidden(window())
            assertEquals(activityTypes, visibleTypes(compose.activity.window))
        }
    }
}
