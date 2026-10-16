package com.reteno.core.fixtures

import com.reteno.core.RetenoConfig
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.di.SdkActions
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
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class FakeSdkActions : SdkActions {
    val calls = mutableListOf<String>()
    val configs = mutableListOf<RetenoConfig>()
    override var isStarted: Boolean = false
    override var isInitialized: Boolean = false
        private set
    private val waitingForInit = mutableListOf<() -> Unit>()
    var failure: Throwable? = null
    val users = mutableListOf<User?>()
    val multiAccountUsers = mutableListOf<User?>()
    val anonymousAttributes = mutableListOf<UserAttributesAnonymous>()
    val ecomEvents = mutableListOf<EcomEvent>()
    val screenTrackingConfigs = mutableListOf<ScreenTrackingConfig>()
    val recomEvents = mutableListOf<RecomEvents>()
    val callbacks = mutableListOf<Any>()
    override var deviceId: String = ""
    override var defaultNotificationChannel: String = ""
    var canPresent = false
    var activityPresented = false
    var dataForSync = false

    override fun canPresentMessages(): Boolean = canPresent

    override fun isActivityPresented(): Boolean = activityPresented

    override fun hasDataForSync(): Boolean = dataForSync

    override fun runAfterInit(operation: () -> Unit) {
        if (isInitialized) {
            operation()
            return
        }
        waitingForInit += operation
    }

    fun markInitialized() {
        isInitialized = true
        waitingForInit.forEach { it() }
        waitingForInit.clear()
    }

    override fun boot() {
        record("boot")
    }

    override fun applyUserConfig(config: RetenoConfig) {
        configs += config
        record("applyUserConfig")
    }

    override fun requestStart() {
        record("requestStart")
    }

    override fun requestStop() {
        record("requestStop")
    }

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        record("submitUserAttributes $externalUserId")
        users += user
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        record("submitMultiAccountUserAttributes $externalUserId")
        multiAccountUsers += user
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        record("submitAnonymousUserAttributes")
        anonymousAttributes += attributes
    }

    override fun submitFcmToken(token: String) {
        record("submitFcmToken")
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        record("submitNotificationsEnabled $enabled")
    }

    override fun submitEvent(event: Event) {
        record("submitEvent ${event.eventTypeKey}")
    }

    override fun submitEcomEvent(event: EcomEvent) {
        record("submitEcomEvent")
        ecomEvents += event
    }

    override fun submitScreenView(screenName: String) {
        record("submitScreenView $screenName")
    }

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        record("submitRetenoLogEvent")
    }

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        record("submitLifecycleEventConfig")
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        record("submitAutoScreenTracking")
        screenTrackingConfigs += config
    }

    override fun submitPushPermissionStatus() {
        record("submitPushPermissionStatus")
    }

    override fun submitSessionDuration(durationMillis: Long) {
        record("submitSessionDuration $durationMillis")
    }

    override fun requestSync() {
        record("requestSync")
    }

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) {
        record("submitPushInteraction $interactionId $status")
    }

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) {
        record("submitInteractionClick $interactionId ${action.type} $forcePush")
    }

    override fun submitInAppInteraction(interaction: InAppInteraction) {
        record("submitInAppInteraction ${interaction.interactionId}")
    }

    override fun submitDeeplinkClick(wrappedLink: String) {
        record("submitDeeplinkClick $wrappedLink")
    }

    override fun submitIamFromPush(interactionId: String) {
        record("submitIamFromPush $interactionId")
    }

    override fun submitDefaultNotificationChannel(channel: String) {
        record("submitDefaultNotificationChannel $channel")
    }

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        record("submitInboxMessagesRequest $page $pageSize $status")
        callbacks += callback
    }

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) {
        record("submitInboxCountRequest")
        callbacks += callback
    }

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) {
        record("subscribeInboxCount")
        callbacks += callback
    }

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) {
        record("unsubscribeInboxCount")
        callbacks += callback
    }

    override fun unsubscribeAllInboxCount() {
        record("unsubscribeAllInboxCount")
    }

    override fun submitInboxMessageOpened(messageId: String) {
        record("submitInboxMessageOpened $messageId")
    }

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) {
        record("submitAllInboxMessagesOpened")
        callbacks += callback
    }

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        record("submitRecommendationRequest $recomVariantId ${responseClass.simpleName} $requestBody")
        callbacks += callback
    }

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) {
        record("submitRecommendationJsonRequest $recomVariantId $requestBody")
        callbacks += callback
    }

    override fun submitRecommendationEvents(events: RecomEvents) {
        record("submitRecommendationEvents ${events.recomVariantId}")
        recomEvents += events
    }

    override fun submitInAppShown(message: InAppMessage) {
        record("submitInAppShown ${message.messageId}")
    }

    override fun submitInAppPause(paused: Boolean) {
        record("submitInAppPause $paused")
    }

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        record("submitInAppPauseBehaviour $behaviour")
    }

    override fun submitPushInAppPause(paused: Boolean) {
        record("submitPushInAppPause $paused")
    }

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        record("submitPushInAppPauseBehaviour $behaviour")
    }

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) {
        record("submitInAppLifecycleCallback ${callback != null}")
    }

    var backgroundSyncAnswer: BackgroundSync? = null

    override val backgroundSync: BackgroundSync
        get() = checkNotNull(backgroundSyncAnswer) { "background sync is not part of facade tests" }

    private fun record(call: String) {
        failure?.let { throw it }
        calls += call
    }
}
