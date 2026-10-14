package com.reteno.core.fixtures

import androidx.work.WorkManager
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.ActionsLocator
import com.reteno.core.di.ServiceLocator
import com.reteno.core.di.provider.RetenoConfigProvider
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.internal.config.SdkConfigurationImpl
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import com.reteno.test.workScope
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope

internal class LocatorFixture(scope: TestScope, initialized: Boolean = true) {
    val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
    val devicesDb = mockk<RetenoDatabaseManagerDevice>(relaxed = true)
    val usersDb = mockk<RetenoDatabaseManagerUser>(relaxed = true)
    val interactionsDb = mockk<RetenoDatabaseManagerInteraction>(relaxed = true)
    val inAppInteractionsDb = mockk<RetenoDatabaseManagerInAppInteraction>(relaxed = true)
    val eventsDb = mockk<RetenoDatabaseManagerEvents>(relaxed = true)
    val inboxDb = mockk<RetenoDatabaseManagerAppInbox>(relaxed = true)
    val inAppDb = mockk<RetenoDatabaseManagerInAppMessages>(relaxed = true)
    val files = mockk<FileManager>(relaxed = true)
    val recomEventsDb = mockk<RetenoDatabaseManagerRecomEvents>(relaxed = true)
    val wrappedLinksDb = mockk<RetenoDatabaseManagerWrappedLink>(relaxed = true)
    val logEventsDb = mockk<RetenoDatabaseManagerLogEvent>(relaxed = true)
    val configRepository = mockk<ConfigRepository>(relaxed = true)
    val workManager = mockk<WorkManager>(relaxed = true)
    val sessionHandler = mockk<RetenoSessionHandler>(relaxed = true)
    val iamController = mockk<IamController>(relaxed = true)
    val iamView = mockk<IamView>(relaxed = true)
    val prefs = mockk<SharedPrefsManager>(relaxed = true)
    val restConfig = mockk<RestConfig>(relaxed = true)
    val activityHelper = mockk<RetenoActivityHelper>(relaxed = true)
    val databaseManager = mockk<RetenoDatabaseManager>(relaxed = true)
    val rest = FakeRestClient()
    val deviceId = stubDeviceId()
    val sink = FakeLogSink()
    val remoteLog = FakeRemoteLog()
    val remoteLogs: List<Pair<String, LogLevel>>
        get() = remoteLog.entries
    val serviceLocator = mockk<ServiceLocator>().also {
        every { it.retenoDatabaseManagerDeviceProvider.get() } returns devicesDb
        every { it.retenoDatabaseManagerUserProvider.get() } returns usersDb
        every { it.retenoDatabaseManagerInteractionProvider.get() } returns interactionsDb
        every { it.retenoDatabaseManagerInAppInteractionProvider.get() } returns inAppInteractionsDb
        every { it.retenoDatabaseManagerEventsProvider.get() } returns eventsDb
        every { it.retenoDatabaseManagerAppInboxProvider.get() } returns inboxDb
        every { it.retenoDatabaseManagerInAppMessagesProvider.get() } returns inAppDb
        every { it.fileManagerProvider.get() } returns files
        every { it.retenoDatabaseManagerRecomEventsProvider.get() } returns recomEventsDb
        every { it.retenoDatabaseManagerWrappedLinksProvider.get() } returns wrappedLinksDb
        every { it.retenoDatabaseManagerLogEventProvider.get() } returns logEventsDb
        every { it.configRepositoryProvider.get() } returns configRepository
        every { it.workManagerProvider.get() } returns workManager
        every { it.restClientProvider.get() } returns rest
        every { it.restConfigProvider.get() } returns restConfig
        every { it.sharedPrefsManagerProvider.get() } returns prefs
        every { it.iamViewProvider.get() } returns iamView
        every { it.retenoActivityHelperProvider.get() } returns activityHelper
        every { it.retenoDatabaseManagerProvider.get() } returns databaseManager
        every { it.retenoSessionHandlerProvider.get() } returns sessionHandler
        every { it.iamControllerProvider.get() } returns iamController
    }
    val platform = FakeAppPlatform()
    val lifecycleEventFilter = LifecycleEventFilter(LifecycleTrackingOptions.ALL)
    val configProvider = mockk<RetenoConfigProvider>(relaxed = true)
    val configuration = SdkConfigurationImpl(configProvider, lifecycleEventFilter)
    val insertedEvents = mutableListOf<EventsDb>()
    val insertedEventKeys: List<String>
        get() = insertedEvents.flatMap { batch -> batch.eventList.map { it.eventTypeKey } }

    init {
        every { configRepository.getDeviceId() } returns deviceId
        every { restConfig.deviceId } returns deviceId
        every { prefs.getIamBaseUrl() } returns null
        every { eventsDb.insertEvents(capture(insertedEvents)) } returns Unit
    }

    fun storeInteractionRequests(): MutableList<InteractionRequestDb> {
        val stored = mutableListOf<InteractionRequestDb>()
        every { interactionsDb.insertInteractionRequest(any()) } answers { stored += firstArg<InteractionRequestDb>() }
        every { interactionsDb.getInteractionRequests() } answers { stored.toList() }
        every { interactionsDb.deleteInteractionRequest(any()) } answers { stored.remove(firstArg()) }
        return stored
    }

    private val dispatchers = scope.testDispatchers()

    val sut = ActionsLocator(
        serviceLocator = serviceLocator,
        dispatchers = dispatchers,
        logSink = sink,
        scope = scope.workScope().let { work ->
            work + SupervisorJob(work.coroutineContext[Job]) + dispatchers.default
        },
        initGate = initGate,
        configuration = configuration,
        platform = platform,
        lifecycleEventFilter = lifecycleEventFilter,
        remoteLog = remoteLog,
        clock = { 1_000L }
    )
}
