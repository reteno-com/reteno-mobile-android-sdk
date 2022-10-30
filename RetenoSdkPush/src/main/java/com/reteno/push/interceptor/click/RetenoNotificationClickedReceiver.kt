package com.reteno.push.interceptor.click

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_LINK_WRAPPED
import com.reteno.push.Util

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

    private val deeplinkController by lazy {
        reteno.serviceLocator.deeplinkControllerProvider.get()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "notification clicked. Context = [" , context , "], intent.extras = [" , intent?.extras.toStringVerbose() , "]")
        /*@formatter:on*/
        sendInteractionStatus(intent)
        handleIntent(context, intent)
    }

    private fun sendInteractionStatus(intent: Intent?) {
        intent?.extras?.getString(Constants.KEY_ES_INTERACTION_ID)?.let { interactionId ->
            interactionController.onInteraction(interactionId, InteractionStatus.OPENED)
        }
    }

    private fun handleIntent(context: Context, intent: Intent?) {
        try {
            intent?.extras?.let { bundle ->
                Util.tryToSendToCustomReceiverNotificationClicked(bundle)

                IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                    val linkWrapped = deeplinkIntent.getStringExtra(KEY_ES_LINK_WRAPPED).orEmpty()
                    val linkUnwrapped = deeplinkIntent.getStringExtra(KEY_ES_LINK_UNWRAPPED).orEmpty()
                    deeplinkController.triggerDeeplinkClicked(linkWrapped, linkUnwrapped)
                    launchDeeplink(context, deeplinkIntent)
                } ?: launchApp(context, intent)
            } ?: launchApp(context, intent)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onReceive() ", t)
            /*@formatter:on*/
        }
    }

    private fun launchDeeplink(context: Context, deeplinkIntent: Intent) {
        IntentHandler.resolveIntentActivity(context, deeplinkIntent)
        context.startActivity(deeplinkIntent)
    }

    private fun launchApp(context: Context, intent: Intent?) {
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(context)
        if (intent == null || launchIntent == null) {
            return
        }

        intent.extras?.let(launchIntent::putExtras)
        context.startActivity(launchIntent)
    }

    companion object {
        val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}