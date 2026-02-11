package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.events.NotificationDelete

class ReceiverNotificationDeleted : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(TAG, "onReceive():", intent.extras.toStringVerbose())
        NotificationDelete.notifyListeners(intent.extras ?: Bundle.EMPTY)
    }

    companion object Companion {
        private val TAG = ReceiverNotificationDeleted::class.java.simpleName
    }
}