package com.reteno.core.internal.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.reteno.core.util.queryBroadcastReceivers

internal interface AppBroadcasts {
    fun send(action: String): Int
}

internal class AppBroadcaster(private val context: Context) : AppBroadcasts {

    override fun send(action: String): Int = deliver(Intent(action))

    fun sendToApp(action: String, extras: Map<String, String>): Int {
        val intent = Intent(action).setPackage(context.packageName)
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
        return deliver(intent)
    }

    private fun deliver(intent: Intent): Int {
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val receivers = context
            .queryBroadcastReceivers(intent)
            .filterNotNull()
            .mapNotNull { it.activityInfo }
        receivers.forEach { receiver ->
            context.sendBroadcast(Intent(intent).setComponent(ComponentName(receiver.packageName, receiver.name)))
        }
        return receivers.size
    }
}
