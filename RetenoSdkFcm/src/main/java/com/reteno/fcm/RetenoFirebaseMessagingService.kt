package com.reteno.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reteno.util.Logger

open class RetenoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "onCreate(): ", "")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.d(TAG, "onNewToken(): ", "token = [" , token , "]")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d(TAG, "onMessageReceived(): ", "message = [" , message , "]")
    }

    companion object {
        val TAG: String = RetenoFirebaseMessagingService::class.java.simpleName
    }
}