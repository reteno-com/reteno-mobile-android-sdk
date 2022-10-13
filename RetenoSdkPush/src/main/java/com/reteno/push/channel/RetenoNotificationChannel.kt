package com.reteno.push.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.text.TextUtils
import com.reteno.data.remote.mapper.fromJson
import com.reteno.data.remote.mapper.fromJsonOrNull
import com.reteno.push.R
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager
import com.reteno.util.Util

object RetenoNotificationChannel {

    val TAG: String = RetenoNotificationChannel::class.java.simpleName
    var DEFAULT_CHANNEL_ID: String = "DEFAULT_CHANNEL_ID"
        private set

    private const val FALLBACK_DEFAULT_CHANNEL_NAME = "default"
    private const val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = "description"

    internal fun createDefaultChannel(context: Context) {
        val defaultChannel = retrieveDefaultNotificationChannel(context)

        DEFAULT_CHANNEL_ID = defaultChannel.id
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            defaultChannel.name,
            defaultChannel.importance
        ).apply {
            description = defaultChannel.description
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Configures default notification channel, which will be used when channel isn't specified on
     * Android O.
     *
     * @param context The application context.
     * @param channel Default channel details.
     */
    fun configureDefaultNotificationChannel(context: Context, channel: String) {
        try {
            if (TextUtils.isEmpty(channel)) {
                return
            }
            storeDefaultNotificationChannel(context, channel)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "configureDefaultNotificationChannel(): ", t)
            /*@formatter:on*/
        }
    }

    /**
     * Stores default notification channel id.
     *
     * @param context The application context.
     * @param channel Channel configuration to store.
     */
    private fun storeDefaultNotificationChannel(context: Context, channel: String) {
        val sharedPrefsManager = SharedPrefsManager(context)
        sharedPrefsManager.saveDefaultNotificationChannel(channel)
    }

    /**
     * Retrieves stored default notification channel.
     *
     * @param context The application context.
     * @return The stored default channel or null.
     */
    private fun retrieveDefaultNotificationChannel(context: Context): NotificationChannelData {
        val defaultChannelOrNull: NotificationChannelData? =
            try {
                val sharedPrefsManager = SharedPrefsManager(context)
                val jsonChannels = sharedPrefsManager.getDefaultNotificationChannel()
                jsonChannels.fromJsonOrNull()
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.e(TAG, "retrieveNotificationChannels(): ", e)
                /*@formatter:on*/
                null
            }


        return defaultChannelOrNull ?: try {
            val defaultJson = Util.readFromRaw(context, R.raw.default_channel) ?: ""

            configureDefaultNotificationChannel(context, defaultJson)
            defaultJson.fromJson()
        } catch (e: Exception) {
            /*@formatter:off*/ Logger.e(TAG, "retrieveNotificationChannels(): ", e)
            /*@formatter:on*/
            NotificationChannelData(
                DEFAULT_CHANNEL_ID,
                FALLBACK_DEFAULT_CHANNEL_NAME,
                FALLBACK_DEFAULT_CHANNEL_DESCRIPTION
            )
        }
    }
}