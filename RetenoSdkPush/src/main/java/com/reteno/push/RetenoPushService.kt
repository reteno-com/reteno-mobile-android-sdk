package com.reteno.push

import android.content.Context
import com.reteno.config.RestConfig
import com.reteno.di.ServiceLocator
import com.reteno.domain.controller.ContactController
import com.reteno.model.device.Device
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager

class RetenoPushService(applicationContext: Context) {

    private val serviceLocator: ServiceLocator = ServiceLocator(applicationContext)

    private val restConfig: RestConfig = serviceLocator.restConfigProvider.get()
    private val sharedPrefsManager: SharedPrefsManager =
        serviceLocator.sharedPrefsManagerProvider.get()
    private val contactController: ContactController =
        serviceLocator.contactControllerProvider.get()


    fun onNewFcmToken(context: Context, token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "context = [" , context , "], token = [" , token , "]")
        /*@formatter:on*/
        val oldToken = getFcmToken()
        if (token != oldToken) {
            sharedPrefsManager.saveFcmToken(token)

            val contact = Device.createDevice(
                context = context,
                deviceId = restConfig.deviceId.id,
                pushToken = token
            )
            contactController.onNewContact(contact)
        }
    }

    fun getFcmToken(): String {
        val token = sharedPrefsManager.getFcmToken()
        /*@formatter:off*/ Logger.i(TAG, "getFcmToken(): ", "token = ", token)
        /*@formatter:on*/
        return token
    }

    companion object {
        val TAG: String = RetenoPushService::class.java.simpleName
    }
}