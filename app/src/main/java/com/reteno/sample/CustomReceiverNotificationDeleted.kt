package com.reteno.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.reteno.core.util.Logger
import com.reteno.core.util.getAppName
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants

class CustomReceiverNotificationDeleted : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(TAG, "onReceive(): ", intent.extras.toStringVerbose())
        Toast.makeText(context, "Notification deleted: ${intent.extras?.getString(Constants.KEY_ES_TITLE) ?: context.getAppName()}", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val TAG = CustomReceiverNotificationDeleted::class.java.simpleName
    }
}