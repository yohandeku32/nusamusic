package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h914dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class VinylSongListTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val titles = listOf("Langit Senja", "Pulang", "Pelan-Pelan", "Di Antara Kita", "Kota Setelah Hujan")
    private val artists = listOf("Ruang Nada", "Arah Utara", "Sore Hari", "Ruang Nada", "Arah Utara")
    private val songs = mutableStateOf((1L..30L).map {
        Song(it, if (it <= 5) titles[it.toInt() - 1] else "Record $it",
            if (it <= 5) artists[it.toInt() - 1] else "Fixture artist $it", "Fixture", "content://fixture/$it", 4000, it)
    })
    private val current = mutableStateOf<Long?>(1L)
    private val query = mutableStateOf("")
    private var plays = 0
    private var toggles = 0
    private var opened = 0
    private var delete: Long? = null
    private lateinit var grid: LazyGridState
    private lateinit var inputMode: InputModeManager

    @Composable private fun cover(song: Song, modifier: Modifier) {
        val palettes = listOf(listOf(Color(0xFFC97440), Color(0xFF263B4A)),
            listOf(Color(0xFFD1AF79), Color(0xFF9D582F)), listOf(Color(0xFF627F68), Color(0xFF1C3C36)),
            listOf(Color(0xFF355273), Color(0xFF111C30)), listOf(Color(0xFF8B9FA5), Color(0xFF273239)))
        val palette = palettes[((song.id - 1) % 5).toInt()]
        Canvas(modifier) {
            drawRect(Brush.verticalGradient(palette))
            drawCircle(Color(0xFFF2D49B).copy(alpha = 0.85f), size.width * 0.16f,
                Offset(size.width * 0.59f, size.height * 0.38f))
            for (line in 0..18) {
                val y = size.height * (0.62f + line * 0.021f)
                drawLine(Color(0xFF142E39).copy(alpha = 0.28f), Offset(0f, y),
                    Offset(size.width, y - size.height * 0.035f), size.height * 0.012f)
            }
        }
    }

    private fun show(width: Int = 411, height: Int = 800, font: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                MaterialTheme(colorScheme = lightColorScheme(background = VinylPaper, surface = VinylPaper,
                    onBackground = Color(0xFF171717), onSurface = Color(0xFF171717), onSurfaceVariant = Color(0xFF565852))) {
                    inputMode = LocalInputModeManager.current
                    grid = rememberLazyGridState()
                    LaunchedEffect(query.value) { grid.scrollToItem(0) }
                    Column(Modifier.requiredSize(width.dp, height.dp).background(VinylPaper)) {
                        Text("Koleksi Vinyl", Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                            fontFamily = FontFamily.Serif, style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground)
                        VinylSongList(filterVinylCatalog(songs.value, query.value), grid, current.value, true, VinylPaper,
                            "Play", "Pause", "Unknown artist", { plays++; current.value = it.id },
                            { delete = it.id }, { toggles++ }, artwork = { song, modifier -> cover(song, modifier) },
                            modifier = Modifier.weight(1f), query = query.value, onQueryChange = { query.value = it })
                        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = Color(0xFFE9E7DF)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                val active = songs.value.firstOrNull { it.id == current.value } ?: songs.value.first()
                                VinylCatalogTrackInfo(active.title, active.artist, "Open player", { opened++ },
                                    artwork = { cover(active, it) }, modifier = Modifier.weight(1f))
                                IconButton(onClick = { toggles++ }) { Icon(Icons.Rounded.Pause, "Mini pause") }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun snapshot(name: String, width: Int = 411, height: Int = 800) {
        compose.runOnIdle {
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            compose.activity.findViewById<View>(android.R.id.content).draw(Canvas(image))
            val directory = File("build/reports/vinyl-preview").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
    }

    @Test fun twoColumnCatalogKeepsCoversAndMetadataSeparateAndUsesTheRightSong() {
        show()
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("vinyl-sleeve-2").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithTag("vinyl-sleeve-3").getUnclippedBoundsInRoot()
        assertEquals(first.top, second.top)
        assertTrue(first.right < second.left)
        assertTrue(third.top >= maxOf(first.bottom, second.bottom))
        snapshot("vinyl-catalog-phone")
        compose.onNodeWithTag("vinyl-sleeve-2").performClick()
        assertEquals(2L, current.value)
        assertEquals(1, plays)
        compose.onNodeWithTag("vinyl-sleeve-2").performClick()
        assertEquals(1, toggles)
    }

    @Test fun menuAndLongPressRequestDeletionWithoutStartingPlayback() {
        show()
        compose.onNodeWithTag("vinyl-song-menu-2").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(2L, delete)
        assertEquals(0, plays)
        assertEquals(0, toggles)
        compose.onNodeWithTag("vinyl-sleeve-3").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        assertEquals(3L, delete)
    }

    @Test fun browsingAndReorderingPreserveSelectionAndLastSongIsReachable() {
        show()
        compose.onNodeWithTag("vinyl-catalog-grid").performScrollToIndex(29)
        compose.onNodeWithTag("vinyl-sleeve-30").assertIsDisplayed()
        assertEquals(0, plays)
        compose.onNodeWithTag("vinyl-sleeve-30").performClick()
        compose.runOnIdle { songs.value = songs.value.filter { it.id % 2L == 0L }.reversed() }
        compose.onNodeWithTag("vinyl-catalog-grid").performScrollToIndex(0)
        compose.onNodeWithTag("vinyl-sleeve-30").assertIsSelected()
        assertEquals(30L, current.value)
        assertEquals(1, plays)
    }

    @Test fun titleAndArtistSearchCanBeClearedAndEmptyResultsNeverPlay() {
        show()
        compose.onNodeWithTag("vinyl-catalog-search").performTextInput("  arah UTARA  ")
        compose.onNodeWithTag("vinyl-catalog-search").performImeAction()
        assertEquals(listOf(2L, 5L), filterVinylCatalog(songs.value, query.value).map { it.id })
        compose.onNodeWithTag("vinyl-sleeve-2").assertIsDisplayed()
        compose.onNodeWithTag("vinyl-sleeve-1").assertDoesNotExist()
        compose.onNodeWithTag("vinyl-catalog-search").performTextReplacement("not-in-library")
        compose.onNodeWithText("No matching songs").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithTag("vinyl-sleeve-1").assertIsDisplayed()
        assertEquals(0, plays)
    }

    @Test fun largeTextUsesOneColumnAndRetainsKeyboardOperation() {
        show(width = 320, height = 720, font = 2f)
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("vinyl-sleeve-2").getUnclippedBoundsInRoot()
        assertEquals(first.left, second.left)
        assertTrue(first.bottom <= second.top)
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("vinyl-sleeve-2").performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("vinyl-sleeve-2").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("vinyl-sleeve-2").assertIsFocused()
        assertEquals(1, plays)
        snapshot("vinyl-catalog-large-text", 320, 720)
    }

    @Test fun tabletUsesFourNonOverlappingColumns() {
        show(width = 800)
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val fourth = compose.onNodeWithTag("vinyl-sleeve-4").getUnclippedBoundsInRoot()
        val fifth = compose.onNodeWithTag("vinyl-sleeve-5").getUnclippedBoundsInRoot()
        assertEquals(first.top, fourth.top)
        assertTrue(first.right < fourth.left)
        assertTrue(fifth.top >= first.bottom)
        snapshot("vinyl-catalog-tablet", 800, 800)
    }

    @Test fun currentTrackInfoOpensPlayerAndPauseUsesTheExistingCallback() {
        show()
        compose.onNodeWithContentDescription("Open player: Langit Senja, Ruang Nada").performClick()
        assertEquals(1, opened)
        assertEquals(0, plays)
        compose.onNodeWithContentDescription("Mini pause").performClick()
        assertEquals(1, toggles)
    }
}
