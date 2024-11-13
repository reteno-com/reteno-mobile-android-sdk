package com.reteno.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.reteno.core.util.Logger.i

class CustomPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras
        Toast.makeText(
            context,
            "Received custom push. Bundle.size = " + extras!!.size(),
            Toast.LENGTH_SHORT
        ).show()
        /*@formatter:off*/i(TAG, "onReceive(): ", "context = [", context, "], intent = [", intent, "]")
            /*@formatter:on*/
    }

    companion object {
        private val TAG = CustomPushReceiver::class.java.getSimpleName()
    }
}
