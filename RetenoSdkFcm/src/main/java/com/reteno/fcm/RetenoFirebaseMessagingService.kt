package com.reteno.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reteno.push.RetenoPushService
import com.reteno.util.Logger

open class RetenoFirebaseMessagingService : FirebaseMessagingService() {

    private val handler: RetenoFirebaseServiceHandler by lazy {
        RetenoFirebaseServiceHandler(application)
    }

    override fun onCreate() {
        super.onCreate()
        handler.toString()
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "")
        /*@formatter:on*/
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        try {
            handler.onNewToken(token)
        } catch (t: Throwable) {
            Logger.e(RetenoPushService.TAG, "onNewPushReceived", t)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        /*@formatter:off*/ Logger.i(TAG, "onMessageReceived(): ", "application = ", application, " message.data = [" , message.toString() , "]")
        /*@formatter:on*/
        try {
            handler.onMessageReceived(message)
        } catch (t: Throwable) {
            Logger.e(RetenoPushService.TAG, "onNewPushReceived", t)
        }
    }

    companion object {
        val TAG: String = RetenoFirebaseMessagingService::class.java.simpleName
    }
}