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
package org.avium.alivenotifscore.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.avium.alivenotifscore.MainActivity
import org.avium.alivenotifscore.R

class LiveNotificationManager(private val context: Context) {

    companion object {
        const val ACTION_SHOW_CHIP = "org.avium.systemui.chips.action.SHOW_CHIP"
        const val EXTRA_TYPE = "type"
        const val EXTRA_TEXT = "text"
        const val CHIP_TYPE_MUSIC = 1
        const val CHANNEL_ID_LIVE_UPDATES = "live_updates_channel"
        const val NOTIFICATION_ID_LIVE = 1001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_LIVE_UPDATES,
            "实况胶囊",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "显示歌词"
            setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendLyricBroadcast(lyric: String) {
        val intent = Intent(ACTION_SHOW_CHIP).apply {
            putExtra(EXTRA_TYPE, CHIP_TYPE_MUSIC)
            putExtra(EXTRA_TEXT, lyric)
        }
        context.sendBroadcast(intent)
    }

    fun createLyricNotification(
        title: String,
        artist: String,
        lyric: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_LIVE_UPDATES)
            .setSmallIcon(R.drawable.ic_notifs_music)
            .setContentTitle(title)
            .setContentText(artist)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(false)
            .addAction(createAction("关闭", "ACTION_CLOSE"))

        builder.setShortCriticalText(lyric)
        builder.setRequestPromotedOngoing(true)

        return builder.build()
    }

    private fun createAction(label: String, action: String): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", action)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }

    fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID_LIVE, notification)
    }

    fun cancelNotification() {
        val intent = Intent(ACTION_SHOW_CHIP).apply {
            putExtra(EXTRA_TYPE, CHIP_TYPE_MUSIC)
            putExtra(EXTRA_TEXT, "")
        }
        context.sendBroadcast(intent)
        notificationManager.cancel(NOTIFICATION_ID_LIVE)
    }
}
