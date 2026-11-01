package com.yohandeku32.nusamusic.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.yohandeku32.nusamusic.R
import com.yohandeku32.nusamusic.data.AppleTallVideoCache

/**
 * Square Apple Music album motion, never a static zoom masquerading as video.
 * One silent, looping Media3 decoder is created only for the opened album.
 * The album's local art is rendered beneath this view until the FIRST frame;
 * a missing/broken HLS stream therefore cannot black out the hero section.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun AppleAlbumAnimatedHero(
    url: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var firstFrame by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }

    val player = remember(url) {
        val selector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(1080, 1080)
                .setMaxVideoFrameRate(30)
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
        }
        val builder = ExoPlayer.Builder(context).setTrackSelector(selector)
        AppleTallVideoCache.mediaSourceFactory(context)?.let {
            builder.setMediaSourceFactory(it)
        }
        builder.build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(
                MediaItem.Builder().setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                failed = true
                firstFrame = false
            }
        }
        player.addListener(listener)
        player.prepare()
        player.playWhenReady = active
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, active) {
        player.playWhenReady = active && !failed
    }
    val opacity by animateFloatAsState(
        targetValue = if (active && firstFrame && !failed) 1f else 0f,
        animationSpec = tween(200),
        label = "artistAlbumAnimatedArtworkFade"
    )

    AndroidView(
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.immersive_canvas_player_view, null, false) as PlayerView)
                .apply { this.player = player }
        },
        update = { view ->
            if (view.player !== player) view.player = player
        },
        modifier = modifier.fillMaxSize().graphicsLayer { alpha = opacity }
    )
}
