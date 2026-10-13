package com.reteno.core.internal.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.reteno.core.util.queryBroadcastReceivers

internal class AppBroadcaster(private val context: Context) {

    fun send(action: String): Int {
        val intent = Intent(action).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val receivers = context.queryBroadcastReceivers(intent).filterNotNull().mapNotNull { it.activityInfo }
        receivers.forEach { receiver ->
            context.sendBroadcast(Intent(intent).setComponent(ComponentName(receiver.packageName, receiver.name)))
        }
        return receivers.size
    }
}
