package com.reteno.push.interceptor.click

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported

class RetenoNotificationClickedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOsVersionSupported()) return
        try {
            handleClick(intent)
        } catch (ex: Throwable) {
            Logger.e(TAG, "onCreate(): ", ex)
        }
        finish()
    }

    private fun handleClick(intent: Intent) {
        val extras = intent.extras ?: return launchApp(intent)
        val handler = NotificationClickHandler(this, extras)
        handler.recordClick()
        handler.dismissAndNotifyHost()
        val deeplinkIntent = handler.deeplinkIntent() ?: return launchApp(intent)
        try {
            startActivity(deeplinkIntent)
        } catch (ex: ActivityNotFoundException) {
            launchApp(deeplinkIntent)
        }
    }

    private fun launchApp(intent: Intent) {
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(this) ?: return
        intent.component = launchIntent.component
        if (NotificationClickHandler.shouldOpenApp(intent.extras)) startActivity(intent)
    }

    companion object {
        private val TAG: String = RetenoNotificationClickedActivity::class.java.simpleName
    }
}
