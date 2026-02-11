package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.events.NotificationClick

class ReceiverNotificationClicked : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(TAG, "onReceive():", intent.extras.toStringVerbose())
        NotificationClick.notifyListeners(intent.extras ?: Bundle.EMPTY)
    }

    companion object Companion {
        private val TAG = ReceiverNotificationClicked::class.java.getSimpleName()
    }
}
