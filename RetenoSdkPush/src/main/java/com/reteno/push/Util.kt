package com.reteno.push

import android.content.Intent
import android.os.Bundle
import com.reteno.RetenoImpl
import com.reteno.util.getApplicationMetaData

object Util {
    fun tryToSendToCustomReceiverPushReceived(data: Bundle) {
        val receiver = RetenoImpl.application.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED)
        tryToSendToReceiver(receiver, data)
    }

    fun tryToSendToCustomReceiverNotificationClicked(data: Bundle) {
        val receiver = RetenoImpl.application.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED)
        tryToSendToReceiver(receiver, data)
    }

    private fun tryToSendToReceiver(receiver: String?, data: Bundle) =
        receiver?.let {
            val context = RetenoImpl.application
            // Forward Intent to a client broadcast receiver.
            val forwardIntent = Intent()
            forwardIntent.setClassName(context, it)
            forwardIntent.putExtras(data)
            context.sendBroadcast(forwardIntent)
        }
}