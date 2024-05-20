package com.reteno.core

import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.RetenoLogEvent

internal interface RetenoInternalFacade {
    /**
     * Method invoked by SDK itself when Firebase token changes
     * */
    fun onNewFcmToken(token: String)

    /**
     * Method invoked by SDK itself to record interaction
     * */
    fun recordInteraction(id: String, status: InteractionStatus)

    fun canPresentMessages(): Boolean

    fun isDatabaseEmpty(): Boolean

    fun getDeviceId(): String

    fun logRetenoEvent(event: RetenoLogEvent)

    fun initializeIamView(interactionId: String)

    fun saveDefaultNotificationChannel(channel: String)

    fun getDefaultNotificationChannel(): String

    fun startScheduler()

    fun notificationsEnabled(enabled: Boolean)

    fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String)
}