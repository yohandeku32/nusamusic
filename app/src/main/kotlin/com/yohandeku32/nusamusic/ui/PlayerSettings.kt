package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
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
    animatedArtworkEnabled: Boolean,
    roundedCarouselCorners: Boolean,
    onRoundedCarouselCornersChange: (Boolean) -> Unit,
    onAnimatedArtworkChange: (Boolean) -> Unit,
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
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingIcon(Icons.Default.Tune)
            Column(Modifier.weight(1f)) {
                Text(
                    settingsText("Pengaturan", "Settings"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, settingsText("Tutup pengaturan", "Close settings"))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "presentation") {
                SettingsSection(settingsText("Tampilan pemutar", "Player appearance")) {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingChoice(
                            "CD", settingsText("Pemutar CD dengan carousel.", "CD player with carousel."),
                            Icons.Default.Album, playerPresentationMode == PlayerPresentationMode.CD,
                            { onPresentationSelected(PlayerPresentationMode.CD) }
                        )
                        SettingChoice(
                            "Vinyl",
                            if (!PlayerPresentationMode.VINYL.isAvailable) settingsText("Belum tersedia", "Unavailable")
                            else settingsText("Pemutar piringan hitam.", "Vinyl record player."),
                            Icons.Default.GraphicEq, playerPresentationMode == PlayerPresentationMode.VINYL,
                            { onPresentationSelected(PlayerPresentationMode.VINYL) },
                            enabled = PlayerPresentationMode.VINYL.isAvailable
                        )
                        SettingChoice(
                            settingsText("Sampul imersif", "Immersive artwork"),
                            settingsText("Sampul album layar penuh.", "Full-screen album cover."),
                            Icons.Default.CropOriginal, playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK,
                            { onPresentationSelected(PlayerPresentationMode.IMMERSIVE_ARTWORK) },
                            enabled = PlayerPresentationMode.IMMERSIVE_ARTWORK.isAvailable
                        )
                    }
                    SettingsDivider()
                    if (playerPresentationMode == PlayerPresentationMode.CD) {
                        SettingToggle(
                            settingsText("Sudut carousel", "Carousel corners"),
                            settingsText("Bulat atau kotak.", "Rounded or square."),
                            roundedCarouselCorners, onRoundedCarouselCornersChange
                        )
                        SettingsDivider()
                    }
                    SettingToggle(
                        settingsText("Animasi sampul", "Animated artwork"),
                        settingsText(
                            "Apple Music → Spotify Canvas. Judul dan artis dikirim saat pencarian.",
                            "Apple Music → Spotify Canvas. Searches share title and artist."
                        ),
                        animatedArtworkEnabled, onAnimatedArtworkChange
                    )
                }
            }
            if (LYRICS_ENABLED && playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) item(key = "lyrics") {
                SettingsSection(settingsText("Lirik", "Lyrics")) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                        Text(settingsText("Contoh lirik", "Lyric preview"),
                            Modifier.fillMaxWidth().padding(20.dp), fontSize = lyricsFontSize.sp,
                            lineHeight = (lyricsFontSize * 1.25f).sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingCaption(settingsText("PRATINJAU FONT", "FONT PREVIEW"))
                            Text(
                                "Nusa Music",
                                fontFamily = titleFontFamily,
                                fontSize = titleFontSize.sp,
                                lineHeight = (titleFontSize * 1.2f).sp
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
                    Text(settingsText("Gaya tombol", "Button style"), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingChoice(
                            settingsText("3D", "3D"),
                            settingsText("Efek tombol timbul.", "Raised button effect."),
                            Icons.Default.ViewInAr, realisticControls, { onRealisticControlsChange(true) }
                        )
                        SettingChoice(
                            "Flat", settingsText("Tanpa efek 3D.", "No 3D effect."),
                            Icons.Default.RadioButtonUnchecked, !realisticControls, { onRealisticControlsChange(false) }
                        )
                    }
                    SettingsDivider()
                    SettingToggle(
                        "Crossfade", settingsText("Peralihan antarlagu.", "Fade between tracks."),
                        crossfadeEnabled, onCrossfadeChange
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title, Modifier.padding(horizontal = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, enabled: Boolean = true) {
    Box(
        Modifier.size(44.dp).background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.06f else 0.03f),
            RoundedCornerShape(14.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.38f))
    }
}

@Composable
private fun SettingChoice(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primary.copy(alpha = 0.07f) else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = 0.45f) else colors.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingIcon(icon, enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.38f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))
            }
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(trailingIcon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingCaption(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingValue(text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RangeLabels(start: String, end: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(start, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(end, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}
