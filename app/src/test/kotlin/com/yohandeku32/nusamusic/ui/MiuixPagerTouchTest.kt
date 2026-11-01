package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real coordinate taps through overlapping Compose layers, not semantic clicks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MiuixPagerTouchTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var pager: PagerState
    private lateinit var scope: CoroutineScope
    private val plays = IntArray(3)
    private var nextTaps = 0

    private fun showPager() {
        compose.setContent {
            pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
            scope = rememberCoroutineScope()
            MiuixHorizontalPager(
                pager, Color.Black, Modifier.fillMaxSize().testTag("pager"),
                pageSurface = { page ->
                    if (page == 2) Color(0xFFF8F9FB) else Color.Black
                }
            ) { page ->
                Box(Modifier.fillMaxSize()) {
                    // Neighbour pages have a full-page target, like Artist or
                    // Library rows. Now Playing contains separate transport
                    // controls and must NEVER route them to the Artist.
                    Box(
                        (if (page == 1) Modifier.align(Alignment.Center).size(64.dp)
                         else Modifier.fillMaxSize())
                            .background(Color.Gray)
                            .clickable { plays[page]++ }
                    )
                    if (page == 1) {
                        Box(Modifier.align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 96.dp)
                            .size(56.dp).testTag("player-next")
                            .background(Color.White)
                            .clickable { nextTaps++ })
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun emptyPlayerSpaceDoesNotPlayInvisibleLibraryRows() {
        showPager()
        compose.onNodeWithTag("pager").performTouchInput {
            click(Offset(width * 0.25f, height * 0.25f))
            click(Offset(width * 0.75f, height * 0.75f))
        }
        compose.runOnIdle { assertEquals(listOf(0, 0, 0), plays.toList()) }
        compose.onNodeWithTag("pager").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(listOf(0, 1, 0), plays.toList()) }
    }

    @Test fun nowPlayingNextButtonNeverClicksHiddenArtistOrLibrary() {
        showPager()
        compose.onNodeWithTag("player-next").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, nextTaps)
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
        // A canceled partial drag must restore the same active hit region.
        compose.onNodeWithTag("pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("player-next").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(2, nextTaps)
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
    }

    @Test fun overlappingLightLibraryCardStillReturnsToCorrectPlayerControls() {
        showPager()
        compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(2, pager.settledPage) }
        compose.onNodeWithTag("pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, pager.settledPage) }
        compose.onNodeWithTag("player-next").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, nextTaps)
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
    }

    @Test fun slowBackAndForthKeepsNowPlayingNextTargetAligned() {
        showPager()
        // Halfway swipes must never redirect a Now Playing button to the
        // Artist/Library content composed in the actual 36dp underlap.
        compose.onNodeWithTag("pager").performTouchInput {
            down(center)
            // Inject a genuinely slow reversible gesture, not two near-
            // instantaneous deltas that accidentally produce a fling.
            repeat(6) {
                moveBy(Offset(width * 0.018f, 0f))
                advanceEventTime(45)
            }
            repeat(6) {
                moveBy(Offset(-width * 0.018f, 0f))
                advanceEventTime(45)
            }
            advanceEventTime(160)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, pager.settledPage) }
        compose.onNodeWithTag("player-next").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, nextTaps)
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
    }

    @Test fun swipeDoesNotPlayAndDestinationReceivesItsOwnTaps() {
        showPager()
        compose.onNodeWithTag("pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, pager.settledPage)
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
        compose.onNodeWithTag("pager").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(listOf(1, 0, 0), plays.toList()) }
        compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, pager.settledPage)
            assertEquals(listOf(1, 1, 0), plays.toList())
        }
    }

    @Test fun tapDuringAnimatedNavigationDoesNotTriggerEitherPage() {
        showPager()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { scope.launch { pager.animateMiuixScrollToPage(0) } }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithTag("pager").performTouchInput { click(center) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle {
            // A touch may stop the pager's settle animation; it must never
            // become a click on either overlapping page.
            assertEquals(listOf(0, 0, 0), plays.toList())
        }
    }
}
