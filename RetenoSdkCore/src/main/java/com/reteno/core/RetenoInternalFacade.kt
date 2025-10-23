package com.reteno.core

import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.LifecycleObserver
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.permission.AndroidPermissionChecker

internal interface RetenoInternalFacade : LifecycleObserver {
    /**
     * Method invoked by SDK itself when Firebase token changes
     * */
    fun onNewFcmToken(token: String)

    /**
     * Method invoked by SDK itself to record interaction
     * */
    fun recordInteraction(id: String, status: InteractionStatus, forcePush: Boolean = false)

    fun canPresentMessages(): Boolean

    fun hasDataForSync(): Boolean

    fun isActivityPresented(): Boolean

    fun getDeviceId(): String

    fun logRetenoEvent(event: RetenoLogEvent)

    fun initializeIamView(interactionId: String)

    fun saveDefaultNotificationChannel(channel: String)

    fun getDefaultNotificationChannel(): String

    fun notificationsEnabled(enabled: Boolean)

    fun deeplinkClicked(linkWrapped: String, linkUnwrapped: String)

    fun getDefaultNotificationChannelConfig(): ((NotificationChannelCompat.Builder) -> Unit)? = null

    fun executeAfterInit(action: () -> Unit)

    suspend fun requestPermissionChecker(): AndroidPermissionChecker?
}