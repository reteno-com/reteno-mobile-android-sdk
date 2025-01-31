package com.reteno.push

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoImpl
import com.reteno.core.util.getApplicationMetaData

internal object Util {

    internal fun tryToSendToCustomReceiverPushReceived(data: Bundle) {
        val receiver = RetenoImpl.instance.application.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED)
        tryToSendToReceiver(receiver, data)
    }

    internal fun tryToSendToCustomReceiverNotificationClicked(data: Bundle) {
        val receiver = RetenoImpl.instance.application.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED)
        tryToSendToReceiver(receiver, data)
    }

    internal fun getLinkFromBundle(bundle: Bundle): Pair<String, String> {
        return if (bundle.getBoolean(Constants.KEY_ACTION_BUTTON, false)) {
            val linkWrapped = bundle.getString(Constants.KEY_BTN_ACTION_LINK_WRAPPED).orEmpty()
            val linkUnwrapped = bundle.getString(Constants.KEY_BTN_ACTION_LINK_UNWRAPPED).orEmpty()
            linkWrapped to linkUnwrapped
        } else {
            val linkWrapped = bundle.getString(Constants.KEY_ES_LINK_WRAPPED).orEmpty()
            val linkUnwrapped = bundle.getString(Constants.KEY_ES_LINK_UNWRAPPED).orEmpty()
            linkWrapped to linkUnwrapped
        }
    }

    /**
     *  For actions with buttons, we have to manually close the notification.
     */
    internal fun closeNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    private fun tryToSendToReceiver(receiver: String?, data: Bundle) =
        receiver?.let {
            val context = RetenoImpl.instance.application
            // Forward Intent to a client broadcast receiver.
            val forwardIntent = Intent()
            forwardIntent.setClassName(context, it)
            forwardIntent.putExtras(data)
            context.sendBroadcast(forwardIntent)
        }
}