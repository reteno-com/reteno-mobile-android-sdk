package com.reteno.push

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_NOTIFICATION_ID
import com.reteno.push.channel.RetenoNotificationChannel


class RetenoNotificationService(
    private val context: Context
) {

    private val notificationHelper = RetenoNotificationHelper(context)

    fun onNewToken(token: String) {
        RetenoInternalEntryPoint.facade.onNewFcmToken(token)
    }

    fun handleNotification(data: Bundle) {
        val interactionId = data.getString(KEY_ES_INTERACTION_ID)
        if (interactionId == null) {
            sendCustomPushBroadcast(data)
        } else {
            handleRetenoNotification(data, interactionId)
        }
    }

    private fun handleRetenoNotification(data: Bundle, interactionId: String) {
        Logger.i(TAG, "handleRetenoNotification(): ", "interactionId = [", interactionId, "]")
        Util.tryToSendToCustomReceiverPushReceived(context, data)
        RetenoNotificationChannel.createDefaultChannel(context)
        showNotification(data)
        recordDelivery(interactionId)
    }

    private fun sendCustomPushBroadcast(bundle: Bundle) {
        val intent = Intent(Constants.BROADCAST_ACTION_CUSTOM_PUSH)
            .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            .putExtras(bundle)

        for (info in context.queryBroadcastReceivers(intent).filterNotNull()) {
            val receiver = info.activityInfo
            intent.component = ComponentName(receiver.packageName, receiver.name)
            context.sendBroadcast(intent)
        }
    }

    private fun showNotification(data: Bundle) {
        val id = notificationHelper.getNotificationId(data)
        data.putInt(KEY_NOTIFICATION_ID, id)

        val builder = notificationHelper.getNotificationBuilderCompat(data)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    private fun recordDelivery(interactionId: String) {
        val channelEnabled = RetenoNotificationChannel.isNotificationChannelEnabled(
            context,
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID
        )
        val permissionsGranted = RetenoNotificationChannel.isNotificationsEnabled(context)
        if (channelEnabled && permissionsGranted) {
            RetenoInternalEntryPoint.facade.recordInteraction(interactionId, InteractionStatus.DELIVERED, forcePush = true)
        }
    }

    companion object {
        private val TAG: String = RetenoNotificationService::class.java.simpleName
    }
}
