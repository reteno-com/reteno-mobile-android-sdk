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
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class UnsupportedOsActions(private val configuration: SdkConfiguration) : SdkActions {

    override val isStarted: Boolean = false

    override val isInitialized: Boolean = false

    override val deviceId: String = ""

    override val defaultNotificationChannel: String = ""

    override val backgroundSync: BackgroundSync = NothingToSync

    override fun canPresentMessages(): Boolean = false

    override fun isActivityPresented(): Boolean = false

    override fun hasDataForSync(): Boolean = true

    override fun runAfterInit(operation: () -> Unit) = Unit

    override fun boot() = Unit

    override fun applyUserConfig(config: RetenoConfig) {
        configuration.useUserConfig(config)
    }

    override fun requestStart() = Unit

    override fun requestStop() = Unit

    override fun submitUserAttributes(externalUserId: String, user: User?) = Unit

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) = Unit

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) = Unit

    override fun submitFcmToken(token: String) = Unit

    override fun submitNotificationsEnabled(enabled: Boolean) = Unit

    override fun submitEvent(event: Event) = Unit

    override fun submitEcomEvent(event: EcomEvent) = Unit

    override fun submitScreenView(screenName: String) = Unit

    override fun submitRetenoLogEvent(event: RetenoLogEvent) = Unit

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) = Unit

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) = Unit

    override fun submitPushPermissionStatus() = Unit

    override fun submitSessionDuration(durationMillis: Long) = Unit

    override fun requestSync() = Unit

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) = Unit

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) = Unit

    override fun submitInAppInteraction(interaction: InAppInteraction) = Unit

    override fun submitInAppShown(message: InAppMessage) = Unit

    override fun submitDeeplinkClick(wrappedLink: String) = Unit

    override fun submitIamFromPush(interactionId: String) = Unit

    override fun submitDefaultNotificationChannel(channel: String) = Unit

    override fun submitInAppPause(paused: Boolean) = Unit

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) = Unit

    override fun submitPushInAppPause(paused: Boolean) = Unit

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) = Unit

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) = Unit

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

    private object NothingToSync : BackgroundSync {
        override suspend fun invoke(): Result<BackgroundSyncOutcome> = Result.success(BackgroundSyncOutcome.NO_DATA)
    }
}
