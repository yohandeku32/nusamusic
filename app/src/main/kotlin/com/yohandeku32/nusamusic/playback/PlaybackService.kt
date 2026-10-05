package com.yohandeku32.nusamusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.yohandeku32.nusamusic.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,
                15_000,
                2_000,
                3_000
            )
            .build()

        val player = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
        )
            .setLoadControl(loadControl)
            .build()

        // Explicitly bind notification taps to NusaMusic's own Activity.
        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }
}
