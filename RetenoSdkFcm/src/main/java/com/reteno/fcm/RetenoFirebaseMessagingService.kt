package com.reteno.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reteno.push.RetenoPushService
import com.reteno.util.Logger

open class RetenoFirebaseMessagingService : FirebaseMessagingService() {

    private val pushService: RetenoPushService by lazy {
        RetenoPushService(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        pushService.toString()
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "")
        /*@formatter:on*/
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        pushService.onNewFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        /*@formatter:off*/ Logger.i(TAG, "onMessageReceived(): ", "message = [" , message , "]")
        /*@formatter:on*/
//        pushService.onMessageReceived(message)
    }

    companion object {
        val TAG: String = RetenoFirebaseMessagingService::class.java.simpleName
    }
}