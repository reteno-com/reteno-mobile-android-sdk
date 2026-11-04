package com.reteno.push.interceptor.click

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported

class RetenoNotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!isOsVersionSupported()) return
        try {
            handleClick(context, intent ?: return)
        } catch (ex: Throwable) {
            Logger.e(TAG, "onReceive(): ", ex)
        }
    }

    private fun handleClick(context: Context, intent: Intent) {
        val extras = intent.extras ?: return launchApp(context, intent)
        val handler = NotificationClickHandler(context, extras)
        handler.recordClick()
        handler.dismissAndNotifyHost()
        val deeplinkIntent = handler.deeplinkIntent() ?: return launchApp(context, intent)
        try {
            context.startActivity(deeplinkIntent)
        } catch (ex: ActivityNotFoundException) {
            launchApp(context, deeplinkIntent)
        }
    }

    private fun launchApp(context: Context, intent: Intent) {
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(context) ?: return
        intent.extras?.let(launchIntent::putExtras)
        if (NotificationClickHandler.shouldOpenApp(intent.extras)) context.startActivity(launchIntent)
    }

    companion object {
        private val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}
