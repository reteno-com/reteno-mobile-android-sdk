package com.reteno.push

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import com.reteno.RetenoApplication
import com.reteno.RetenoImpl
import com.reteno.config.RestConfig
import com.reteno.di.ServiceLocator
import com.reteno.domain.controller.ContactController
import com.reteno.model.device.Device
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager


class RetenoNotificationService {

    private val serviceLocator: ServiceLocator =
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl).serviceLocator

    private val restConfig: RestConfig = serviceLocator.restConfigProvider.get()
    private val sharedPrefsManager: SharedPrefsManager =
        serviceLocator.sharedPrefsManagerProvider.get()
    private val contactController: ContactController =
        serviceLocator.contactControllerProvider.get()


    fun onNewToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/

        val oldToken = sharedPrefsManager.getFcmToken()
        if (token != oldToken) {
            sharedPrefsManager.saveFcmToken(token)

            val contact = Device.createDevice(
                deviceId = restConfig.deviceId.id,
                pushToken = token
            )
            contactController.onNewContact(contact)
        }
    }

    fun showNotification(data: Bundle) {
        val context = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "showNotification(): ", "context = [" , context , "], data = [" , data.toString() , "]")
        /*@formatter:on*/
        // TODO: SEND MESSAGE_DELIVERED event to backend to track it

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