package com.reteno.push

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import com.reteno.RetenoApplication
import com.reteno.RetenoImpl
import com.reteno.config.RestConfig
import com.reteno.di.ServiceLocator
import com.reteno.domain.controller.ContactController
import com.reteno.model.device.Device
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager


class RetenoNotificationService(private val application: Application) {

    private val serviceLocator: ServiceLocator =
        ((application as RetenoApplication).getRetenoInstance() as RetenoImpl).serviceLocator

    private val restConfig: RestConfig = serviceLocator.restConfigProvider.get()
    private val sharedPrefsManager: SharedPrefsManager =
        serviceLocator.sharedPrefsManagerProvider.get()
    private val contactController: ContactController =
        serviceLocator.contactControllerProvider.get()


    fun onNewToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "application = [" , application , "], token = [" , token , "]")
        /*@formatter:on*/
        val oldToken = sharedPrefsManager.getFcmToken()
        if (token != oldToken) {
            sharedPrefsManager.saveFcmToken(token)

            val contact = Device.createDevice(
                context = application.applicationContext,
                deviceId = restConfig.deviceId.id,
                pushToken = token
            )
            contactController.onNewContact(contact)
        }
    }

    fun showNotification(data: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "onPushReceived(): ", "data = [" , data.toString() , "]")
        /*@formatter:on*/
        // TODO: SEND MESSAGE_DELIVERED event to backend to track it

        Util.tryToSendToCustomReceiverPushReceived(application, data)

        RetenoNotificationHelper.createChannel(application)
        val id = RetenoNotificationHelper.getNotificationId(data)
        val builder = RetenoNotificationHelper.getNotificationBuilderCompat(application, data)

        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }



    companion object {
        val TAG: String = RetenoNotificationService::class.java.simpleName
    }
}