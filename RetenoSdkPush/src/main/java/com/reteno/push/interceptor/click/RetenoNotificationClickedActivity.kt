package com.reteno.push.interceptor.click

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ACTION_BUTTON
import com.reteno.push.Util
import com.reteno.push.Util.closeNotification

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
    private val scheduleController by lazy {
        reteno.serviceLocator.scheduleControllerProvider.get()
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
            scheduleController.forcePush()
        }
    }

    private fun handleIntent(intent: Intent?) {
        try {
            intent?.extras?.let { bundle ->
                if (bundle.getBoolean(KEY_ACTION_BUTTON, false)) {
                    val notificationId = bundle.getInt(Constants.KEY_NOTIFICATION_ID, -1)
                    closeNotification(this, notificationId)
                }
                bundle.remove(Constants.KEY_NOTIFICATION_ID)

                Util.tryToSendToCustomReceiverNotificationClicked(bundle)

                IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                    val (linkWrapped, linkUnwrapped) = Util.getLinkFromBundle(bundle)
                    deeplinkController.triggerDeeplinkClicked(linkWrapped, linkUnwrapped)
                    launchDeeplink(deeplinkIntent)
                } ?: launchApp(intent)
            } ?: launchApp(intent)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "handleIntent() ", t)
            /*@formatter:on*/
        }
    }

    private fun launchDeeplink(deeplinkIntent: Intent) {
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
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(this)
        if (intent == null || launchIntent == null) {
            return
        }

        intent.component = launchIntent.component
        this.startActivity(intent)
        finish()
    }

    companion object {
        private val TAG: String = RetenoNotificationClickedActivity::class.java.simpleName
    }
}