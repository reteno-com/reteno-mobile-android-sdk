package com.reteno.push.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.fromJsonOrNull
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import com.reteno.push.R

internal object RetenoNotificationChannel {

    private val TAG: String = RetenoNotificationChannel::class.java.simpleName

    internal var DEFAULT_CHANNEL_ID: String = "defaultId"
        private set

    private const val FALLBACK_DEFAULT_CHANNEL_NAME = "default"
    private const val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = "description"

    internal fun isNotificationsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val enabled = manager.areNotificationsEnabled()

        /*@formatter:off*/ Logger.i(TAG, "isNotificationsEnabled(): ", "enabled = [" , enabled , "],")
        /*@formatter:on*/
        return enabled
    }

    internal fun isNotificationChannelEnabled(context: Context, channelId: String?): Boolean {
        val isEnabled = if (channelId.isNullOrBlank()) {
            false
        } else {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId)
            if (channel == null) {
                createDefaultChannel(context)
                true
            } else {
                applyChannelPropertiesFromDefaultConfig(context, channelId)
                channel.importance != NotificationManager.IMPORTANCE_NONE
            }
        }

        /*@formatter:off*/ Logger.i(TAG, "isNotificationChannelEnabled(): ", "channelId = [" , channelId , "], isEnabled = [", isEnabled, "]")
        /*@formatter:on*/
        return isEnabled
    }

    internal fun createDefaultChannel(context: Context) {
        /*@formatter:off*/ Logger.i(TAG, "createDefaultChannel(): ", "context = [" , context , "]")
        /*@formatter:on*/
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        RetenoInternalImpl.instance.getDefaultNotificationChannelConfig()?.let { config ->
            val channel = NotificationChannelCompat.Builder(
                DEFAULT_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .apply(config)
                .build()

            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        } ?: run {
            val channelData = retrieveDefaultNotificationChannelData(context)
            DEFAULT_CHANNEL_ID = channelData.id
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                channelData.name,
                channelData.importance
            ).apply {
                description = channelData.description
                enableLights(channelData.enableLights)
                lightColor = channelData.lightColor
                enableVibration(channelData.enableVibration)
                channelData.vibrationPattern?.toLongArray()?.let(::setVibrationPattern)
                lockscreenVisibility = channelData.lockscreenVisibility
                setBypassDnd(channelData.bypassDnd)
                setShowBadge(channelData.showBadge)
            }
            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        }
    }

    private fun applyChannelPropertiesFromDefaultConfig(context: Context, channelId: String) {
        RetenoInternalImpl.instance.getDefaultNotificationChannelConfig()?.let { config ->
            val channel = NotificationChannelCompat.Builder(
                channelId,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .apply(config)
                .build()

            NotificationManagerCompat.from(context)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Configures default notification channel, which will be used when channel isn't specified on
     * Android O.
     *
     * @param channel Default channel details.
     */
    private fun configureDefaultNotificationChannel(channel: String) {
        channel.takeUnless { it.isEmpty() }?.let {
            try {
                RetenoInternalImpl.instance.saveDefaultNotificationChannel(channel)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "configureDefaultNotificationChannel(): ", t)
                /*@formatter:on*/
            }
        }
    }

    /**
     * Retrieves stored default notification channel.
     *
     * @return The stored default channel or null.
     */
    private fun retrieveDefaultNotificationChannelData(context: Context): NotificationChannelData {
        val defaultChannelOrNull: NotificationChannelData? =
            try {
                val jsonChannel = RetenoInternalImpl.instance.getDefaultNotificationChannel()
                jsonChannel.fromJsonOrNull()
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.d(TAG, "retrieveDefaultNotificationChannelData(): Failed to read saved DefaultChannelId ", e)
                /*@formatter:on*/
                null
            }


        return defaultChannelOrNull ?: try {
            val defaultJson = Util.readFromRaw(context, R.raw.default_channel) ?: ""

            configureDefaultNotificationChannel(defaultJson)
            defaultJson.fromJson<NotificationChannelData>()
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "retrieveDefaultNotificationChannelData(): FALLBACK_MODE", e)
            /*@formatter:on*/
            NotificationChannelData(
                DEFAULT_CHANNEL_ID,
                FALLBACK_DEFAULT_CHANNEL_NAME,
                FALLBACK_DEFAULT_CHANNEL_DESCRIPTION
            )
        }
    }
}