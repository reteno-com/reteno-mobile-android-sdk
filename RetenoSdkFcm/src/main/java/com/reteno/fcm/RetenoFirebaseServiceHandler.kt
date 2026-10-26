package com.reteno.fcm

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import com.reteno.core.util.Logger
import com.reteno.push.RetenoNotificationService

class RetenoFirebaseServiceHandler(private val pushService: RetenoNotificationService) {

    /**
     * Call from your implementation of [com.google.firebase.messaging.FirebaseMessagingService.onCreate]
     */
    fun onCreate() {
        Logger.i(TAG, "onCreate(): ")
        // TODO: Nothing to do yet. Maybe remove later
    }

    /**
     * Call from your implementation of [com.google.firebase.messaging.FirebaseMessagingService.onNewToken]
     */
    fun onNewToken(token: String) {
        pushService.onNewToken(token)
    }

    /**
     * Call from your implementation of
     * [com.google.firebase.messaging.FirebaseMessagingService.onMessageReceived]
     */
    fun onMessageReceived(remoteMessage: RemoteMessage) {
        val messageMap = remoteMessage.data
        /*@formatter:off*/ Logger.i(TAG, "onMessageReceived(): ", "messageMap = [" , messageMap , "]")
        /*@formatter:on*/
        pushService.handleNotification(getBundle(messageMap))
    }

    /**
     * @param messageMap [RemoteMessage]'s data map.
     */
    private fun getBundle(messageMap: Map<String, String>): Bundle {
        val bundle = Bundle()
        for ((key, value) in messageMap) {
            bundle.putString(key, value)
        }
        return bundle
    }

    companion object {
        private val TAG: String = RetenoFirebaseServiceHandler::class.java.simpleName
    }
}