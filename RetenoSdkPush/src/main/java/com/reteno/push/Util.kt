package com.reteno.push

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.getApplicationMetaData

internal object Util {

    internal fun tryToSendToCustomReceiverPushReceived(context: Context, data: Bundle) {
        tryToSendToReceiver(context, Constants.META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED, data)
    }

    internal fun tryToSendToCustomReceiverNotificationClicked(context: Context, data: Bundle) {
        tryToSendToReceiver(context, Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED, data)
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

    private fun tryToSendToReceiver(context: Context, metaDataKey: String, data: Bundle) {
        val receiver = context.getApplicationMetaData().getString(metaDataKey) ?: return
        val forwardIntent = Intent()
        forwardIntent.setClassName(context, receiver)
        forwardIntent.putExtras(data)
        context.sendBroadcast(forwardIntent)
    }
}

internal fun Bundle.toStringMap(): Map<String, String?> {
    return keySet()
        .associateWith {
            getString(it)
        }
}
