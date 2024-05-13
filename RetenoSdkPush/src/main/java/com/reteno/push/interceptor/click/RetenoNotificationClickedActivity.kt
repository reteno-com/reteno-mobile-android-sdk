package com.reteno.push.interceptor.click

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ACTION_BUTTON
import com.reteno.push.Util
import com.reteno.push.Util.closeNotification

class RetenoNotificationClickedActivity : Activity() {

    private val reteno by lazy {
        ((RetenoImpl.application as RetenoApplication).getRetenoInstance() as RetenoImpl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "notification clicked, intent.extras = [" , intent.extras.toStringVerbose() , "]")
        /*@formatter:on*/

        try {
            sendInteractionStatus(intent)
            handleIntent(intent)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onCreate(): ", ex)
            /*@formatter:on*/
        }

        finish()
    }

    private fun sendInteractionStatus(intent: Intent?) {
        if (intent?.extras?.getString(Constants.KEY_ES_IAM) != "1") {
            intent?.extras?.getString(Constants.KEY_ES_INTERACTION_ID)?.let { interactionId ->
                /*@formatter:off*/ Logger.i(TAG, "sendInteractionStatus(): ", "intent = [", intent, "]")
                /*@formatter:on*/
                reteno.recordInteraction(interactionId, InteractionStatus.CLICKED)
                reteno.forcePushData()
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "handleIntent(): ", "intent = [", intent, "]")
        /*@formatter:on*/
        intent?.extras?.let { bundle ->
            if (bundle.getBoolean(KEY_ACTION_BUTTON, false)) {
                val notificationId = bundle.getInt(Constants.KEY_NOTIFICATION_ID, -1)
                closeNotification(this, notificationId)
            }
            bundle.remove(Constants.KEY_NOTIFICATION_ID)

            Util.tryToSendToCustomReceiverNotificationClicked(bundle)

            IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                val (linkWrapped, linkUnwrapped) = Util.getLinkFromBundle(bundle)
                RetenoImpl.instance.deeplinkClicked(linkWrapped, linkUnwrapped)
                launchDeeplink(deeplinkIntent)
            } ?: launchApp(intent)
        } ?: launchApp(intent)
    }

    private fun launchDeeplink(deeplinkIntent: Intent) {
        /*@formatter:off*/ Logger.i(TAG, "launchDeeplink(): ", "deeplinkIntent = [", deeplinkIntent, "]")
        /*@formatter:on*/
        try {
            startActivity(deeplinkIntent)
        } catch (ex: ActivityNotFoundException) {
            /*@formatter:off*/ Logger.i(TAG, "launchDeeplink(): ", "deeplinkIntent = [" , deeplinkIntent , "], exception = [", ex.message, "]")
            /*@formatter:on*/
            launchApp(deeplinkIntent)
        }
        finish()
    }

    private fun launchApp(intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "launchApp(): ", "intent = [", intent, "]")
        /*@formatter:on*/
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(this)
        if (intent == null || launchIntent == null) {
            return
        }
        intent.extras?.let(::checkIam)

        intent.component = launchIntent.component
        this.startActivity(intent)
        finish()
    }

    private fun checkIam(bundle: Bundle) {
        /*@formatter:off*/ Logger.i(TAG, "RetenoNotificationClickedActivity.class: checkIam(): ", "bundle = [", bundle, "]")
        /*@formatter:on*/
        bundle.getString(Constants.KEY_ES_IAM)
            .takeIf { it == "1" }
            ?.run {
                bundle.getString(Constants.KEY_ES_INTERACTION_ID)?.let(RetenoImpl.instance::initializeIamView)
            }
    }

    companion object {
        private val TAG: String = RetenoNotificationClickedActivity::class.java.simpleName
    }
}