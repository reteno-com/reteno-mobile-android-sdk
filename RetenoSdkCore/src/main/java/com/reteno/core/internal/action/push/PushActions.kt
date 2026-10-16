package com.reteno.core.internal.action.push

import com.reteno.core.domain.model.interaction.InteractionStatus

internal interface PushActions {
    fun submitPushInteraction(interactionId: String, status: InteractionStatus)

    fun submitDeeplinkClick(wrappedLink: String)

    fun submitIamFromPush(interactionId: String)

    fun submitDefaultNotificationChannel(channel: String)
}
