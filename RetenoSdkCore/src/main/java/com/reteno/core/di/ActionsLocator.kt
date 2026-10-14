package com.reteno.core.di

import com.reteno.core.RetenoConfig
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
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.deeplink.TrackDeeplinkClickAction
import com.reteno.core.internal.action.device.ContactSession
import com.reteno.core.internal.action.device.CurrentDevice
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSessionAction
import com.reteno.core.internal.action.device.RegisterDeviceAction
import com.reteno.core.internal.action.device.UpdateFcmTokenAction
import com.reteno.core.internal.action.device.UpdateNotificationsEnabledAction
import com.reteno.core.internal.action.event.LogEcomEventAction
import com.reteno.core.internal.action.event.LogEventAction
import com.reteno.core.internal.action.event.LogRetenoEventAction
import com.reteno.core.internal.action.event.LogScreenViewEventAction
import com.reteno.core.internal.action.inbox.GetInboxMessagesAction
import com.reteno.core.internal.action.inbox.GetInboxMessagesCountAction
import com.reteno.core.internal.action.inbox.InboxCountPoller
import com.reteno.core.internal.action.inbox.MarkAllInboxMessagesOpenedAction
import com.reteno.core.internal.action.inbox.MarkInboxMessageOpenedAction
import com.reteno.core.internal.action.init.ApplyConfigAction
import com.reteno.core.internal.action.init.BootSdkAction
import com.reteno.core.internal.action.init.SetSessionDurationAction
import com.reteno.core.internal.action.init.StartSdkAction
import com.reteno.core.internal.action.init.StopSdkAction
import com.reteno.core.internal.action.interaction.ProcessCachedInteractionsAction
import com.reteno.core.internal.action.interaction.RecordInAppInteractionAction
import com.reteno.core.internal.action.interaction.RecordInteractionAction
import com.reteno.core.internal.action.interaction.RecordInteractionClickAction
import com.reteno.core.internal.action.interaction.RecordPushInteractionAction
import com.reteno.core.internal.action.lifecycle.AppBackgroundedAction
import com.reteno.core.internal.action.lifecycle.AppForegroundedAction
import com.reteno.core.internal.action.lifecycle.EnableAutoScreenTrackingAction
import com.reteno.core.internal.action.lifecycle.SetLifecycleEventConfigAction
import com.reteno.core.internal.action.lifecycle.TrackPermissionDialogAction
import com.reteno.core.internal.action.lifecycle.UpdatePushPermissionStatusAction
import com.reteno.core.internal.action.push.InitializeIamFromPushAction
import com.reteno.core.internal.action.push.SaveDefaultNotificationChannelAction
import com.reteno.core.internal.action.recommendation.FetchRecommendationAction
import com.reteno.core.internal.action.recommendation.FetchRecommendationJsonAction
import com.reteno.core.internal.action.recommendation.LogRecommendationEventsAction
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncAction
import com.reteno.core.internal.action.sync.ClearOldDataAction
import com.reteno.core.internal.action.sync.DataSync
import com.reteno.core.internal.action.sync.PushDeviceStep
import com.reteno.core.internal.action.sync.PushEventsStep
import com.reteno.core.internal.action.sync.PushInAppInteractionsStep
import com.reteno.core.internal.action.sync.PushInboxStatusStep
import com.reteno.core.internal.action.sync.PushInteractionsStep
import com.reteno.core.internal.action.sync.PushLogEventsStep
import com.reteno.core.internal.action.sync.PushRecomEventsStep
import com.reteno.core.internal.action.sync.PushUserStep
import com.reteno.core.internal.action.sync.PushWrappedLinksStep
import com.reteno.core.internal.action.sync.SyncCoordinator
import com.reteno.core.internal.action.sync.SyncDataAction
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.RemoteLogFailureReporter
import com.reteno.core.internal.action.user.ContactWriter
import com.reteno.core.internal.action.user.SegmentationRefreshImpl
import com.reteno.core.internal.action.user.SetAnonymousUserAttributesAction
import com.reteno.core.internal.action.user.SetMultiAccountUserAttributesAction
import com.reteno.core.internal.action.user.SetUserAttributesAction
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.lifecycle.AppForegroundState
import com.reteno.core.internal.lifecycle.AppLifecycleTracking
import com.reteno.core.internal.lifecycle.AppLifecycleTrackingImpl
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.LifecycleEvents
import com.reteno.core.internal.lifecycle.ScreenTracker
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.internal.lifecycle.StartStopSequencer
import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.random.Random

internal class ActionsLocator(
    private val serviceLocator: ServiceLocator,
    private val dispatchers: RetenoDispatchers,
    private val logSink: RetenoLogSink,
    private val scope: CoroutineScope,
    private val initGate: InitGate,
    private val configuration: SdkConfiguration,
    private val platform: AppPlatform,
    private val lifecycleEventFilter: LifecycleEventFilter,
    private val remoteLog: RemoteLog,
    private val clock: () -> Long
) : SdkActions {

    private val runner = ActionRunner(logSink, RemoteLogFailureReporter(remoteLog))
    private val eventBus = EventBus()
    private val mainScope = scope + dispatchers.main
    private val contactSession = ContactSession()
    private val runState = SdkRunState()
    private val appForegroundState = AppForegroundState()

    val contactQueue = SerialQueue(scope, "Contact", logSink)
    private val lifecycleQueue = SerialQueue(scope, "Lifecycle", logSink)
    private val initQueue = SerialQueue(mainScope, "Init", logSink)
    private val recommendationEventsQueue = SerialQueue(scope, "RecommendationEvents", logSink)

    private val api by lazy { SuspendApiClient(serviceLocator.restClientProvider.get(), dispatchers) }
    private val configRepository by lazy { serviceLocator.configRepositoryProvider.get() }
    private val restConfig by lazy { serviceLocator.restConfigProvider.get() }
    private val prefs by lazy { serviceLocator.sharedPrefsManagerProvider.get() }
    private val sessionHandler by lazy { serviceLocator.retenoSessionHandlerProvider.get() }
    private val iamController by lazy { serviceLocator.iamControllerProvider.get() }
    private val iamView by lazy { serviceLocator.iamViewProvider.get() }
    private val eventsDb by lazy { serviceLocator.retenoDatabaseManagerEventsProvider.get() }
    private val devicesDb by lazy { serviceLocator.retenoDatabaseManagerDeviceProvider.get() }
    private val usersDb by lazy { serviceLocator.retenoDatabaseManagerUserProvider.get() }
    private val interactionsDb by lazy { serviceLocator.retenoDatabaseManagerInteractionProvider.get() }
    private val inAppInteractionsDb by lazy { serviceLocator.retenoDatabaseManagerInAppInteractionProvider.get() }
    private val wrappedLinksDb by lazy { serviceLocator.retenoDatabaseManagerWrappedLinksProvider.get() }
    private val inboxDb by lazy { serviceLocator.retenoDatabaseManagerAppInboxProvider.get() }
    private val recomEventsDb by lazy { serviceLocator.retenoDatabaseManagerRecomEventsProvider.get() }

    private val lifecycleEvents by lazy { LifecycleEvents(lifecycleEventFilter, logEvent) }
    private val screenTracker by lazy { ScreenTracker(logScreenView, scope) }
    private val currentDevice by lazy { CurrentDevice(configRepository, devicesDb, platform, dispatchers) }
    private val segmentationRefresh by lazy { SegmentationRefreshImpl(iamController, scope, runner) }
    private val sdk by lazy { StartStopSequencer(mainScope, initGate, startSdk, stopSdk, runState) }

    private val contactWriter by lazy {
        ContactWriter(
            configRepository = configRepository,
            usersDb = usersDb,
            currentDevice = currentDevice,
            sync = dataSync,
            session = contactSession,
            sdk = sdk,
            sessionHandler = sessionHandler,
            remoteLog = remoteLog,
            dispatchers = dispatchers
        )
    }

    override val isStarted: Boolean
        get() = runState.isStarted

    val registerDevice: RegisterDeviceAction by lazy {
        RegisterDeviceAction(configRepository, currentDevice, dataSync, contactSession, runner)
    }

    val ensureDeviceRegisteredThisSession: EnsureDeviceRegisteredThisSessionAction by lazy {
        EnsureDeviceRegisteredThisSessionAction(
            configRepository = configRepository,
            devicesDb = devicesDb,
            currentDevice = currentDevice,
            sync = dataSync,
            session = contactSession,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    val updateFcmToken: UpdateFcmTokenAction by lazy {
        UpdateFcmTokenAction(configRepository, currentDevice, dataSync, contactSession, runner)
    }

    val updateNotificationsEnabled: UpdateNotificationsEnabledAction by lazy {
        UpdateNotificationsEnabledAction(
            configRepository = configRepository,
            currentDevice = currentDevice,
            sync = dataSync,
            session = contactSession,
            lifecycleEvents = lifecycleEvents,
            runner = runner
        )
    }

    val setUserAttributes: SetUserAttributesAction by lazy {
        SetUserAttributesAction(contactWriter, sdk, segmentationRefresh, runner)
    }

    val setMultiAccountUserAttributes: SetMultiAccountUserAttributesAction by lazy {
        SetMultiAccountUserAttributesAction(contactWriter, sdk, segmentationRefresh, runner)
    }

    val setAnonymousUserAttributes: SetAnonymousUserAttributesAction by lazy {
        SetAnonymousUserAttributesAction(configRepository, usersDb, dataSync, remoteLog, dispatchers, runner)
    }

    val logEvent: LogEventAction by lazy {
        LogEventAction(eventsDb, restConfig, eventBus, dispatchers, runner)
    }

    val logEcomEvent: LogEcomEventAction by lazy {
        LogEcomEventAction(eventsDb, restConfig, eventBus, dispatchers, runner)
    }

    val logScreenView: LogScreenViewEventAction by lazy {
        LogScreenViewEventAction(logEvent)
    }

    val logRetenoEvent: LogRetenoEventAction by lazy {
        LogRetenoEventAction(serviceLocator.retenoDatabaseManagerLogEventProvider.get(), dispatchers, runner)
    }

    private val recordInteraction: RecordInteractionAction by lazy {
        RecordInteractionAction(interactionsDb, configRepository, dispatchers, Util::getCurrentTimeStamp, runner)
    }

    private val recordPushInteraction: RecordPushInteractionAction by lazy {
        RecordPushInteractionAction(interactionsDb, initGate, processCachedInteractions, dispatchers, runner)
    }

    private val recordInteractionClick: RecordInteractionClickAction by lazy {
        RecordInteractionClickAction(interactionsDb, dataSync, dispatchers, Util::getCurrentTimeStamp, runner)
    }

    private val recordInAppInteraction: RecordInAppInteractionAction by lazy {
        RecordInAppInteractionAction(inAppInteractionsDb, dataSync, dispatchers, runner)
    }

    private val processCachedInteractions: ProcessCachedInteractionsAction by lazy {
        ProcessCachedInteractionsAction(interactionsDb, recordInteraction, dataSync, dispatchers, runner)
    }

    private val trackDeeplinkClick: TrackDeeplinkClickAction by lazy {
        TrackDeeplinkClickAction(wrappedLinksDb, dataSync, dispatchers, runner)
    }

    private val initializeIamFromPush: InitializeIamFromPushAction by lazy {
        InitializeIamFromPushAction(iamView, dispatchers, runner)
    }

    private val saveDefaultNotificationChannel: SaveDefaultNotificationChannelAction by lazy {
        SaveDefaultNotificationChannelAction(configRepository, dispatchers, runner)
    }

    private val getInboxMessages: GetInboxMessagesAction by lazy {
        GetInboxMessagesAction(api, initGate, dispatchers, runner)
    }

    private val getInboxMessagesCount: GetInboxMessagesCountAction by lazy {
        GetInboxMessagesCountAction(api, initGate, dispatchers, runner)
    }

    private val inboxCountPoller: InboxCountPoller by lazy {
        InboxCountPoller(getInboxMessagesCount, scope, dispatchers)
    }

    private val markInboxMessageOpened: MarkInboxMessageOpenedAction by lazy {
        MarkInboxMessageOpenedAction(
            inboxDb = inboxDb,
            configRepository = configRepository,
            sync = dataSync,
            initGate = initGate,
            dispatchers = dispatchers,
            timestamp = Util::getCurrentTimeStamp,
            runner = runner
        )
    }

    private val markAllInboxMessagesOpened: MarkAllInboxMessagesOpenedAction by lazy {
        MarkAllInboxMessagesOpenedAction(api, inboxDb, inboxCountPoller, initGate, dispatchers, runner)
    }

    private val fetchRecommendation: FetchRecommendationAction by lazy {
        FetchRecommendationAction(api, initGate, dispatchers, runner)
    }

    private val fetchRecommendationJson: FetchRecommendationJsonAction by lazy {
        FetchRecommendationJsonAction(api, initGate, dispatchers, runner)
    }

    private val logRecommendationEvents: LogRecommendationEventsAction by lazy {
        LogRecommendationEventsAction(recomEventsDb, dispatchers, runner)
    }

    private val appForegrounded: AppForegroundedAction by lazy {
        AppForegroundedAction(
            platform = platform,
            dataSync = dataSync,
            configRepository = configRepository,
            iamController = iamController,
            state = appForegroundState,
            lifecycleEvents = lifecycleEvents,
            clock = clock,
            runner = runner
        )
    }

    private val appBackgrounded: AppBackgroundedAction by lazy {
        AppBackgroundedAction(sessionHandler, appForegroundState, lifecycleEvents, runner)
    }

    private val trackPermissionDialog: TrackPermissionDialogAction by lazy {
        TrackPermissionDialogAction(platform, lifecycleEvents, runner)
    }

    val lifecycleTracking: AppLifecycleTracking by lazy {
        AppLifecycleTrackingImpl(
            platform = platform,
            appForegrounded = appForegrounded,
            appBackgrounded = appBackgrounded,
            trackPermissionDialog = trackPermissionDialog,
            queue = lifecycleQueue,
            dispatchers = dispatchers
        )
    }

    val setLifecycleEventConfig: SetLifecycleEventConfigAction by lazy {
        SetLifecycleEventConfigAction(lifecycleEventFilter, runner)
    }

    val enableAutoScreenTracking: EnableAutoScreenTrackingAction by lazy {
        EnableAutoScreenTrackingAction(screenTracker, platform, runner)
    }

    val updatePushPermissionStatus: UpdatePushPermissionStatusAction by lazy {
        UpdatePushPermissionStatusAction(platform, runner)
    }

    private val applyConfig: ApplyConfigAction by lazy {
        ApplyConfigAction(
            configuration = configuration,
            restConfig = restConfig,
            configRepository = configRepository,
            prefs = prefs,
            sessionHandler = sessionHandler,
            registerDevice = registerDevice,
            initGate = initGate,
            iamController = iamController,
            iamView = iamView,
            lifecycleTracking = lifecycleTracking,
            processCachedInteractions = processCachedInteractions,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    private val bootSdk: BootSdkAction by lazy {
        BootSdkAction(platform, prefs, restConfig, applyConfig, dispatchers, runner)
    }

    private val startSdk: StartSdkAction by lazy {
        StartSdkAction(
            ensureDeviceRegistered = ensureDeviceRegisteredThisSession,
            sessionHandler = sessionHandler,
            iamController = iamController,
            dataSync = dataSync,
            iamView = iamView,
            inboxCount = inboxCountPoller,
            state = runState,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    private val stopSdk: StopSdkAction by lazy {
        StopSdkAction(runState, sessionHandler, dataSync, iamView, inboxCountPoller, dispatchers, runner)
    }

    val setSessionDuration: SetSessionDurationAction by lazy {
        SetSessionDurationAction(sessionHandler, runner)
    }

    val clearOldData: ClearOldDataAction by lazy {
        ClearOldDataAction(
            interactionsDb = interactionsDb,
            inAppInteractionsDb = inAppInteractionsDb,
            eventsDb = eventsDb,
            inboxDb = inboxDb,
            recomEventsDb = recomEventsDb,
            wrappedLinksDb = wrappedLinksDb,
            configuration = configuration,
            remoteLog = remoteLog,
            clock = clock,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    val dataSync: DataSync by lazy {
        DataSync(
            coordinator = SyncCoordinator(syncData, scope, initGate),
            clearOldDataAction = clearOldData,
            platform = platform,
            configuration = configuration,
            jitter = { Random.nextLong(DataSync.RANDOM_DELAY) },
            scope = scope,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    private val syncData: SyncDataAction by lazy {
        SyncDataAction(
            contactSteps = listOf(
                PushDeviceStep(devicesDb, api, configRepository, dispatchers, runner),
                PushUserStep(usersDb, api, dispatchers, runner)
            ),
            dataSteps = listOf(
                PushInteractionsStep(interactionsDb, api, dispatchers, runner),
                PushInAppInteractionsStep(inAppInteractionsDb, api, dispatchers, runner),
                PushEventsStep(eventsDb, api, dispatchers, runner),
                PushInboxStatusStep(inboxDb, api, inboxCountPoller, dispatchers, runner),
                PushRecomEventsStep(recomEventsDb, api, dispatchers, runner),
                PushWrappedLinksStep(wrappedLinksDb, api, dispatchers, runner),
                PushLogEventsStep(serviceLocator.retenoDatabaseManagerLogEventProvider.get(), api, dispatchers, runner)
            ),
            runner = runner
        )
    }

    override val backgroundSync: BackgroundSync by lazy {
        BackgroundSyncAction(
            activityHelper = serviceLocator.retenoActivityHelperProvider.get(),
            initGate = initGate,
            databaseManager = serviceLocator.retenoDatabaseManagerProvider.get(),
            sync = dataSync,
            platform = platform,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    override fun boot() {
        initQueue.enqueue { bootSdk() }
    }

    override fun applyUserConfig(config: RetenoConfig) {
        configuration.useUserConfig(config)
        initQueue.enqueue { applyConfig(config, byUser = true) }
    }

    override fun requestStart() {
        sdk.start()
    }

    override fun requestStop() {
        sdk.stop()
    }

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        contactQueue.enqueue { setUserAttributes(externalUserId, user) }
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        contactQueue.enqueue { setMultiAccountUserAttributes(externalUserId, user) }
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        contactQueue.enqueue { setAnonymousUserAttributes(attributes) }
    }

    override fun submitFcmToken(token: String) {
        contactQueue.enqueue { updateFcmToken(token) }
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        contactQueue.enqueue { updateNotificationsEnabled(enabled) }
    }

    override fun submitEvent(event: Event) {
        scope.launch { logEvent(event) }
    }

    override fun submitEcomEvent(event: EcomEvent) {
        scope.launch { logEcomEvent(event) }
    }

    override fun submitScreenView(screenName: String) {
        scope.launch { logScreenView(screenName) }
    }

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        scope.launch { logRetenoEvent(event) }
    }

    override fun submitLifecycleEventConfig(options: LifecycleTrackingOptions) {
        scope.launch { setLifecycleEventConfig(options) }
    }

    override fun submitAutoScreenTracking(config: ScreenTrackingConfig) {
        mainScope.launch { enableAutoScreenTracking(config) }
    }

    override fun submitPushPermissionStatus() {
        scope.launch(dispatchers.io) { updatePushPermissionStatus() }
    }

    override fun submitSessionDuration(durationMillis: Long) {
        scope.launch { setSessionDuration(durationMillis) }
    }

    override fun requestSync() {
        dataSync.request()
    }

    override fun submitPushInteraction(interactionId: String, status: InteractionStatus) {
        scope.launch { recordPushInteraction(interactionId, status) }
    }

    override fun submitInteractionClick(interactionId: String, action: InteractionAction, forcePush: Boolean) {
        scope.launch { recordInteractionClick(interactionId, action, forcePush) }
    }

    override fun submitInAppInteraction(interaction: InAppInteraction) {
        scope.launch { recordInAppInteraction(interaction) }
    }

    override fun submitDeeplinkClick(wrappedLink: String) {
        scope.launch { trackDeeplinkClick(wrappedLink) }
    }

    override fun submitIamFromPush(interactionId: String) {
        scope.launch { initializeIamFromPush(interactionId) }
    }

    override fun submitDefaultNotificationChannel(channel: String) {
        scope.launch { saveDefaultNotificationChannel(channel) }
    }

    override fun submitInboxMessagesRequest(
        page: Int?,
        pageSize: Int?,
        status: AppInboxStatus?,
        callback: RetenoResultCallback<AppInboxMessages>
    ) {
        scope.launch { getInboxMessages(page, pageSize, status, callback) }
    }

    override fun submitInboxCountRequest(callback: RetenoResultCallback<Int>) {
        scope.launch { getInboxMessagesCount(callback) }
    }

    override fun subscribeInboxCount(callback: RetenoResultCallback<Int>) {
        inboxCountPoller.subscribe(callback)
    }

    override fun unsubscribeInboxCount(callback: RetenoResultCallback<Int>) {
        inboxCountPoller.unsubscribe(callback)
    }

    override fun unsubscribeAllInboxCount() {
        inboxCountPoller.unsubscribeAll()
    }

    override fun submitInboxMessageOpened(messageId: String) {
        scope.launch { markInboxMessageOpened(messageId) }
    }

    override fun submitAllInboxMessagesOpened(callback: RetenoResultCallback<Unit>) {
        scope.launch { markAllInboxMessagesOpened(callback) }
    }

    override fun <T : RecomBase> submitRecommendationRequest(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        scope.launch { fetchRecommendation(recomVariantId, requestBody, responseClass, callback) }
    }

    override fun submitRecommendationJsonRequest(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ) {
        scope.launch { fetchRecommendationJson(recomVariantId, requestBody, callback) }
    }

    override fun submitRecommendationEvents(events: RecomEvents) {
        recommendationEventsQueue.enqueue { logRecommendationEvents(events) }
    }
}
