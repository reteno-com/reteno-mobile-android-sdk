package com.reteno.push.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.data.remote.mapper.fromJsonOrNull
import com.reteno.core.util.Procedure
import com.reteno.core.util.Util
import com.reteno.push.R

internal object RetenoNotificationChannel {

    internal var DEFAULT_CHANNEL_ID: String = "defaultId"
        private set

    private const val FALLBACK_DEFAULT_CHANNEL_NAME = "default"
    private const val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = "description"

    internal fun isNotificationsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.areNotificationsEnabled()
    }

    internal fun isNotificationChannelEnabled(context: Context, channelId: String?): Boolean {
        if (channelId.isNullOrBlank()) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId)
        if (channel == null) {
            createDefaultChannel(context)
            return true
        }
        val config = RetenoInternalEntryPoint.facade.getDefaultNotificationChannelConfig()
        if (config != null) createConfiguredChannel(context, channelId, config)
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    internal fun createDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val config = RetenoInternalEntryPoint.facade.getDefaultNotificationChannelConfig()
        if (config != null) {
            createConfiguredChannel(context, DEFAULT_CHANNEL_ID, config)
        } else {
            createChannelFromData(context, retrieveDefaultNotificationChannelData(context))
        }
    }

    private fun createConfiguredChannel(context: Context, channelId: String, config: Procedure<NotificationChannelCompat.Builder>) {
        val channel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .apply(config::execute)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun createChannelFromData(context: Context, channelData: NotificationChannelData) {
        DEFAULT_CHANNEL_ID = channelData.id
        val channel = NotificationChannel(DEFAULT_CHANNEL_ID, channelData.name, channelData.importance).apply {
            description = channelData.description
            enableLights(channelData.enableLights)
            lightColor = channelData.lightColor
            enableVibration(channelData.enableVibration)
            channelData.vibrationPattern?.let { vibrationPattern = it.toLongArray() }
            lockscreenVisibility = channelData.lockscreenVisibility
            setBypassDnd(channelData.bypassDnd)
            setShowBadge(channelData.showBadge)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun retrieveDefaultNotificationChannelData(context: Context): NotificationChannelData =
        parseChannelData(RetenoInternalEntryPoint.facade.getDefaultNotificationChannel()) ?: readBundledChannelData(context)

    private fun readBundledChannelData(context: Context): NotificationChannelData {
        val json = Util.readFromRaw(context, R.raw.default_channel) ?: return fallbackChannelData()
        val channelData = parseChannelData(json) ?: return fallbackChannelData()
        RetenoInternalEntryPoint.facade.saveDefaultNotificationChannel(json)
        return channelData
    }

    private fun parseChannelData(json: String): NotificationChannelData? =
        runCatching { json.fromJsonOrNull<NotificationChannelData>() }.getOrNull()

    private fun fallbackChannelData() = NotificationChannelData(
        DEFAULT_CHANNEL_ID,
        FALLBACK_DEFAULT_CHANNEL_NAME,
        FALLBACK_DEFAULT_CHANNEL_DESCRIPTION
    )
}
