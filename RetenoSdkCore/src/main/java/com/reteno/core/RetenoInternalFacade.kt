package com.reteno.core

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.LifecycleObserver
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.permission.AndroidPermissionChecker
import com.reteno.core.util.Procedure

/**
 * SDK operations used by the Reteno Push and FCM modules.
 */
@InternalRetenoApi
interface RetenoInternalFacade : LifecycleObserver {
    val application: Application

    /**
     * Method invoked by SDK itself when Firebase token changes
     * */
    fun onNewFcmToken(token: String)

    /**
     * Method invoked by SDK itself to record interaction
     * */
    fun recordInteraction(id: String, status: InteractionStatus, forcePush: Boolean = false)

    /**
     * Method invoked by SDK itself to record interaction
     * */
    fun recordInteractionAction(id: String, action: InteractionAction, forcePush: Boolean = false)

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

    fun getDefaultNotificationChannelConfig(): Procedure<NotificationChannelCompat.Builder>?

    fun executeAfterInit(action: () -> Unit)

    fun setSessionDuration(duration: Long)

    fun updatePushPermissionStatus()

    suspend fun requestPermissionChecker(): AndroidPermissionChecker?
}
