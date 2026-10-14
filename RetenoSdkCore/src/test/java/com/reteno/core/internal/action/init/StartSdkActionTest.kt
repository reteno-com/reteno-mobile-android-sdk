package com.reteno.core.internal.action.init

import com.reteno.core.domain.controller.IamController
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSession
import com.reteno.core.internal.action.inbox.InboxCountPolling
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StartSdkActionTest {

    private class SutFixture(scope: TestScope) {
        val calls = mutableListOf<String>()
        val dispatchers = scope.testDispatchers()
        val state = SdkRunState()
        val traced = TestActionRunner(scope)
        private val ensureDeviceRegistered = mockk<EnsureDeviceRegisteredThisSession>().also {
            coEvery { it() } answers { calls += "ensure device" + onIo(); Result.success(Unit) }
        }
        private val sessionHandler = mockk<RetenoSessionHandler>().also { every { it.start() } answers { calls += "start session" } }
        private val iamController = mockk<IamController>().also { coEvery { it.getInAppMessages() } answers { calls += "fetch in-apps" + onIo() } }
        private val dataSync = mockk<SyncScheduler>().also { every { it.startScheduler() } answers { calls += "start scheduler" } }
        private val iamView = mockk<IamView>().also { every { it.start() } answers { calls += "start in-app view" + onMain() } }
        private val inboxCount = mockk<InboxCountPolling>().also { every { it.resume() } answers { calls += "resume inbox count polling" } }
        val sut = StartSdkAction(ensureDeviceRegistered, sessionHandler, iamController, dataSync, iamView, inboxCount, state, dispatchers, traced.runner)

        private fun onIo() = if (dispatchers.markedIo.isRunningHere) " on io" else ""

        private fun onMain() = if (dispatchers.markedMain.isRunningHere) " on main" else ""
    }

    @Test
    fun `should start sdk parts in order on their threads`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf("ensure device on io", "start session", "fetch in-apps on io", "start scheduler", "start in-app view on main", "resume inbox count polling"),
            fixture.calls
        )
        assertTrue(fixture.state.isStarted)
    }

    @Test
    fun `should fetch in-app messages only once until stopped`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut()
        fixture.calls.clear()

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ensure device on io", "start session", "start scheduler", "start in-app view on main", "resume inbox count polling"), fixture.calls)
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ sdk already started, in-app messages are fetched") })
    }
}
