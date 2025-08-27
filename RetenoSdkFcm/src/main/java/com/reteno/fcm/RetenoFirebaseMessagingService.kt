package com.reteno.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported
import com.reteno.push.Constants
import com.reteno.push.RetenoNotificationService

open class RetenoFirebaseMessagingService : FirebaseMessagingService() {

    // TODO move to serviceProvider if number of object allocations grow
    private val pushService: RetenoNotificationService by lazy {
        RetenoNotificationService(this)
    }
    private val handler: RetenoFirebaseServiceHandler by lazy {
        RetenoFirebaseServiceHandler(pushService)
    }

    override fun onCreate() {
        super.onCreate()
        if (!isOsVersionSupported()) {
            return
        }
        handler.toString()
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "")
        /*@formatter:on*/
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "onNewToken(): ", "token = [" , token , "]")
        /*@formatter:on*/
        try {
            handler.onNewToken(token)
        } catch (t: Throwable) {
            Logger.e(TAG, "onNewPushReceived", t)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "onMessageReceived(): ", "context = [", application, "] message.data = [" , message.toString() , "]")
        /*@formatter:on*/
        try {
            handler.onMessageReceived(message)
        } catch (t: Throwable) {
            Logger.e(TAG, "onNewPushReceived", t)
        }
    }

    public fun isRetenoMessage(message: RemoteMessage): Boolean {
        return message.data.containsKey(Constants.KEY_ES_INTERACTION_ID)
    }

    companion object {
        private val TAG: String = RetenoFirebaseMessagingService::class.java.simpleName
    }
}