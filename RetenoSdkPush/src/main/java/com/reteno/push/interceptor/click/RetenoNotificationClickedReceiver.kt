package com.reteno.push.interceptor.click

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.RetenoImpl
import com.reteno.push.Constants.KEY_ES_LINK
import com.reteno.push.Util
import com.reteno.util.Logger
import com.reteno.util.toStringVerbose

class RetenoNotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "notification clicked. Context = [" , context , "], intent.extras = [" , intent?.extras.toStringVerbose() , "]")
        /*@formatter:on*/
        try {
            intent?.extras?.let { bundle->
                Util.tryToSendToCustomReceiverNotificationClicked(bundle)
            }


            intent?.getStringExtra(KEY_ES_LINK)?.let { link ->
                // TODO: Handle deepling here

                return
            }

            getAppLaunchIntent()?.let(context::startActivity)
        } catch (t: Throwable) {
            Logger.e(TAG, "onReceive() ", t)
        }
    }

    private fun getAppLaunchIntent(): Intent? {
        val context = RetenoImpl.application
        return context.packageManager.getLaunchIntentForPackage(context.packageName)
    }



    companion object {
        val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}