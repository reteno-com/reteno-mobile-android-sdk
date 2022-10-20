package com.reteno.push

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.InteractionController
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.core.util.Logger


class RetenoNotificationService {

    private val reteno =
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    private val serviceLocator = reteno.serviceLocator
    private val contactController = serviceLocator.contactControllerProvider.get()
    private val interactionController = serviceLocator.interactionControllerProvider.get()

    fun onNewToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        contactController.onNewFcmToken(token)
    }

    fun showNotification(data: Bundle) {
        val context = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "showNotification(): ", "context = [" , context , "], data = [" , data.toString() , "]")
        /*@formatter:on*/
        data.getString(KEY_ES_INTERACTION_ID)?.let { interactionId ->
            interactionController.onInteraction(interactionId, InteractionStatus.DELIVERED)
        }

        Util.tryToSendToCustomReceiverPushReceived(data)

        RetenoNotificationChannel.createDefaultChannel()
        val id = RetenoNotificationHelper.getNotificationId(data)
        val builder = RetenoNotificationHelper.getNotificationBuilderCompat(data)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    companion object {
        val TAG: String = RetenoNotificationService::class.java.simpleName
    }
}