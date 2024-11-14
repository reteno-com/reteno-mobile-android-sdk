package com.reteno.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger.i
import com.reteno.core.util.toStringVerbose

class CustomReceiverNotificationClicked : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        i(TAG, "onReceive(): ", intent.extras.toStringVerbose())
    }

    companion object {
        private val TAG = CustomReceiverNotificationClicked::class.java.getSimpleName()
    }
}
