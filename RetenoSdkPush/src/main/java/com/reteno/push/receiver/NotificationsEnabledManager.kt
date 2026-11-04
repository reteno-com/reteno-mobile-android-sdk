package com.reteno.push.receiver

import android.content.Context
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID

internal object NotificationsEnabledManager {

    internal fun onCheckState(context: Context) {
        val notificationsEnabled = RetenoNotificationChannel.isNotificationsEnabled(context)
        val defaultChannelEnabled =
            RetenoNotificationChannel.isNotificationChannelEnabled(context, DEFAULT_CHANNEL_ID)
        RetenoInternalEntryPoint.facade.notificationsEnabled(notificationsEnabled && defaultChannelEnabled)
    }
}
