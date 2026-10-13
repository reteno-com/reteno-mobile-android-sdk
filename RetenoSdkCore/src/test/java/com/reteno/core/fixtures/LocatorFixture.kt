package com.reteno.core.fixtures

import androidx.work.WorkManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.repository.AppInboxRepository
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.ActionsLocator
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.lifecycle.RetenoSessionHandler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope

internal class LocatorFixture(scope: TestScope, initialized: Boolean = true) {
    val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
    val devicesDb = mockk<RetenoDatabaseManagerDevice>(relaxed = true)
    val usersDb = mockk<RetenoDatabaseManagerUser>(relaxed = true)
    val interactionsDb = mockk<RetenoDatabaseManagerInteraction>(relaxed = true)
    val inAppInteractionsDb = mockk<RetenoDatabaseManagerInAppInteraction>(relaxed = true)
    val eventsDb = mockk<RetenoDatabaseManagerEvents>(relaxed = true)
    val inboxDb = mockk<RetenoDatabaseManagerAppInbox>(relaxed = true)
    val recomEventsDb = mockk<RetenoDatabaseManagerRecomEvents>(relaxed = true)
    val wrappedLinksDb = mockk<RetenoDatabaseManagerWrappedLink>(relaxed = true)
    val logEventsDb = mockk<RetenoDatabaseManagerLogEvent>(relaxed = true)
    val configRepository = mockk<ConfigRepository>(relaxed = true)
    val inboxRepository = mockk<AppInboxRepository>(relaxed = true)
    val workManager = mockk<WorkManager>(relaxed = true)
    val sessionHandler = mockk<RetenoSessionHandler>(relaxed = true)
    val iamController = mockk<IamController>(relaxed = true)
    val rest = FakeRestClient()
    val deviceId = stubDeviceId()
    val sink = FakeLogSink()
    val remoteLogs = mutableListOf<Pair<String, LogLevel>>()
    private val serviceLocator = mockk<ServiceLocator>().also {
        every { it.retenoDatabaseManagerDeviceProvider.get() } returns devicesDb
        every { it.retenoDatabaseManagerUserProvider.get() } returns usersDb
        every { it.retenoDatabaseManagerInteractionProvider.get() } returns interactionsDb
        every { it.retenoDatabaseManagerInAppInteractionProvider.get() } returns inAppInteractionsDb
        every { it.retenoDatabaseManagerEventsProvider.get() } returns eventsDb
        every { it.retenoDatabaseManagerAppInboxProvider.get() } returns inboxDb
        every { it.retenoDatabaseManagerRecomEventsProvider.get() } returns recomEventsDb
        every { it.retenoDatabaseManagerWrappedLinksProvider.get() } returns wrappedLinksDb
        every { it.retenoDatabaseManagerLogEventProvider.get() } returns logEventsDb
        every { it.configRepositoryProvider.get() } returns configRepository
        every { it.appInboxRepositoryProvider.get() } returns inboxRepository
        every { it.workManagerProvider.get() } returns workManager
        every { it.restClientProvider.get() } returns rest
        every { it.restConfigProvider.get().deviceId } returns deviceId
        every { it.retenoSessionHandlerProvider.get() } returns sessionHandler
        every { it.iamControllerProvider.get() } returns iamController
    }
    val sdk = FakeSdkLifecycle()

    init {
        every { configRepository.getDeviceId() } returns deviceId
    }

    val sut = ActionsLocator(
        serviceLocator = serviceLocator,
        dispatchers = scope.testDispatchers(),
        logSink = sink,
        awaitInit = initGate::awaitInit,
        createDevice = ::stubDevice,
        sdkLifecycle = { sdk }
    ) { message, level -> remoteLogs += message to level }
}
