/*
     Copyright (C) 2026 The AviumUI Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package org.avium.alivenotifscore.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.avium.alivenotifscore.manager.LiveNotificationManager

class LyricNotificationService : Service() {

    companion object {
        private const val STATUS_BAR_LYRIC_KEY = "status_bar_show_lyric"

        fun isLyricEnabled(context: Context): Boolean {
            return Settings.Secure.getInt(context.contentResolver, STATUS_BAR_LYRIC_KEY, 0) == 1
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationManager: LiveNotificationManager
    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            handlePlaybackState(state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = LiveNotificationManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val emptyNotification = createEmptyNotification()
        startForeground(1001, emptyNotification)
        startMonitoring()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "lyric_service_channel",
            "歌词服务",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createEmptyNotification(): Notification {
        return Notification.Builder(this, "lyric_service_channel")
            .setContentTitle("歌词服务运行中")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    private fun startMonitoring() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, this.javaClass)

        try {
            val controllers = manager.getActiveSessions(component)
            if (controllers.isNotEmpty()) {
                attachToController(controllers[0])
            }

            manager.addOnActiveSessionsChangedListener(
                { controllers ->
                    activeController?.unregisterCallback(callback)
                    activeController = null
                    notificationManager.cancelNotification()
                    
                    if (controllers?.isNotEmpty() == true) {
                        attachToController(controllers[0])
                    }
                },
                component
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun attachToController(controller: MediaController) {
        activeController = controller
        controller.registerCallback(callback)
        handlePlaybackState(controller.playbackState)
    }

    private fun handlePlaybackState(state: PlaybackState?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        
        if (!isPlaying) {
            notificationManager.cancelNotification()
        } else {
            activeController?.metadata?.let { updateNotification(it) }
        }
    }

    private fun updateNotification(metadata: MediaMetadata) {
        val isPlaying = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING
        if (!isPlaying) {
            notificationManager.cancelNotification()
            return
        }

        if (!isLyricEnabled(this)) {
            return
        }

        val lyric = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        notificationManager.sendLyricBroadcast(lyric)
    }

    override fun onDestroy() {
        super.onDestroy()
        activeController?.unregisterCallback(callback)
        serviceScope.cancel()
        notificationManager.cancelNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
