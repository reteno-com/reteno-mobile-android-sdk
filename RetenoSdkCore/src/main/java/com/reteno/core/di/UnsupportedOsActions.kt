package com.reteno.core.di

import com.reteno.core.RetenoConfig
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.event.EventActions
import com.reteno.core.internal.action.iam.InAppActions
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.action.init.SdkControl
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.core.internal.action.lifecycle.LifecycleActions
import com.reteno.core.internal.action.push.PushActions
import com.reteno.core.internal.action.recommendation.RecommendationActions
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.internal.action.sync.SyncActions
import com.reteno.core.internal.action.user.ContactActions
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class UnsupportedOsActions(configuration: SdkConfiguration) : SdkActions {

    override val control: SdkControl = UnsupportedOsControl(configuration)

    override val queries: SdkQueries = UnsupportedOsQueries

    override val contact: ContactActions = UnsupportedOsContact

    override val events: EventActions = UnsupportedOsEvents

    override val lifecycle: LifecycleActions = UnsupportedOsLifecycle

    override val inApp: InAppActions = UnsupportedOsInApp

    override val interactions: InteractionSubmitter = UnsupportedOsInteractions

    override val push: PushActions = UnsupportedOsPush

    override val sync: SyncActions = UnsupportedOsSync

    override val inbox: InboxActions = UnsupportedOsInbox

    override val recommendation: RecommendationActions = UnsupportedOsRecommendation
}

private class UnsupportedOsControl(private val configuration: SdkConfiguration) : SdkControl {

    override val isStarted: Boolean = false

    override val isInitialized: Boolean = false

    override fun runAfterInit(operation: () -> Unit) = Unit

    override fun boot() = Unit

    override fun applyUserConfig(config: RetenoConfig) {
        configuration.useUserConfig(config)
    }

    override fun requestStart() = Unit

    override fun requestStop() = Unit

    override fun submitSessionDuration(durationMillis: Long) = Unit
}

private object UnsupportedOsQueries : SdkQueries {

    override val deviceId: String = ""

    override val defaultNotificationChannel: String = ""

    override fun canPresentMessages(): Boolean = false

    override fun isActivityPresented(): Boolean = false

    override fun hasDataForSync(): Boolean = true
}

private object UnsupportedOsContact : ContactActions {

    override fun submitUserAttributes(externalUserId: String, user: User?) = Unit

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) = Unit

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) = Unit

    override fun submitFcmToken(token: String) = Unit

    override fun submitNotificationsEnabled(enabled: Boolean) = Unit
}

private object UnsupportedOsEvents : EventActions {

    override fun submitEvent(event: Event) = Unit

    override fun submitEcomEvent(event: EcomEvent) = Unit

    override fun submitScreenView(screenName: String) = Unit
}

private object UnsupportedOsLifecycle : LifecycleActions {

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) = Unit

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) = Unit

    override fun submitPushPermissionStatus() = Unit
}

private object UnsupportedOsInApp : InAppActions {

    override fun submitInAppPause(paused: Boolean) = Unit

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) = Unit

    override fun submitPushInAppPause(paused: Boolean) = Unit

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) = Unit

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) = Unit
}

private object UnsupportedOsInteractions : InteractionSubmitter {

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) = Unit

    override fun submitInAppInteraction(interaction: InAppInteraction) = Unit

    override fun submitInAppShown(message: InAppMessage) = Unit
}

private object UnsupportedOsPush : PushActions {

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) = Unit

    override fun submitDeeplinkClick(wrappedLink: String) = Unit

    override fun submitIamFromPush(interactionId: String) = Unit

    override fun submitDefaultNotificationChannel(channel: String) = Unit
}

private object UnsupportedOsSync : SyncActions {

    override val backgroundSync: BackgroundSync = NothingToSync

    override fun requestSync() = Unit
}

private object NothingToSync : BackgroundSync {

    override suspend fun invoke(): Result<BackgroundSyncOutcome> = Result.success(BackgroundSyncOutcome.NO_DATA)
}

private object UnsupportedOsInbox : InboxActions {

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) = Unit

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) = Unit

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) = Unit

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) = Unit

    override fun unsubscribeAllInboxCount() = Unit

    override fun submitInboxMessageOpened(messageId: String) = Unit

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) = Unit
}

private object UnsupportedOsRecommendation : RecommendationActions {

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) = Unit

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) = Unit

    override fun submitRecommendationEvents(events: RecomEvents) = Unit
}
