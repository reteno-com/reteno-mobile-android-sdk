package com.reteno.push

import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.RetenoInternalImpl
import com.reteno.push.channel.RetenoNotificationChannel

object RetenoNotifications {

    @JvmStatic
    fun updateDefaultNotificationChannel(name: String? = null, description: String? = null) {
        if (name == null && description == null) return
        RetenoInternalImpl.instance.executeAfterInit {
            val channel = NotificationChannelCompat.Builder(
                RetenoNotificationChannel.DEFAULT_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                name?.let { setName(it) }
                description?.let { setDescription(it) }
            }.build()

            NotificationManagerCompat.from(RetenoInternalImpl.instance.application)
                .createNotificationChannel(channel)
        }
    }
}