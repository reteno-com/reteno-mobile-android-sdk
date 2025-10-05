package com.reteno.push.interceptor.click

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported
import com.reteno.core.util.toStringVerbose
import com.reteno.push.Constants
import com.reteno.push.Constants.KEY_ACTION_BUTTON
import com.reteno.push.Util
import com.reteno.push.Util.closeNotification

class RetenoNotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "notification clicked. Context = [" , context , "], intent.extras = [" , intent?.extras.toStringVerbose() , "]")
        /*@formatter:on*/
        try {
            sendInteractionStatus(RetenoInternalImpl.instance, intent)
            handleIntent(context, RetenoInternalImpl.instance, intent)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "onReceive(): ", ex)
            /*@formatter:on*/
        }
    }

    private fun sendInteractionStatus(reteno: RetenoInternalImpl, intent: Intent?) {
        if (intent?.extras?.getString(Constants.KEY_ES_IAM) != "1") {

            intent?.extras?.getString(Constants.KEY_ES_INTERACTION_ID)?.let { interactionId ->
                /*@formatter:off*/ Logger.i(TAG, "sendInteractionStatus(): ", "intent = [", intent, "]")
                /*@formatter:on*/
                reteno.recordInteraction(interactionId, InteractionStatus.CLICKED)
                reteno.forcePushData()
            }
        }
    }

    private fun handleIntent(context: Context, reteno: RetenoInternalImpl, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "handleIntent(): ", "context = [", context, "], intent = [", intent, "]")
        /*@formatter:on*/
        intent?.extras?.let { bundle ->
            if (bundle.getBoolean(KEY_ACTION_BUTTON, false)) {
                val notificationId = bundle.getInt(Constants.KEY_NOTIFICATION_ID, -1)
                closeNotification(context, notificationId)
            }
            bundle.remove(Constants.KEY_NOTIFICATION_ID)

            Util.tryToSendToCustomReceiverNotificationClicked(bundle)

            IntentHandler.getDeepLinkIntent(bundle)?.let { deeplinkIntent ->
                val (linkWrapped, linkUnwrapped) = Util.getLinkFromBundle(bundle)
                reteno.deeplinkClicked(linkWrapped, linkUnwrapped)
                launchDeeplink(context, deeplinkIntent)
            } ?: launchApp(context, intent)
        } ?: launchApp(context, intent)
    }

    private fun launchDeeplink(context: Context, deeplinkIntent: Intent) {
        /*@formatter:off*/ Logger.i(TAG, "launchDeeplink(): ", "context = [", context, "], deeplinkIntent = [", deeplinkIntent, "]")
        /*@formatter:on*/
        try {
            context.startActivity(deeplinkIntent)
        } catch (ex: ActivityNotFoundException) {
            /*@formatter:off*/ Logger.i(TAG, "launchDeeplink(): ", "deeplinkIntent = [" , deeplinkIntent , "], exception = [", ex.message, "]")
            /*@formatter:on*/
            launchApp(context, deeplinkIntent)
        }
    }

    private fun launchApp(context: Context, intent: Intent?) {
        /*@formatter:off*/ Logger.i(TAG, "launchApp(): ", "context = [", context, "], intent = [", intent, "]")
        /*@formatter:on*/
        val launchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(context)
        if (intent == null || launchIntent == null) {
            return
        }
        intent.extras?.let(launchIntent::putExtras)
        val isIam = intent.extras?.let(::checkIam)?:false
        when {
            isIam && RetenoInternalImpl.instance.isActivityPresented() -> {}
            else -> {
                context.startActivity(launchIntent)
            }
        }
    }

    private fun checkIam(bundle: Bundle): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "RetenoNotificationClickedReceiver.class: checkIam(): ", "bundle = [", bundle, "]")
        /*@formatter:on*/
        bundle.getString(Constants.KEY_ES_IAM)
            .takeIf { it == "1" }
            ?.run {
                bundle.getString(Constants.KEY_ES_INTERACTION_ID)?.let(RetenoInternalImpl.instance::initializeIamView)
                return true
            }
        return false
    }

    companion object {
        private val TAG: String = RetenoNotificationClickedReceiver::class.java.simpleName
    }
}