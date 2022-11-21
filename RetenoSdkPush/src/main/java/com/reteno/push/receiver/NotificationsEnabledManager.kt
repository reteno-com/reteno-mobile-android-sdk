package com.reteno.push.receiver

import android.content.Context
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID

internal object NotificationsEnabledManager {

    private val TAG: String = NotificationsEnabledManager::class.java.simpleName

    internal fun onCheckState(context: Context) {
        /*@formatter:off*/ Logger.i(TAG, "onCheckState(): ", "context = [" , context , "]")
        /*@formatter:on*/
        val notificationsEnabled = RetenoNotificationChannel.isNotificationsEnabled(context)
        val defaultChannelEnabled =
            RetenoNotificationChannel.isNotificationChannelEnabled(context, DEFAULT_CHANNEL_ID)

        val reteno = ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
        val contactController = reteno.serviceLocator.contactControllerProvider.get()
        contactController.notificationsEnabled(notificationsEnabled && defaultChannelEnabled)

        val scheduleController = reteno.serviceLocator.scheduleControllerProvider.get()
        scheduleController.startScheduler()
    }
}