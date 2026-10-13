package com.reteno.core.di

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.logevent.LogLevel
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
import com.reteno.core.internal.action.user.SegmentationRefresh
import com.reteno.core.internal.action.user.SetAnonymousUserAttributesAction
import com.reteno.core.internal.action.user.SetMultiAccountUserAttributesAction
import com.reteno.core.internal.action.user.SetUserAttributesAction
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.SerialQueue
import com.reteno.core.internal.coroutine.retenoScope
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.lifecycle.SdkLifecycle
import com.reteno.core.internal.log.LogPriority
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.internal.log.log
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.util.Util
import kotlin.random.Random

internal class ActionsLocator(
    private val serviceLocator: ServiceLocator,
    private val dispatchers: RetenoDispatchers,
    private val logSink: RetenoLogSink,
    private val awaitInit: suspend () -> Unit,
    private val createDevice: (DeviceId, String, Boolean) -> Device,
    private val sdkLifecycle: () -> SdkLifecycle,
    private val captureRemoteLog: (String, LogLevel) -> Unit
) {

    private val runner = ActionRunner(logSink, RemoteLogFailureReporter { captureRemoteLog(it, LogLevel.ERROR) })
    private val eventBus = EventBus()
    private val scope = retenoScope(dispatchers, logSink)
    private val deviceId: () -> DeviceId = { serviceLocator.restConfigProvider.get().deviceId }
    private val api by lazy { SuspendApiClient(serviceLocator.restClientProvider.get(), dispatchers) }
    private val configRepository by lazy { serviceLocator.configRepositoryProvider.get() }
    private val contactSession = ContactSession()
    private val reportInvalid: (String) -> Unit = { captureRemoteLog(it, LogLevel.INFO) }

    val contactQueue = SerialQueue(scope) { error ->
        logSink.log(LogPriority.ERROR, "Reteno") { "Contact task failed: ${Redact.stackTrace(error)}" }
    }

    private val currentDevice by lazy {
        CurrentDevice(configRepository, serviceLocator.retenoDatabaseManagerDeviceProvider.get(), createDevice, dispatchers)
    }

    private val segmentationRefresh by lazy {
        SegmentationRefresh({ serviceLocator.iamControllerProvider.get().refreshSegmentation() }, scope, runner)
    }

    private val contactWriter by lazy {
        ContactWriter(
            configRepository = configRepository,
            usersDb = serviceLocator.retenoDatabaseManagerUserProvider.get(),
            currentDevice = currentDevice,
            sync = dataSync,
            session = contactSession,
            sdk = sdkLifecycle,
            clearSession = { serviceLocator.retenoSessionHandlerProvider.get().clearSessionForced() },
            reportInvalid = reportInvalid,
            dispatchers = dispatchers
        )
    }

    val registerDevice: RegisterDeviceAction by lazy {
        RegisterDeviceAction(configRepository, currentDevice, dataSync, contactSession, runner)
    }

    val ensureDeviceRegisteredThisSession: EnsureDeviceRegisteredThisSessionAction by lazy {
        EnsureDeviceRegisteredThisSessionAction(
            configRepository, serviceLocator.retenoDatabaseManagerDeviceProvider.get(), currentDevice, dataSync, contactSession, dispatchers, runner
        )
    }

    val updateFcmToken: UpdateFcmTokenAction by lazy {
        UpdateFcmTokenAction(configRepository, currentDevice, dataSync, contactSession, runner)
    }

    val updateNotificationsEnabled: UpdateNotificationsEnabledAction by lazy {
        UpdateNotificationsEnabledAction(configRepository, currentDevice, dataSync, contactSession, runner)
    }

    val setUserAttributes: SetUserAttributesAction by lazy {
        SetUserAttributesAction(contactWriter, sdkLifecycle, segmentationRefresh::request, runner)
    }

    val setMultiAccountUserAttributes: SetMultiAccountUserAttributesAction by lazy {
        SetMultiAccountUserAttributesAction(contactWriter, sdkLifecycle, segmentationRefresh::request, runner)
    }

    val setAnonymousUserAttributes: SetAnonymousUserAttributesAction by lazy {
        SetAnonymousUserAttributesAction(
            configRepository, serviceLocator.retenoDatabaseManagerUserProvider.get(), dataSync, reportInvalid, dispatchers, runner
        )
    }

    val logEvent: LogEventAction by lazy {
        LogEventAction(serviceLocator.retenoDatabaseManagerEventsProvider.get(), deviceId, eventBus, dispatchers, runner)
    }

    val logEcomEvent: LogEcomEventAction by lazy {
        LogEcomEventAction(serviceLocator.retenoDatabaseManagerEventsProvider.get(), deviceId, eventBus, dispatchers, runner)
    }

    val logScreenView: LogScreenViewEventAction by lazy {
        LogScreenViewEventAction(logEvent)
    }

    val logRetenoEvent: LogRetenoEventAction by lazy {
        LogRetenoEventAction(serviceLocator.retenoDatabaseManagerLogEventProvider.get(), dispatchers, runner)
    }

    val clearOldData: ClearOldDataAction by lazy {
        ClearOldDataAction(
            interactionsDb = serviceLocator.retenoDatabaseManagerInteractionProvider.get(),
            inAppInteractionsDb = serviceLocator.retenoDatabaseManagerInAppInteractionProvider.get(),
            eventsDb = serviceLocator.retenoDatabaseManagerEventsProvider.get(),
            inboxDb = serviceLocator.retenoDatabaseManagerAppInboxProvider.get(),
            recomEventsDb = serviceLocator.retenoDatabaseManagerRecomEventsProvider.get(),
            wrappedLinksDb = serviceLocator.retenoDatabaseManagerWrappedLinksProvider.get(),
            outdatedTime = SchedulerUtils::getOutdatedTime,
            reportRemoved = { captureRemoteLog(it, LogLevel.INFO) },
            dispatchers = dispatchers,
            runner = runner
        )
    }

    val dataSync: DataSync by lazy {
        DataSync(
            coordinator = SyncCoordinator(syncData, scope, awaitInit),
            clearOldData = { clearOldData() },
            enqueueBackgroundSync = { PushDataWorker.enqueuePeriodicWork(serviceLocator.workManagerProvider.get()) },
            isDebugView = Util::isDebugView,
            jitter = { Random.nextLong(DataSync.RANDOM_DELAY) },
            scope = scope,
            dispatchers = dispatchers,
            runner = runner
        )
    }

    private val syncData: SyncDataAction by lazy {
        SyncDataAction(
            contactSteps = listOf(
                PushDeviceStep(
                    serviceLocator.retenoDatabaseManagerDeviceProvider.get(),
                    api,
                    { serviceLocator.configRepositoryProvider.get().saveDeviceRegistered(true) },
                    dispatchers,
                    runner
                ),
                PushUserStep(serviceLocator.retenoDatabaseManagerUserProvider.get(), api, dispatchers, runner)
            ),
            dataSteps = listOf(
                PushInteractionsStep(serviceLocator.retenoDatabaseManagerInteractionProvider.get(), api, dispatchers, runner),
                PushInAppInteractionsStep(serviceLocator.retenoDatabaseManagerInAppInteractionProvider.get(), api, dispatchers, runner),
                PushEventsStep(serviceLocator.retenoDatabaseManagerEventsProvider.get(), api, dispatchers, runner),
                PushInboxStatusStep(
                    serviceLocator.retenoDatabaseManagerAppInboxProvider.get(),
                    api,
                    { serviceLocator.appInboxRepositoryProvider.get().refreshMessagesCount() },
                    dispatchers,
                    runner
                ),
                PushRecomEventsStep(serviceLocator.retenoDatabaseManagerRecomEventsProvider.get(), api, dispatchers, runner),
                PushWrappedLinksStep(serviceLocator.retenoDatabaseManagerWrappedLinksProvider.get(), api, dispatchers, runner),
                PushLogEventsStep(serviceLocator.retenoDatabaseManagerLogEventProvider.get(), api, dispatchers, runner)
            ),
            runner = runner
        )
    }

    fun backgroundSync(
        isForeground: () -> Boolean,
        isInitialized: () -> Boolean,
        hasDataForSync: () -> Boolean
    ): BackgroundSyncAction = BackgroundSyncAction(
        isForeground = isForeground,
        isInitialized = isInitialized,
        hasDataForSync = hasDataForSync,
        sync = dataSync,
        cancelPeriodicWork = { serviceLocator.workManagerProvider.get().cancelUniqueWork(PushDataWorker.PUSH_DATA_WORK_NAME) },
        dispatchers = dispatchers,
        runner = runner
    )
}
