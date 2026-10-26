package com.reteno.push.channel

import android.app.Notification
import android.app.NotificationManager

internal data class NotificationChannelData(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int = NotificationManager.IMPORTANCE_HIGH,
    val enableLights: Boolean = false,
    val lightColor: Int = 0,
    val enableVibration: Boolean = false,
    val vibrationPattern: List<Long>? = null,
    val lockscreenVisibility: Int = Notification.VISIBILITY_PUBLIC,
    val bypassDnd: Boolean = false,
    val showBadge: Boolean = false
)
