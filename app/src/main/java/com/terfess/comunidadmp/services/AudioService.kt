package com.terfess.comunidadmp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.terfess.comunidadmp.R

class AudioService : MediaSessionService() {
    private val customCommandClose = SessionCommand(ACTION_CLOSE, Bundle.EMPTY)
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer


    companion object {
        private const val CHANNEL_ID = "AUDIO_CHANNEL"
        private const val CHANNEL_NAME = "Canal de Audio"
        const val ACTION_CLOSE = "com.terfess.miradioyopal.ACTION_CLOSE"
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        //attributes for player, allow stop sound when this app lose the focus audio
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000,  // Min buffer size
                60000,  // Max buffer size
                1500,   // Buffer para iniciar reproducción
                5000    // Buffer después de rebuffering
            )
            .build()


        //initialize exoplayer3
        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()

        player.skipSilenceEnabled = false
        player.setHandleAudioBecomingNoisy(true)


        //set created attributes on player
        player.setAudioAttributes(audioAttributes, true)


        val closeButton =
            CommandButton.Builder()
                .setDisplayName("Close Service")
                .setIconResId(R.drawable.ic_power)
                .setSessionCommand(customCommandClose)
                .build()

        //initialize the media session
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MyCallback())
            .setCustomLayout(ImmutableList.of(closeButton))
            .build()
    }


    private inner class MyCallback : MediaSession.Callback {

        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Set available player and session commands.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(customCommandClose)
                        .build()
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {

                ACTION_CLOSE -> {
                    Log.i("AudioService", "onCustomCommand: close")
//                    MediaFragment.SharedData.valueClose.postValue(true)
                    onDestroy()
                }
            }
            return Futures.immediateFuture(null)
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                // Opcional: Configurar otras propiedades del canal, como descripción, sonido, etc.
                // description = "Descripción del canal de notificación"
                // setSound(soundUri, audioAttributes)
            }


            // Registrar el canal con el NotificationManager
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession!!.player
        if (player.playWhenReady) {
            // Make sure the service is not in foreground.
            player.prepare()
        } else {
            stopSelf()
        }

    }

    // allow others elements of the systeam entry at the session media
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession


    // release the player and media session in onDestroy
    override fun onDestroy() {
        super.onDestroy()
        player.release()
        mediaSession?.release()
        stopSelf() // stop the service
        println("terminado")
    }

}