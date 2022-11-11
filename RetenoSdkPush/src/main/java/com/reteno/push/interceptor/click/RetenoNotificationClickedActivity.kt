package com.reteno.push.interceptor.click

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ES_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_LINK_WRAPPED
import com.reteno.push.Util

class RetenoNotificationClickedActivity : Activity() {

    private val reteno by lazy {
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }
    private val interactionController by lazy {
        reteno.serviceLocator.interactionControllerProvider.get()
    }
    private val deeplinkController by lazy {
        reteno.serviceLocator.deeplinkControllerProvider.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "notification clicked, intent.extras = [" , intent.extras.toStringVerbose() , "]")
        /*@formatter:on*/

        sendInteractionStatus(intent)
        handleIntent(intent)

        finish()
    }

    private fun sendInteractionStatus(intent: Intent?) {
        intent?.extras?.getString(Constants.KEY_ES_INTERACTION_ID)?.let { interactionId ->
            interactionController.onInteraction(interactionId, InteractionStatus.OPENED)
        }
    }

    private fun handleIntent(intent: Intent?) {
        try {
            intent?.extras?.let { bundle ->
                Util.tryToSendToCustomReceiverNotificationClicked(bundle)

                IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                    val linkWrapped = deeplinkIntent.getStringExtra(KEY_ES_LINK_WRAPPED).orEmpty()
                    val linkUnwrapped = deeplinkIntent.getStringExtra(KEY_ES_LINK_UNWRAPPED).orEmpty()
                    deeplinkController.triggerDeeplinkClicked(linkWrapped, linkUnwrapped)
                    launchDeeplink(deeplinkIntent)
                } ?: launchApp(intent)
            } ?: launchApp(intent)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(RetenoNotificationClickedReceiver.TAG, "handleIntent() ", t)
            /*@formatter:on*/
        }
    }

    private fun launchDeeplink(deeplinkIntent: Intent) {
        IntentHandler.resolveIntentActivity(this, deeplinkIntent)
        this.startActivity(deeplinkIntent)
        finish()
    }

    private fun launchApp(intent: Intent?) {
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(this)
        if (intent == null || launchIntent == null) {
            return
        }

        intent.component = launchIntent.component
        this.startActivity(intent)
        finish()
    }

    companion object {
        val TAG: String = RetenoNotificationClickedActivity::class.java.simpleName
    }
}