package com.reteno.push

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.util.getApplicationMetaData

object Util {
    fun tryToSendToCustomReceiverPushReceived(context: Context, data: Bundle) {
        val receiver = context.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED)
        tryToSendToReceiver(receiver, context, data)
    }

    fun tryToSendToCustomReceiverNotificationClicked(context: Context, data: Bundle) {
        val receiver = context.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED)
        tryToSendToReceiver(receiver, context, data)
    }

    private fun tryToSendToReceiver(receiver: String?, context: Context, data: Bundle) =
        receiver?.let {
            // Forward Intent to a client broadcast receiver.
            val forwardIntent = Intent()
            forwardIntent.setClassName(context, it)
            forwardIntent.putExtras(data)
            context.sendBroadcast(forwardIntent)
        }
}