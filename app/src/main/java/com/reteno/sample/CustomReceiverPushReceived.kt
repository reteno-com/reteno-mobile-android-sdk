package com.reteno.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose

class CustomReceiverPushReceived : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(TAG, "onReceive(): ", intent.extras.toStringVerbose())
    }

    companion object {
        private val TAG = CustomReceiverPushReceived::class.java.getSimpleName()
    }
}
