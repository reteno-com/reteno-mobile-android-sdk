package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.events.InAppCustomData
import com.reteno.push.events.NotificationInAppCustomDataReceived

class ReceiverInAppData : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(TAG, "onReceive(): ", intent.extras.toStringVerbose())
        val extras = intent.extras ?: Bundle.EMPTY
        val url = extras.getString("url")
        val source = extras.getString("inapp_source").orEmpty()
        val id = extras.getString("inapp_id").orEmpty()
        val keys = extras.keySet()
        keys.removeAll(listOf("url", "inapp_source", "inapp_id"))
        val dataMap = mutableMapOf<String, String>()
        keys.forEach {
            extras.getString(it)?.let { value ->
                dataMap[it] = value
            }
        }
        NotificationInAppCustomDataReceived.notifyListeners(
            InAppCustomData(
                url = url,
                source = source,
                inAppId = id,
                data = dataMap
            )
        )
    }

    companion object Companion {
        private val TAG = ReceiverInAppData::class.java.simpleName
    }
}