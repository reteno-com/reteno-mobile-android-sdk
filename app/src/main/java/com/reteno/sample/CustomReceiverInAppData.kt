package com.reteno.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose

class CustomReceiverInAppData : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(TAG, "onReceive(): ", intent.extras.toStringVerbose())
        Toast.makeText(context, "CustomInAppHandler: ${intent.extras.toStringVerbose()}", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val TAG = CustomReceiverInAppData::class.java.simpleName
    }
}