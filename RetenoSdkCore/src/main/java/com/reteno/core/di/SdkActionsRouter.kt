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
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

internal class SdkActionsRouter(
    private val graph: ActionGraph,
    private val infrastructure: SdkInfrastructure,
    private val scope: CoroutineScope,
    private val dispatchers: RetenoDispatchers,
    logSink: RetenoLogSink
) : SdkActions {

    private val mainScope = scope + dispatchers.main

    val contactQueue = SerialQueue(scope, "Contact", logSink)
    private val initQueue = SerialQueue(mainScope, "Init", logSink)
    private val inAppQueue = SerialQueue(scope, "InApp", logSink)
    private val recommendationEventsQueue = SerialQueue(scope, "RecommendationEvents", logSink)

    override val isStarted: Boolean
        get() = graph.init.runState.isStarted

    override val isInitialized: Boolean
        get() = graph.core.initGate.isInitialized

    override fun runAfterInit(operation: () -> Unit) {
        val initGate = graph.core.initGate
        if (initGate.isInitialized) {
            operation()
            return
        }
        mainScope.launch {
            initGate.awaitInit()
            operation()
        }
    }

    override val deviceId: String
        get() = infrastructure.configRepository.getDeviceId().id

    override val defaultNotificationChannel: String
        get() = infrastructure.configRepository.getDefaultNotificationChannel()

    override fun canPresentMessages(): Boolean = infrastructure.activityHelper.canPresentMessages()

    override fun isActivityPresented(): Boolean = infrastructure.activityHelper.currentActivity != null

    override fun hasDataForSync(): Boolean =
        try {
            infrastructure.db.databaseManager.hasDataForSync()
        } catch (error: Exception) {
            graph.core.remoteLog.recordError("Failed to check local database for unsynced data", error)
            true
        }

    override val backgroundSync: BackgroundSync
        get() = graph.sync.backgroundSync

    override fun boot() {
        graph.core.hostUi.trackActivities()
        graph.inApp.inAppTriggers.listen()
        initQueue.enqueue { graph.init.bootSdk() }
    }

    override fun applyUserConfig(config: RetenoConfig) {
        graph.core.configuration.useUserConfig(config)
        initQueue.enqueue { graph.init.applyConfig(config, byUser = true) }
    }

    override fun requestStart() {
        graph.init.sdk.start()
    }

    override fun requestStop() {
        graph.init.sdk.stop()
    }

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        contactQueue.enqueue { graph.user.setUserAttributes(externalUserId, user) }
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        contactQueue.enqueue { graph.user.setMultiAccountUserAttributes(externalUserId, user) }
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        contactQueue.enqueue { graph.user.setAnonymousUserAttributes(attributes) }
    }

    override fun submitFcmToken(token: String) {
        contactQueue.enqueue { graph.device.updateFcmToken(token) }
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        contactQueue.enqueue { graph.device.updateNotificationsEnabled(enabled) }
    }

    override fun submitEvent(event: Event) {
        scope.launch { graph.events.logEvent(event) }
    }

    override fun submitEcomEvent(event: EcomEvent) {
        scope.launch { graph.events.logEcomEvent(event) }
    }

    override fun submitScreenView(screenName: String) {
        scope.launch { graph.events.logScreenView(screenName) }
    }

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        scope.launch { graph.events.logRetenoEvent(event) }
    }

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        scope.launch { graph.lifecycle.setLifecycleEventConfig(options) }
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        mainScope.launch { graph.lifecycle.enableAutoScreenTracking(config) }
    }

    override fun submitPushPermissionStatus() {
        scope.launch(dispatchers.io) { graph.lifecycle.updatePushPermissionStatus() }
    }

    override fun submitSessionDuration(durationMillis: Long) {
        scope.launch { graph.init.setSessionDuration(durationMillis) }
    }

    override fun requestSync() {
        graph.sync.dataSync.request()
    }

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) {
        scope.launch { graph.interactions.recordPushInteraction(interactionId, status) }
    }

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) {
        scope.launch { graph.interactions.recordInteractionClick(interactionId, action, forcePush) }
    }

    override fun submitInAppInteraction(interaction: InAppInteraction) {
        scope.launch { graph.interactions.recordInAppInteraction(interaction) }
    }

    override fun submitInAppShown(message: InAppMessage) {
        scope.launch { graph.inApp.recordInAppShown(message) }
    }

    override fun submitInAppPause(paused: Boolean) {
        inAppQueue.enqueue { graph.inApp.pauseInApps(paused) }
    }

    override fun submitInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        inAppQueue.enqueue { graph.inApp.setInAppPauseBehaviour(behaviour) }
    }

    override fun submitPushInAppPause(paused: Boolean) {
        graph.inApp.inAppPresenter.pauseIncomingPushInApps(paused)
    }

    override fun submitPushInAppPauseBehaviour(behaviour: InAppPauseBehaviour) {
        graph.inApp.inAppPresenter.setPauseBehaviour(behaviour)
    }

    override fun submitInAppLifecycleCallback(callback: InAppLifecycleCallback?) {
        graph.inApp.inAppPresenter.setInAppLifecycleCallback(callback)
    }

    override fun submitDeeplinkClick(wrappedLink: String) {
        scope.launch { graph.deeplink.trackDeeplinkClick(wrappedLink) }
    }

    override fun submitIamFromPush(interactionId: String) {
        scope.launch { graph.push.initializeIamFromPush(interactionId) }
    }

    override fun submitDefaultNotificationChannel(channel: String) {
        scope.launch { graph.push.saveDefaultNotificationChannel(channel) }
    }

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        scope.launch { graph.inbox.getInboxMessages(page, pageSize, status, callback) }
    }

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) {
        scope.launch { graph.inbox.getInboxMessagesCount(callback) }
    }

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) {
        graph.inbox.inboxCountPoller.subscribe(callback)
    }

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) {
        graph.inbox.inboxCountPoller.unsubscribe(callback)
    }

    override fun unsubscribeAllInboxCount() {
        graph.inbox.inboxCountPoller.unsubscribeAll()
    }

    override fun submitInboxMessageOpened(messageId: String) {
        scope.launch { graph.inbox.markInboxMessageOpened(messageId) }
    }

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) {
        scope.launch { graph.inbox.markAllInboxMessagesOpened(callback) }
    }

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        scope.launch {
            graph.recommendation.fetchRecommendation(recomVariantId, requestBody, responseClass, callback)
        }
    }

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) {
        scope.launch { graph.recommendation.fetchRecommendationJson(recomVariantId, requestBody, callback) }
    }

    override fun submitRecommendationEvents(events: RecomEvents) {
        recommendationEventsQueue.enqueue { graph.recommendation.logRecommendationEvents(events) }
    }
}
