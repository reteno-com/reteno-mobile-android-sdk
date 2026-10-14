package com.reteno.core.di

import com.reteno.core.RetenoConfig
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.lifecycle.ScreenTrackingConfig

internal interface SdkActions : InteractionSubmitter, InboxActions {
    val isStarted: Boolean

    fun boot()

    fun applyUserConfig(config: RetenoConfig)

    fun requestStart()

    fun requestStop()

    fun submitUserAttributes(externalUserId: String, user: User?)

    fun submitMultiAccountUserAttributes(externalUserId: String, user: User?)

    fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous)

    fun submitFcmToken(token: String)

    fun submitNotificationsEnabled(enabled: Boolean)

    fun submitEvent(event: Event)

    fun submitEcomEvent(event: EcomEvent)

    fun submitScreenView(screenName: String)

    fun submitRetenoLogEvent(event: RetenoLogEvent)

    fun submitLifecycleEventConfig(options: LifecycleTrackingOptions)

    fun submitAutoScreenTracking(config: ScreenTrackingConfig)

    fun submitPushPermissionStatus()

    fun submitSessionDuration(durationMillis: Long)

    fun requestSync()

    fun submitPushInteraction(interactionId: String, status: InteractionStatus)

    fun submitDeeplinkClick(wrappedLink: String)

    fun submitIamFromPush(interactionId: String)

    fun submitDefaultNotificationChannel(channel: String)

    val backgroundSync: BackgroundSync
}
