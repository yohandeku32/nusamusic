package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.R
import androidx.core.view.WindowCompat
import java.util.Locale

private fun introText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", true)) id else en

internal object ArunaIntroPreferences {
    private const val COMPLETE = "welcome_completed_v1"

    fun prepare(preferences: SharedPreferences, existingUse: Boolean): Boolean {
        if (preferences.contains(COMPLETE)) return !preferences.getBoolean(COMPLETE, false)
        val configured = existingUse || preferences.all.isNotEmpty()
        // Mark an unfinished first launch before startup caches write their
        // own preferences, so process recreation cannot look like an upgrade.
        preferences.edit().putBoolean(COMPLETE, configured).apply()
        return !configured
    }

    fun complete(preferences: SharedPreferences) {
        preferences.edit().putBoolean(COMPLETE, true).apply()
    }
}

internal enum class IntroMusicChoice { FOLDER, DEVICE, LATER }

@Composable
internal fun ArunaIntroHost(
    preferences: SharedPreferences,
    showInitially: Boolean,
    onChooseFolder: () -> Unit,
    onDeviceMusic: () -> Unit,
    content: @Composable (openLibrary: Boolean) -> Unit
) {
    var visible by rememberSaveable { mutableStateOf(showInitially) }
    var openLibrary by rememberSaveable { mutableStateOf(false) }
    if (visible) ArunaIntroScreen(onChoice = { choice ->
        if (visible) {
            ArunaIntroPreferences.complete(preferences)
            openLibrary = choice != IntroMusicChoice.LATER
            visible = false
            when (choice) {
                IntroMusicChoice.FOLDER -> onChooseFolder()
                IntroMusicChoice.DEVICE -> onDeviceMusic()
                IntroMusicChoice.LATER -> Unit
            }
        }
    }) else content(openLibrary)
}

@Composable
internal fun MusicImportActions(
    onChooseFolder: () -> Unit,
    onDeviceMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onChooseFolder,
            modifier = Modifier.fillMaxWidth().introFocusRing().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) {
            Text(introText("Pilih folder musik", "Choose a music folder"), fontSize = 16.sp)
        }
        OutlinedButton(onClick = onDeviceMusic,
            modifier = Modifier.fillMaxWidth().introFocusRing().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text(introText("Cari musik di perangkat", "Find music on this device"), fontSize = 16.sp)
        }
        Text(introText("Pencarian di perangkat memerlukan izin akses file audio.",
            "Searching your device needs audio file access."),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun Modifier.introFocusRing(): Modifier {
    var focused by remember { mutableStateOf(false) }
    return onFocusChanged { focused = it.hasFocus }
        .border(2.dp, if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            RoundedCornerShape(19.dp))
        .padding(3.dp)
}

@Composable
internal fun ArunaIntroScreen(
    onChoice: (IntroMusicChoice) -> Unit,
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previousLightNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (previousLightNavigation != null) controller?.isAppearanceLightNavigationBars = previousLightNavigation
        }
    }
    // Match the charcoal, warm ink and lime action of Aruna's default CD player.
    MaterialTheme(colorScheme = darkColorScheme(
        background = Color(0xFF111111), surface = Color(0xFF191919),
        primary = Color(0xFFB5DF51), onPrimary = Color(0xFF111111),
        onBackground = Color(0xFFF2F0E9), onSurface = Color(0xFFF2F0E9),
        onSurfaceVariant = Color(0xFFA5A5A0)
    )) {
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(contentInsets)) {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Aruna", color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { onChoice(IntroMusicChoice.LATER) },
                    modifier = Modifier.introFocusRing().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text(introText("Nanti saja", "Not now"))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val wide = maxWidth >= 640.dp && maxWidth > maxHeight * 1.25f
                val largeText = LocalDensity.current.fontScale >= 1.5f
                if (wide) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        WelcomeRecord(Modifier.weight(0.42f), compact = largeText)
                        Column(Modifier.weight(0.58f).fillMaxHeight().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            WelcomeCopy(compact = true)
                            MusicImportActions({ onChoice(IntroMusicChoice.FOLDER) },
                                { onChoice(IntroMusicChoice.DEVICE) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else {
                    val compact = maxHeight < 610.dp || largeText
                    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().align(Alignment.Center)
                        .verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                        .padding(horizontal = 28.dp, vertical = if (compact) 16.dp else 28.dp),
                        verticalArrangement = if (compact) Arrangement.spacedBy(24.dp) else Arrangement.SpaceBetween) {
                        WelcomeRecord(Modifier.fillMaxWidth(), compact = compact)
                        WelcomeCopy(compact = compact)
                        MusicImportActions({ onChoice(IntroMusicChoice.FOLDER) },
                            { onChoice(IntroMusicChoice.DEVICE) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCopy(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(introText("Selamat datang\ndi Aruna.", "Welcome\nto Aruna."),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 34.sp else 40.sp, lineHeight = if (compact) 40.sp else 47.sp,
            letterSpacing = (-1).sp)
        Text(introText("Koleksi musikmu, siap didengarkan. Mulai dari folder pilihanmu atau cari musik di perangkat.",
            "Your music, ready to play. Start with a folder you choose, or find music on your device."),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, lineHeight = 24.sp)
    }
}

@Composable
private fun WelcomeRecord(modifier: Modifier, compact: Boolean) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(420)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.nusamusic_app_icon), contentDescription = null,
            modifier = Modifier.size(if (compact) 112.dp else 184.dp).graphicsLayer {
                alpha = entrance.value
                rotationZ = -10f * (1f - entrance.value)
                scaleX = 0.94f + entrance.value * 0.06f
                scaleY = scaleX
            })
    }
}
