package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger

class NotificationChannelStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "context = [" , context , "], intent = [" , intent , "]")
        /*@formatter:on*/
        context?.let(NotificationsEnabledManager::onCheckState)
    }

    companion object {
        private val TAG: String = NotificationChannelStateChangeReceiver::class.java.simpleName
    }
}