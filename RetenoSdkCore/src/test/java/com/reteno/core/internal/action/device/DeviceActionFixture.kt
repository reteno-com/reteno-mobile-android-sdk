package com.reteno.core.internal.action.device

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope

internal class DeviceActionFixture(scope: TestScope) {
    val configRepository = mockk<ConfigRepository>()
    val devicesDb = mockk<RetenoDatabaseManagerDevice>()
    val deviceId = stubDeviceId()
    val sync = FakeSyncTrigger()
    val session = ContactSession()
    val dispatchers = scope.testDispatchers()
    val traced = TestActionRunner(scope)
    val currentDevice = CurrentDevice(configRepository, devicesDb, ::stubDevice, dispatchers)

    init {
        every { configRepository.getDeviceId() } returns deviceId
        every { configRepository.isNotificationsEnabled() } returns true
        every { devicesDb.getDevices(any()) } returns emptyList()
        justRun { devicesDb.insertDevice(any()) }
        justRun { devicesDb.deleteDevices(any()) }
    }
}
