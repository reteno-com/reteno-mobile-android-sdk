package com.reteno.push.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger

class AppResumeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
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
        private val TAG: String = AppResumeReceiver::class.java.simpleName
    }
}