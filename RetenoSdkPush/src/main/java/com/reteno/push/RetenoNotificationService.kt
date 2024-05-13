package com.reteno.push

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.Reteno
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_NOTIFICATION_ID
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.receiver.NotificationsEnabledManager


class RetenoNotificationService(
    private val context: Context,
    private val reteno: Reteno
) {

    private val notificationHelper = RetenoNotificationHelper(context)

    fun onNewToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        val retenoImpl = reteno as RetenoImpl
        retenoImpl.onNewFcmToken(token)
    }

    fun handleNotification(data: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "handleNotification(): ", "data = [" , data.toString() , "]")
        /*@formatter:on*/

        val hasInteractionId = data.containsKey(KEY_ES_INTERACTION_ID)
        if (hasInteractionId) {
            handleRetenoNotification(data)
        } else {
            sendCustomPushBroadcast(data)
        }
    }

    private fun handleRetenoNotification(data: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "handleRetenoNotification(): ", "data = [" , data.toString() , "]")
        /*@formatter:on*/
        Util.tryToSendToCustomReceiverPushReceived(data)
        RetenoNotificationChannel.createDefaultChannel(context)
        showNotification(data)
        handleInteractionStatus(data)
        NotificationsEnabledManager.onCheckState(context)
    }

    private fun sendCustomPushBroadcast(bundle: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "sendCustomPushBroadcast(): ", "bundle = [" , bundle , "]")
        /*@formatter:on*/
        val intent = Intent(Constants.BROADCAST_ACTION_CUSTOM_PUSH)
            .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            .putExtras(bundle)

        val infoList = context.queryBroadcastReceivers(intent)
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                context.sendBroadcast(intent)
            }
        }
    }

    private fun showNotification(data: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "showNotification(): ", "data = [" , data.toStringVerbose() , "]")
        /*@formatter:on*/
        val id = notificationHelper.getNotificationId(data)

        // Pass the Id for closing notifications after clicking on action buttons
        data.putInt(KEY_NOTIFICATION_ID, id)

        val builder = notificationHelper.getNotificationBuilderCompat(data)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    private fun handleInteractionStatus(data: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "handleInteractionStatus(): ", "data = [" , data , "]")
        /*@formatter:on*/
        val channelEnabled =
            RetenoNotificationChannel.isNotificationChannelEnabled(
                context,
                RetenoNotificationChannel.DEFAULT_CHANNEL_ID
            )
        val permissionsGranted =
            RetenoNotificationChannel.isNotificationsEnabled(context)

        if (channelEnabled && permissionsGranted) {
            data.getString(KEY_ES_INTERACTION_ID)?.let { interactionId ->
                val serviceLocator = (reteno as RetenoImpl).serviceLocator
                val interactionController = serviceLocator.interactionControllerProvider.get()
                val scheduleController = serviceLocator.scheduleControllerProvider.get()

                interactionController.onInteraction(interactionId, InteractionStatus.DELIVERED)
                scheduleController.forcePush()
            }
        }
    }

    companion object {
        private val TAG: String = RetenoNotificationService::class.java.simpleName
    }
}