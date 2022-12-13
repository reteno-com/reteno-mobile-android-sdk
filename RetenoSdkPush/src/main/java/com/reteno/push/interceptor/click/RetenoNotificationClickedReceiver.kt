package com.reteno.push.interceptor.click

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ACTION_BUTTON
import com.reteno.push.Util
import com.reteno.push.Util.closeNotification

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

    private val scheduleController by lazy {
        reteno.serviceLocator.scheduleControllerProvider.get()
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
            scheduleController.forcePush()
        }
    }

    private fun handleIntent(context: Context, intent: Intent?) {
        try {
            intent?.extras?.let { bundle ->
                if (bundle.getBoolean(KEY_ACTION_BUTTON, false)) {
                    val notificationId = bundle.getInt(Constants.KEY_NOTIFICATION_ID, -1)
                    closeNotification(context, notificationId)
                }
                bundle.remove(Constants.KEY_NOTIFICATION_ID)

                Util.tryToSendToCustomReceiverNotificationClicked(bundle)

                IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                    val (linkWrapped, linkUnwrapped) = Util.getLinkFromBundle(bundle)

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
        try {
            context.startActivity(deeplinkIntent)
        } catch (ex: ActivityNotFoundException) {
            /*@formatter:off*/ Logger.i(TAG, "launchDeeplink(): ", "deeplinkIntent = [" , deeplinkIntent , "], exception = [", ex.message, "]")
            /*@formatter:on*/
            launchApp(context, deeplinkIntent)
        }
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
        private val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}