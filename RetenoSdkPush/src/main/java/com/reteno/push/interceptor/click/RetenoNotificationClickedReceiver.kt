package com.reteno.push.interceptor.click

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_LINK
import com.reteno.push.Util
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose

class RetenoNotificationClickedReceiver : BroadcastReceiver() {

    // Don't move out of lazy delegate as Robolectric tests will fail
    // https://github.com/robolectric/robolectric/issues/4308
    private val reteno by lazy {
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }
    // Don't move out of lazy delegate as Robolectric tests will fail
    // https://github.com/robolectric/robolectric/issues/4308
    private val interactionController by lazy {
        reteno.serviceLocator.interactionControllerProvider.get()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "notification clicked. Context = [" , context , "], intent.extras = [" , intent?.extras.toStringVerbose() , "]")
        /*@formatter:on*/
        intent?.extras?.getString(Constants.KEY_ES_INTERACTION_ID)?.let { interactionId ->
            interactionController.onInteraction(interactionId, InteractionStatus.OPENED)
        }

        try {
            intent?.extras?.let { bundle ->
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