package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.BuildConfig
import com.yohandeku32.nusamusic.LYRICS_ENABLED
import com.yohandeku32.nusamusic.TITLE_FONT_OPTIONS
import com.yohandeku32.nusamusic.model.PlayerPresentationMode
import java.util.Locale

private fun settingsText(id: String, en: String): String =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

/** Presentation only: the player owns preferences and the existing folder/font launchers. */
@Composable
internal fun PlayerSettingsContent(
    lyricsFontSize: Float,
    onLyricsFontSizeChange: (Float) -> Unit,
    onLyricsFontSizeChangeFinished: () -> Unit,
    hasLyricsSong: Boolean,
    lyricsLoading: Boolean,
    onRetryLyrics: () -> Unit,
    titleFontSize: Float,
    onTitleFontSizeChange: (Float) -> Unit,
    onTitleFontSizeChangeFinished: () -> Unit,
    titleFontName: String,
    titleFontFamily: FontFamily,
    customTitleFontName: String?,
    hasCustomTitleFont: Boolean,
    onTitleFontSelected: (String) -> Unit,
    onChooseCustomTitleFont: () -> Unit,
    onResetCustomTitleFont: () -> Unit,
    playerPresentationMode: PlayerPresentationMode,
    onPresentationSelected: (PlayerPresentationMode) -> Unit,
    libraryLightMode: Boolean,
    onLibraryLightModeChange: (Boolean) -> Unit,
    liquidGlassEnabled: Boolean,
    onLiquidGlassChange: (Boolean) -> Unit,
    animatedArtworkEnabled: Boolean,
    roundedCarouselCorners: Boolean,
    onRoundedCarouselCornersChange: (Boolean) -> Unit,
    onAnimatedArtworkChange: (Boolean) -> Unit,
    artworkSourceOrder: String,
    onArtworkSourceOrderChange: (String) -> Unit,
    realisticControls: Boolean,
    onRealisticControlsChange: (Boolean) -> Unit,
    crossfadeEnabled: Boolean,
    onCrossfadeChange: (Boolean) -> Unit,
    crossfadeDurationSeconds: Float,
    onCrossfadeDurationChange: (Float) -> Unit,
    onCrossfadeDurationChangeFinished: () -> Unit,
    musicFolders: List<Pair<String, String>>,
    isScanningMusic: Boolean,
    onSelectMusicFolder: () -> Unit,
    onRemoveMusicFolder: (String) -> Unit,
    onScanMusic: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val swipeThresholdPx = with(LocalDensity.current) { 76.dp.toPx() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    var swipeDismissTriggered by remember { mutableStateOf(false) }

    fun dismissFromSwipe() {
        if (!swipeDismissTriggered) {
            swipeDismissTriggered = true
            latestDismiss()
        }
    }

    // Only the header/handle and an overscroll pull at the absolute top can
    // dismiss the sheet. The outer ModalBottomSheet remains non-draggable,
    // preventing the sheet/LazyColumn gesture fight that caused the old jitter.
    val headerSwipe = Modifier.pointerInput(swipeThresholdPx) {
        var dragDistance = 0f
        detectVerticalDragGestures(
            onDragStart = { dragDistance = 0f },
            onVerticalDrag = { change, amount ->
                dragDistance = (dragDistance + amount).coerceAtLeast(0f)
                change.consume()
            },
            onDragEnd = {
                if (dragDistance >= swipeThresholdPx) dismissFromSwipe()
                dragDistance = 0f
            },
            onDragCancel = { dragDistance = 0f }
        )
    }
    val scrollPull = remember(listState, swipeThresholdPx) {
        object : NestedScrollConnection {
            private var pull = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) pull = 0f
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    available.y > 0f
                ) {
                    pull += available.y
                    if (pull >= swipeThresholdPx) {
                        pull = 0f
                        dismissFromSwipe()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pull = 0f
                return Velocity.Zero
            }
        }
    }

    // Keep geometry fixed while the list scrolls; add breathing room at the
    // top and a small drag indicator so swipe-to-dismiss is discoverable.
    Column(modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp).then(headerSwipe),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.width(36.dp).height(4.dp).background(
                    colors.onSurfaceVariant.copy(alpha = 0.32f),
                    CircleShape
                )
            )
        }
        Row(
            Modifier.fillMaxWidth()
                .then(headerSwipe)
                .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onDismiss,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = colors.surfaceVariant.copy(alpha = 0.68f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        settingsText("Kembali", "Back"),
                        Modifier.size(21.dp)
                    )
                }
            }
            Text(
                settingsText("Pengaturan", "Settings"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
        }
        HorizontalDivider(
            Modifier.padding(horizontal = 20.dp),
            color = colors.onSurface.copy(alpha = 0.07f)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).nestedScroll(scrollPull),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "presentation") {
                SettingsSection(settingsText("Tampilan pemutar", "Player appearance")) {
                    Row(
                        Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingModeTile(
                            label = "CD",
                            icon = Icons.Default.Album,
                            selected = playerPresentationMode == PlayerPresentationMode.CD,
                            enabled = PlayerPresentationMode.CD.isAvailable,
                            onClick = { onPresentationSelected(PlayerPresentationMode.CD) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingModeTile(
                            label = "Vinyl",
                            icon = Icons.Default.GraphicEq,
                            selected = playerPresentationMode == PlayerPresentationMode.VINYL,
                            enabled = PlayerPresentationMode.VINYL.isAvailable,
                            onClick = { onPresentationSelected(PlayerPresentationMode.VINYL) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingModeTile(
                            label = settingsText("Imersif", "Immersive"),
                            icon = Icons.Default.CropOriginal,
                            selected = playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK,
                            enabled = PlayerPresentationMode.IMMERSIVE_ARTWORK.isAvailable,
                            onClick = { onPresentationSelected(PlayerPresentationMode.IMMERSIVE_ARTWORK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SettingsDivider()
                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                        SettingToggle(
                            title = settingsText("Liquid Glass", "Liquid Glass"),
                            subtitle = settingsText(
                                "Kaca dinamis pada navigasi. Nonaktifkan untuk tampilan lebih ringan.",
                                "Dynamic glass navigation. Turn off for smoother, lighter rendering."
                            ),
                            checked = liquidGlassEnabled,
                            onCheckedChange = onLiquidGlassChange,
                            icon = Icons.Default.WaterDrop
                        )
                        SettingsDivider()
                        SettingToggle(
                            settingsText("Daftar lagu terang", "Light song list"),
                            settingsText(
                                "Hanya daftar lagu dan pencarian. Tampilan pemutar tetap gelap.",
                                "Song list and search only. Now Playing stays dark."
                            ),
                            libraryLightMode,
                            onLibraryLightModeChange,
                            icon = Icons.Default.LightMode
                        )
                        SettingsDivider()
                    }
                    if (playerPresentationMode == PlayerPresentationMode.CD) {
                        SettingToggle(
                            settingsText("Sudut carousel", "Carousel corners"),
                            settingsText("Bulat atau kotak.", "Rounded or square."),
                            roundedCarouselCorners, onRoundedCarouselCornersChange,
                            icon = Icons.Default.ViewCarousel
                        )
                        SettingsDivider()
                    }
                    SettingToggle(
                        settingsText("Animasi sampul", "Animated artwork"),
                        settingsText(
                            "Mengikuti prioritas sumber animasi di bawah.",
                            "Uses your chosen animation source priority below."
                        ),
                        animatedArtworkEnabled, onAnimatedArtworkChange,
                        icon = Icons.Default.AutoAwesome
                    )
                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK &&
                        animatedArtworkEnabled
                    ) {
                        SettingsDivider()
                        var sourceMenuOpen by remember { mutableStateOf(false) }
                        val sourceOptions = listOf(
                            "APPLE_FIRST" to settingsText("Apple Music dahulu", "Apple Music first"),
                            "SPOTIFY_FIRST" to settingsText("Spotify Canvas dahulu", "Spotify Canvas first"),
                            "APPLE_ONLY" to settingsText("Apple Music saja", "Apple Music only"),
                            "SPOTIFY_ONLY" to settingsText("Spotify Canvas saja", "Spotify Canvas only")
                        )
                        Box {
                            SettingAction(
                                title = settingsText("Sumber animasi", "Animation source"),
                                subtitle = sourceOptions.firstOrNull { it.first == artworkSourceOrder }?.second
                                    ?: sourceOptions.first().second,
                                icon = Icons.Default.AutoAwesome,
                                onClick = { sourceMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = sourceMenuOpen,
                                onDismissRequest = { sourceMenuOpen = false }
                            ) {
                                sourceOptions.forEach { (value, title) ->
                                    DropdownMenuItem(
                                        text = { Text(title) },
                                        onClick = {
                                            sourceMenuOpen = false
                                            onArtworkSourceOrderChange(value)
                                        },
                                        trailingIcon = {
                                            if (artworkSourceOrder == value)
                                                Icon(Icons.Default.Check, null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (LYRICS_ENABLED && playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) item(key = "lyrics") {
                SettingsSection(settingsText("Lirik", "Lyrics")) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            settingsText("Contoh lirik", "Lyric preview"),
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                            fontSize = lyricsFontSize.sp,
                            lineHeight = (lyricsFontSize * 1.25f).sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(settingsText("Ukuran lirik", "Lyrics size"), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        SettingValue("${lyricsFontSize.toInt()} sp")
                    }
                    Slider(value = lyricsFontSize, onValueChange = onLyricsFontSizeChange,
                        onValueChangeFinished = onLyricsFontSizeChangeFinished, valueRange = 18f..48f, steps = 29,
                        modifier = Modifier.semantics { contentDescription = settingsText("Ukuran lirik", "Lyrics size") })
                    RangeLabels("18 sp", "48 sp")
                    if (hasLyricsSong) {
                        SettingsDivider()
                        TextButton(onClick = onRetryLyrics, enabled = !lyricsLoading) {
                            Text(settingsText("Muat ulang lirik", "Reload lyrics"))
                        }
                    }
                    Text(settingsText("Lirik dari metadata audio.", "Lyrics from audio metadata."), color = colors.onSurfaceVariant)
                }
            }
            item(key = "typography") {
                SettingsSection(settingsText("Judul lagu", "Song title")) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.primary.copy(alpha = 0.08f)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            SettingCaption(settingsText("PRATINJAU", "PREVIEW"))
                            Text(
                                "Nusa Music",
                                fontFamily = titleFontFamily,
                                fontSize = titleFontSize.sp,
                                lineHeight = (titleFontSize * 1.2f).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(settingsText("Ukuran teks", "Text size"), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        SettingValue("${titleFontSize.toInt()} sp")
                    }
                    Slider(
                        value = titleFontSize,
                        onValueChange = onTitleFontSizeChange,
                        onValueChangeFinished = onTitleFontSizeChangeFinished,
                        valueRange = 24f..44f,
                        steps = 19,
                        modifier = Modifier.semantics {
                            contentDescription = settingsText("Ukuran judul lagu", "Song title size")
                        }
                    )
                    RangeLabels("24 sp", "44 sp")
                    SettingsDivider()
                    var fontMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        SettingAction(
                            settingsText("Jenis font", "Font family"),
                            customTitleFontName ?: titleFontName,
                            Icons.Default.TextFields,
                            { fontMenuExpanded = true },
                            trailingIcon = Icons.Default.KeyboardArrowDown
                        )
                        DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                            TITLE_FONT_OPTIONS.forEach { fontName ->
                                val selected = !hasCustomTitleFont && fontName == titleFontName
                                DropdownMenuItem(
                                    text = { Text(fontName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = { if (selected) Icon(Icons.Default.Check, settingsText("Dipilih", "Selected")) },
                                    onClick = {
                                        onTitleFontSelected(fontName)
                                        fontMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    SettingAction(
                        settingsText("Impor font", "Import font"),
                        settingsText("File TTF atau OTF", "TTF or OTF file"),
                        Icons.Default.FolderOpen, onChooseCustomTitleFont
                    )
                    if (hasCustomTitleFont) {
                        SettingAction(
                            settingsText("Reset font", "Reset font"),
                            customTitleFontName ?: settingsText("Font kustom", "Custom font"),
                            Icons.Default.RestartAlt, onResetCustomTitleFont
                        )
                    }
                }
            }
            item(key = "playback") {
                SettingsSection(settingsText("Pemutaran", "Playback")) {
                    Text(
                        settingsText("Gaya tombol", "Button style"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(colors.onSurface.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingSegment(
                            label = "3D",
                            selected = realisticControls,
                            modifier = Modifier.weight(1f),
                            onClick = { onRealisticControlsChange(true) }
                        )
                        SettingSegment(
                            label = "Flat",
                            selected = !realisticControls,
                            modifier = Modifier.weight(1f),
                            onClick = { onRealisticControlsChange(false) }
                        )
                    }
                    SettingsDivider()
                    SettingToggle(
                        "Crossfade", settingsText("Peralihan antarlagu.", "Fade between tracks."),
                        crossfadeEnabled, onCrossfadeChange,
                        icon = Icons.Default.GraphicEq
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            settingsText("Durasi transisi", "Transition duration"),
                            Modifier.weight(1f), color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingValue(settingsText("${crossfadeDurationSeconds.toInt()} detik", "${crossfadeDurationSeconds.toInt()} s"))
                    }
                    Slider(
                        value = crossfadeDurationSeconds,
                        onValueChange = onCrossfadeDurationChange,
                        onValueChangeFinished = onCrossfadeDurationChangeFinished,
                        valueRange = 1f..12f,
                        steps = 10,
                        enabled = crossfadeEnabled,
                        modifier = Modifier.semantics {
                            contentDescription = settingsText("Durasi crossfade", "Crossfade duration")
                        }
                    )
                    RangeLabels(settingsText("1 detik", "1 s"), settingsText("12 detik", "12 s"))
                }
            }
            item(key = "library") {
                SettingsSection(settingsText("Koleksi musik", "Music library")) {
                    SettingAction(
                        settingsText("Tambah folder musik", "Add music folder"),
                        settingsText("Ambil musik dari folder perangkat.", "Add music from your device."),
                        Icons.Default.CreateNewFolder, onSelectMusicFolder
                    )
                    if (musicFolders.isNotEmpty()) {
                        SettingsDivider()
                        SettingCaption(settingsText("FOLDER TERHUBUNG", "CONNECTED FOLDERS"))
                        Spacer(Modifier.height(8.dp))
                        musicFolders.forEach { (uri, name) ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { onRemoveMusicFolder(uri) }) {
                                    Icon(Icons.Default.Close, settingsText("Hapus folder $name", "Remove folder $name"), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onScanMusic,
                        enabled = !isScanningMusic,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (isScanningMusic) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        } else {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (isScanningMusic) settingsText("Memindai musik…", "Scanning music…") else settingsText("Pindai ulang musik", "Rescan music"))
                    }
                }
            }
            item(key = "about") {
                SettingsSection(settingsText("Tentang Nusa", "About Nusa")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingIcon(Icons.Default.MusicNote)
                        Column(Modifier.weight(1f)) {
                            Text("Nusa Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(settingsText("Versi ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        settingsText("Dibuat oleh yohandeku32", "By yohandeku32"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceVariant.copy(alpha = 0.34f),
            border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.055f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

/** Lightweight, visual player mode selector; no second nested scroll area. */
@Composable
private fun SettingModeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primary.copy(alpha = 0.13f)
            else colors.surface.copy(alpha = if (enabled) 0.75f else 0.35f),
        border = BorderStroke(
            1.dp,
            if (selected) colors.primary.copy(alpha = 0.55f)
            else colors.onSurface.copy(alpha = 0.07f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .height(120.dp)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, null, Modifier.size(29.dp),
                    tint = (if (selected) colors.primary else colors.onSurface)
                        .copy(alpha = if (enabled) 0.95f else 0.30f)
                )
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = settingsText("Dipilih", "Selected"),
                        modifier = Modifier.size(16.dp),
                        tint = colors.primary
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!enabled) {
                    Text(
                        settingsText("Belum tersedia", "Unavailable"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) colors.surface else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).background(
            colors.primary.copy(alpha = if (enabled) 0.10f else 0.035f),
            RoundedCornerShape(13.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null, Modifier.size(21.dp),
            tint = colors.primary.copy(alpha = if (enabled) 0.95f else 0.32f)
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) SettingIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 3
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingIcon(icon)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                trailingIcon, null, Modifier.size(20.dp),
                tint = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingValue(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RangeLabels(start: String, end: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(start, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(end, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.075f)
    )
}
