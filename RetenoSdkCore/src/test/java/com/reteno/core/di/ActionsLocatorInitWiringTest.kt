package com.reteno.core.di

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.internal.coroutine.InitState
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorInitWiringTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = false) {
        val env = LocatorFixture(scope, initialized)

        init {
            coEvery { env.configRepository.awaitForDeviceId() } returns env.deviceId
        }
    }

    @Test
    fun `should boot from cached configuration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val cached = stubConfig()
        every { fixture.env.prefs.getCachedConfiguration() } returns cached

        whenn()
        fixture.env.sut.boot()
        runCurrent()

        then()
        assertEquals(listOf("init WorkManager"), fixture.env.platform.calls)
        coVerify(exactly = 1) { fixture.env.restConfig.initDeviceId(DeviceIdMode.CACHE) }
        verify(exactly = 1) { fixture.env.configProvider.setConfig(cached) }
        verify(exactly = 1) { fixture.env.prefs.cacheConfiguration(fixture.env.deviceId.id, cached) }
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(cached.sessionDuration) }
        assertEquals(InitState.FROM_CACHE, fixture.env.initGate.state)
    }

    @Test
    fun `should stay uninitialized when nothing is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.prefs.getCachedConfiguration() } returns null

        whenn()
        fixture.env.sut.boot()
        runCurrent()

        then()
        assertEquals(InitState.NOT_INITIALIZED, fixture.env.initGate.state)
    }

    @Test
    fun `should apply user configuration register device and start lifecycle tracking`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig(customDeviceId = true)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.env.sut.applyUserConfig(config)
        val configSetImmediately = runCatching { verify(exactly = 1) { fixture.env.configProvider.setConfig(config) } }.isSuccess
        runCurrent()

        then()
        assertTrue(configSetImmediately)
        coVerify(exactly = 1) { fixture.env.restConfig.initDeviceId(DeviceIdMode.CLIENT_UUID) }
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(any()) }
        verify(exactly = 1) { fixture.env.iamController.pauseInAppMessages(false) }
        verify(exactly = 1) { fixture.env.iamView.pauseIncomingPushInApps(true) }
        verify(exactly = 1) { fixture.env.interactionsDb.getInteractionRequests() }
        assertNotNull(fixture.env.platform.onForeground)
        assertEquals(InitState.BY_USER, fixture.env.initGate.state)
    }

    @Test
    fun `should keep user configuration when cached boot runs first`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val cached = stubConfig(sessionDuration = 1_000L)
        val user = stubConfig(sessionDuration = 2_000L)
        every { fixture.env.prefs.getCachedConfiguration() } returns cached

        whenn()
        fixture.env.sut.boot()
        fixture.env.sut.applyUserConfig(user)
        runCurrent()

        then()
        verify(exactly = 0) { fixture.env.configProvider.setConfig(cached) }
        verify(atLeast = 1) { fixture.env.configProvider.setConfig(user) }
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(2_000L) }
        assertEquals(InitState.BY_USER, fixture.env.initGate.state)
    }

    @Test
    fun `should start and stop sdk after user initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)

        whenn()
        fixture.env.sut.requestStart()
        runCurrent()
        val startedAfterStart = fixture.env.sut.isStarted
        fixture.env.sut.requestStop()
        runCurrent()

        then()
        assertTrue(startedAfterStart)
        assertFalse(fixture.env.sut.isStarted)
        verify(exactly = 1) { fixture.env.sessionHandler.start() }
        coVerify(exactly = 1) { fixture.env.iamController.getInAppMessages() }
        verify(exactly = 1) { fixture.env.iamView.start() }
        verify(exactly = 1) { fixture.env.sessionHandler.stop() }
        verify(exactly = 1) { fixture.env.iamView.pause() }
    }

    @Test
    fun `should wait for user initialization before starting sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.env.initGate.markInitialized(byUser = false)

        whenn()
        fixture.env.sut.requestStart()
        runCurrent()

        then()
        verify(exactly = 0) { fixture.env.sessionHandler.start() }
    }

    @Test
    fun `should apply session duration to session handler`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)

        whenn()
        fixture.env.sut.setSessionDuration(5_000L)

        then()
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(5_000L) }
    }
}
