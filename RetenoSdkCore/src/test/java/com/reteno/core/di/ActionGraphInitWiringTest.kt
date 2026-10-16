package com.reteno.core.di

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.fixtures.ActionGraphFixture
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.stubInAppMessage
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionGraphInitWiringTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = false) {
        val env = ActionGraphFixture(scope, initialized)

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
        fixture.env.sut.control.boot()
        runCurrent()

        then()
        assertEquals(listOf("init WorkManager"), fixture.env.platform.calls)
        coVerify(exactly = 1) { fixture.env.restConfig.initDeviceId(DeviceIdMode.CACHE) }
        assertSame(cached, fixture.env.configuration.config)
        verify(exactly = 1) { fixture.env.prefs.cacheConfiguration(fixture.env.deviceId.id, cached) }
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(cached.sessionDuration) }
        assertEquals(InitState.FROM_CACHE, fixture.env.initGate.state)
    }

    @Test
    fun `should track activities when booting`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.env.sut.control.boot()

        then()
        assertEquals(1, fixture.env.platform.activityTrackingStarts)
    }

    @Test
    fun `should stay uninitialized when nothing is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.env.prefs.getCachedConfiguration() } returns null

        whenn()
        fixture.env.sut.control.boot()
        runCurrent()

        then()
        assertEquals(InitState.NOT_INITIALIZED, fixture.env.initGate.state)
    }

    @Test
    fun `should set user configuration before queued initialization runs`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig(customDeviceId = true)

        whenn()
        fixture.env.sut.control.applyUserConfig(config)

        then()
        assertSame(config, fixture.env.configuration.config)
        assertEquals(InitState.NOT_INITIALIZED, fixture.env.initGate.state)
    }

    @Test
    fun `should apply queued user configuration on main`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var onMain: Boolean? = null
        every { fixture.env.sessionHandler.setSessionResetDuration(any()) } answers {
            onMain = fixture.env.dispatchers.markedMain.isRunningHere
        }

        whenn()
        fixture.env.sut.control.applyUserConfig(stubConfig(customDeviceId = true))
        runCurrent()

        then()
        assertEquals(true, onMain)
    }

    @Test
    fun `should apply user configuration register device and start lifecycle tracking`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig(customDeviceId = true, pausedInApps = true)
        coEvery { fixture.env.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.env.sut.control.applyUserConfig(config)
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.env.restConfig.initDeviceId(DeviceIdMode.CLIENT_UUID) }
        verify(exactly = 1) { fixture.env.devicesDb.insertDevice(any()) }
        assertTrue(fixture.env.graph.inApp.inAppMessagesState.isPaused)
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
        fixture.env.sut.control.boot()
        fixture.env.sut.control.applyUserConfig(user)
        runCurrent()

        then()
        assertSame(user, fixture.env.configuration.config)
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(2_000L) }
        assertEquals(InitState.BY_USER, fixture.env.initGate.state)
    }

    @Test
    fun `should start and stop sdk after user initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)

        whenn()
        fixture.env.sut.control.requestStart()
        runCurrent()
        val startedAfterStart = fixture.env.sut.control.isStarted
        fixture.env.sut.control.requestStop()
        runCurrent()

        then()
        assertTrue(startedAfterStart)
        assertFalse(fixture.env.sut.control.isStarted)
        verify(exactly = 1) { fixture.env.sessionHandler.start() }
        assertEquals(1, fixture.env.rest.requests.count { it.contract == ApiContract.InAppMessages.GetInAppMessages })
        verify(exactly = 1) { fixture.env.sessionHandler.stop() }
    }

    @Test
    fun `should show in-app offered before start once sdk starts`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)
        fixture.env.graph.inApp.iamView.initialize(stubInAppMessage())
        runCurrent()
        val createdBeforeStart = fixture.env.platform.screen.calls.toList()

        whenn()
        fixture.env.sut.control.requestStart()
        runCurrent()

        then()
        assertTrue(createdBeforeStart.isEmpty())
        assertTrue(fixture.env.platform.screen.calls.single().startsWith("create"))
    }

    @Test
    fun `should hold in-app offered after sdk stops`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)
        fixture.env.sut.control.requestStart()
        runCurrent()
        fixture.env.sut.control.requestStop()
        runCurrent()

        whenn()
        fixture.env.graph.inApp.iamView.initialize(stubInAppMessage())
        runCurrent()

        then()
        assertTrue(fixture.env.platform.screen.calls.isEmpty())
    }

    @Test
    fun `should hold push in-app when user configuration pauses push in-apps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.sut.control.applyUserConfig(stubConfig(pausedPushInApps = true))
        runCurrent()
        fixture.env.sut.push.submitIamFromPush("interaction-1")
        runCurrent()

        whenn()
        fixture.env.platform.screen.postWidgetMessage("""{"type":"WIDGET_INIT_SUCCESS"}""")
        runCurrent()

        then()
        assertTrue(fixture.env.platform.screen.calls.single().startsWith("create"))
    }

    @Test
    fun `should wait for user initialization before starting sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.env.initGate.markInitialized(byUser = false)

        whenn()
        fixture.env.sut.control.requestStart()
        runCurrent()

        then()
        verify(exactly = 0) { fixture.env.sessionHandler.start() }
    }

    @Test
    fun `should apply session duration to session handler`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)

        whenn()
        fixture.env.graph.init.setSessionDuration(5_000L)

        then()
        verify(exactly = 1) { fixture.env.sessionHandler.setSessionResetDuration(5_000L) }
    }

    @Test
    fun `should report sdk initialized once the gate opens`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        val initializedBefore = fixture.env.sut.control.isInitialized

        whenn()
        fixture.env.initGate.markInitialized(byUser = true)

        then()
        assertFalse(initializedBefore)
        assertTrue(fixture.env.sut.control.isInitialized)
    }

    @Test
    fun `should run operations held before init on main once the gate opens`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        var ranOnMain: Boolean? = null
        fixture.env.sut.control.runAfterInit { ranOnMain = fixture.env.dispatchers.markedMain.isRunningHere }
        runCurrent()
        val ranBefore = ranOnMain

        whenn()
        fixture.env.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(null, ranBefore)
        assertEquals(true, ranOnMain)
    }

    @Test
    fun `should run operations at once after init`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = true)
        val ran = mutableListOf<String>()

        whenn()
        fixture.env.sut.control.runAfterInit { ran += "now" }

        then()
        assertEquals(listOf("now"), ran)
    }
}
