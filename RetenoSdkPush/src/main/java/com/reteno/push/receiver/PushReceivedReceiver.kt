package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.events.NotificationReceived

class PushReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(TAG, "onReceive():", intent.extras.toStringVerbose())
        NotificationReceived.notifyListeners(intent.extras ?: Bundle.EMPTY)
    }

    companion object {
        private val TAG = PushReceivedReceiver::class.java.getSimpleName()
    }
}