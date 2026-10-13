package com.reteno.core.internal.action.user

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.FakeSdkLifecycle
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.device.ContactSession
import com.reteno.core.internal.action.device.CurrentDevice
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope

internal class UserActionFixture(scope: TestScope, suffix: String? = null) {
    val configRepository = mockk<ConfigRepository>()
    val devicesDb = mockk<RetenoDatabaseManagerDevice>()
    val usersDb = mockk<RetenoDatabaseManagerUser>()
    var deviceId: DeviceId = stubDeviceId(idSuffix = suffix, externalId = "old-user")
    val sync = FakeSyncTrigger()
    val session = ContactSession()
    val dispatchers = scope.testDispatchers()
    val traced = TestActionRunner(scope)
    val sdk = FakeSdkLifecycle(onMain = { dispatchers.markedMain.isRunningHere })
    val log = mutableListOf<String>()
    val invalidReports = mutableListOf<String>()
    var sessionClearedOnIo: Boolean? = null
    val writer = ContactWriter(
        configRepository = configRepository,
        usersDb = usersDb,
        currentDevice = CurrentDevice(configRepository, devicesDb, ::stubDevice, dispatchers),
        sync = sync,
        session = session,
        sdk = { sdk },
        clearSession = { sessionClearedOnIo = dispatchers.markedIo.isRunningHere; log += "clear session" },
        reportInvalid = { invalidReports += it },
        dispatchers = dispatchers
    )

    init {
        every { configRepository.getDeviceId() } answers { deviceId }
        every { configRepository.isNotificationsEnabled() } returns true
        every { configRepository.setExternalUserId(any()) } answers { log += "external id ${firstArg<String?>()}"; deviceId = deviceId.copy(externalId = firstArg()) }
        every { configRepository.setDeviceIdSuffix(any()) } answers { log += "suffix ${firstArg<String?>()}"; deviceId = deviceId.copy(idSuffix = firstArg()) }
        justRun { configRepository.setUserEmail(any()) }
        justRun { configRepository.setUserPhone(any()) }
        coEvery { configRepository.getFcmToken() } returns "token"
        every { devicesDb.getDevices(any()) } returns emptyList()
        justRun { devicesDb.insertDevice(any()) }
        every { usersDb.insertUser(any()) } answers { log += "user saved" }
    }
}
