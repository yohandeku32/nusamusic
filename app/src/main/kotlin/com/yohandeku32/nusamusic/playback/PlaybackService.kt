package com.yohandeku32.nusamusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.decent.usbaudio.media3.UsbAudioSink
import com.decent.usbaudio.media3.UsbAudioSinkConfig
import com.yohandeku32.nusamusic.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var usbAudioSink: UsbAudioSink? = null

    override fun onCreate() {
        super.onCreate()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink {
                // Keep Hi-Res PCM in float when the Media3 decoder hands us
                // 24/32-bit PCM. The Decent USB driver converts it to the DAC's
                // negotiated integer depth without going through AudioTrack.
                val delegate = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setEnableAudioOutputPlaybackParameters(false)
                    .build()

                return UsbAudioSink(
                    delegate = delegate,
                    context = context,
                    config = UsbAudioSinkConfig(
                        bitPerfectEnabled = true,
                        forceRouteToSpeaker = true
                    )
                ).also { usbAudioSink = it }
            }
        }.setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        )

        val loadControl = UsbAudioSink.wrapLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5_000,
                    15_000,
                    2_000,
                    3_000
                )
                .build()
        ) {
            usbAudioSink?.isNativeEngineActive == true
        }

        val player = ExoPlayer.Builder(
            this,
            renderersFactory
        )
            .setLoadControl(loadControl)
            .build()

        usbAudioSink?.attachToPlayer(player)

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
        usbAudioSink?.let {
            runCatching { it.detachFromPlayer() }
        }
        usbAudioSink = null

        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }
}
