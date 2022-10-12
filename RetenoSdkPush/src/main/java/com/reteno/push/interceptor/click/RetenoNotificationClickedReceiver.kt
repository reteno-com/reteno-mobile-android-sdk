package com.reteno.push.interceptor.click

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.push.Constants.KEY_ES_LINK
import com.reteno.util.Logger

class RetenoNotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "notification clicked. Context = [" , context , "], intent.extras = [" , intent?.extras , "]")
        /*@formatter:on*/
        try {
            intent?.getStringExtra(KEY_ES_LINK)?.let { link ->
                // TODO: Handle deepling here

                return
            }

            getAppLaunchIntent(context)?.let(context::startActivity)
        } catch (t: Throwable) {
            Logger.e(TAG, "onReceive() ", t)
        }
    }

    private fun getAppLaunchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)


    companion object {
        val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}