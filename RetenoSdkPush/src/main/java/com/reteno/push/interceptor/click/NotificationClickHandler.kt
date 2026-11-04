package com.reteno.push.interceptor.click

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.push.Constants
import com.reteno.push.Util

internal class NotificationClickHandler(private val context: Context, private val extras: Bundle) {

    private val facade get() = RetenoInternalEntryPoint.facade
    private val isActionButton = extras.getBoolean(Constants.KEY_ACTION_BUTTON, false)
    private val isInApp = extras.getString(Constants.KEY_ES_IAM) == IN_APP_FLAG

    fun recordClick() {
        if (isInApp) return
        val interactionId = extras.getString(Constants.KEY_ES_INTERACTION_ID) ?: return
        if (isActionButton) {
            val action = InteractionAction(
                type = OPEN_URL,
                targetComponentId = extras.getString(Constants.KEY_BTN_ACTION_ID, null),
                url = extras.getString(Constants.KEY_BTN_ACTION_LINK_UNWRAPPED, null)
            )
            facade.recordInteractionAction(interactionId, action, forcePush = true)
        } else {
            facade.recordInteraction(interactionId, InteractionStatus.CLICKED, forcePush = true)
        }
    }

    fun dismissAndNotifyHost() {
        if (isActionButton) Util.closeNotification(context, extras.getInt(Constants.KEY_NOTIFICATION_ID, -1))
        extras.remove(Constants.KEY_NOTIFICATION_ID)
        Util.tryToSendToCustomReceiverNotificationClicked(context, extras)
    }

    fun deeplinkIntent(): Intent? {
        val intent = IntentHandler.getDeepLinkIntent(extras) ?: return null
        val (linkWrapped, linkUnwrapped) = Util.getLinkFromBundle(extras)
        facade.deeplinkClicked(linkWrapped, linkUnwrapped)
        return intent
    }

    companion object {
        private const val IN_APP_FLAG = "1"
        private const val OPEN_URL = "OPEN_URL"

        fun shouldOpenApp(extras: Bundle?): Boolean {
            val bundle = extras ?: return true
            if (bundle.getString(Constants.KEY_ES_IAM) != IN_APP_FLAG) return true
            val facade = RetenoInternalEntryPoint.facade
            bundle.getString(Constants.KEY_ES_INTERACTION_ID)?.let(facade::initializeIamView)
            return !facade.isActivityPresented()
        }
    }
}
