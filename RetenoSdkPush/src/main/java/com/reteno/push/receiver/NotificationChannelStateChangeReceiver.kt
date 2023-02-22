package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported

class NotificationChannelStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "context = [" , context , "], intent = [" , intent , "]")
        /*@formatter:on*/
        try {
            context?.let(NotificationsEnabledManager::onCheckState)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onReceive(): ", ex)
            /*@formatter:on*/
        }
    }

    companion object {
        private val TAG: String = NotificationChannelStateChangeReceiver::class.java.simpleName
    }
}