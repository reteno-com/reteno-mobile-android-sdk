package com.reteno.push.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.text.TextUtils
import com.reteno.RetenoApplication
import com.reteno.RetenoImpl
import com.reteno.data.remote.mapper.fromJson
import com.reteno.data.remote.mapper.fromJsonOrNull
import com.reteno.push.R
import com.reteno.util.Logger
import com.reteno.util.Util

object RetenoNotificationChannel {

    val TAG: String = RetenoNotificationChannel::class.java.simpleName
    var DEFAULT_CHANNEL_ID: String = "DEFAULT_CHANNEL_ID"
        private set

    private const val FALLBACK_DEFAULT_CHANNEL_NAME = "default"
    private const val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = "description"

    private val configRepository =
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
            .serviceLocator
            .configRepositoryProvider
            .get()

    @JvmStatic
    fun createDefaultChannel() {
        val context = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "createDefaultChannel(): ", "context = [" , context , "]")
        /*@formatter:on*/

        val channelData = retrieveDefaultNotificationChannelData()
        DEFAULT_CHANNEL_ID = channelData.id

        val channel = NotificationChannel(
            channelData.id,
            channelData.name,
            channelData.importance
        ).apply {
            description = channelData.description
            channelData.groupId?.let(::setGroup)
            enableLights(channelData.enableLights)
            lightColor = channelData.lightColor
            enableVibration(channelData.enableVibration)
            channelData.vibrationPattern?.toLongArray()?.let(::setVibrationPattern)
            lockscreenVisibility = channelData.lockscreenVisibility
            setBypassDnd(channelData.bypassDnd)
            setShowBadge(channelData.showBadge)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Configures default notification channel, which will be used when channel isn't specified on
     * Android O.
     *
     * @param channel Default channel details.
     */
    @JvmStatic
    fun configureDefaultNotificationChannel(channel: String) {
        try {
            if (TextUtils.isEmpty(channel)) {
                return
            }
            storeDefaultNotificationChannel(channel)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "configureDefaultNotificationChannel(): ", t)
            /*@formatter:on*/
        }
    }

    /**
     * Stores default notification channel id.
     *
     * @param channel Channel configuration to store.
     */
    @JvmStatic
    private fun storeDefaultNotificationChannel(channel: String) {
        configRepository.saveDefaultNotificationChannel(channel)
    }

    /**
     * Retrieves stored default notification channel.
     *
     * @return The stored default channel or null.
     */
    @JvmStatic
    private fun retrieveDefaultNotificationChannelData(): NotificationChannelData {
        val defaultChannelOrNull: NotificationChannelData? =
            try {
                val jsonChannel = configRepository.getDefaultNotificationChannel()
                jsonChannel.fromJsonOrNull()
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.e(TAG, "retrieveNotificationChannels(): ", e)
                /*@formatter:on*/
                null
            }


        return defaultChannelOrNull ?: try {
            val defaultJson = Util.readFromRaw(R.raw.default_channel) ?: ""

            configureDefaultNotificationChannel(defaultJson)
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