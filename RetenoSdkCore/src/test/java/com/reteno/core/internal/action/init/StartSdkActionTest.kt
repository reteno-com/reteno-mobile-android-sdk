package com.reteno.core.internal.action.init

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.device.EnsureDeviceRegisteredThisSession
import com.reteno.core.internal.action.device.RefreshDeviceLanguage
import com.reteno.core.internal.action.device.SettleDeviceLanguage
import com.reteno.core.internal.action.iam.FetchInAppMessages
import com.reteno.core.internal.action.iam.ProcessInAppMessages
import com.reteno.core.internal.action.inbox.InboxCountPolling
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.lifecycle.SdkRunState
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StartSdkActionTest {

    private class SutFixture(scope: TestScope) {
        val calls = mutableListOf<String>()
        val deviceCalls = mutableListOf<String>()
        var languageSettled = CompletableDeferred(Unit)
        var fetched: Result<List<InAppMessage>> = Result.success(listOf(stubInAppMessage(messageId = 7)))
        val dispatchers = scope.testDispatchers()
        val state = SdkRunState()
        val traced = TestActionRunner(scope)
        private val settleDeviceLanguage = mockk<SettleDeviceLanguage>().also {
            coEvery { it() } coAnswers {
                deviceCalls += "settle device language" + onIo()
                languageSettled.await()
                Result.success(Unit)
            }
        }
        private val ensureDeviceRegistered = mockk<EnsureDeviceRegisteredThisSession>().also {
            coEvery { it() } answers { deviceCalls += "ensure device" + onIo(); Result.success(Unit) }
        }
        private val refreshDeviceLanguage = mockk<RefreshDeviceLanguage>().also {
            coEvery { it() } answers { deviceCalls += "refresh device language" + onIo(); Result.success(Unit) }
        }
        private val sessionHandler = mockk<RetenoSessionHandler>().also {
            every { it.start() } answers { calls += "start session" }
        }
        private val fetchInAppMessages = mockk<FetchInAppMessages>().also {
            coEvery { it() } answers {
                calls += "fetch in-apps" + onIo()
                fetched
            }
        }
        private val processInAppMessages = mockk<ProcessInAppMessages>().also {
            coEvery { it(any()) } answers {
                calls += "schedule in-apps ${firstArg<List<InAppMessage>>().map { it.messageId }}" + onIo()
                Result.success(Unit)
            }
        }
        private val dataSync = mockk<SyncScheduler>().also {
            every { it.startScheduler() } answers { calls += "start scheduler" }
        }
        private val iamView = mockk<IamView>().also {
            every { it.start() } answers { calls += "start in-app view" }
        }
        private val inboxCount = mockk<InboxCountPolling>().also {
            every { it.resume() } answers { calls += "resume inbox count polling" }
        }
        val sut = StartSdkAction(
            settleDeviceLanguage = settleDeviceLanguage,
            ensureDeviceRegistered = ensureDeviceRegistered,
            refreshDeviceLanguage = refreshDeviceLanguage,
            sessionHandler = sessionHandler,
            fetchInAppMessages = fetchInAppMessages,
            processInAppMessages = processInAppMessages,
            dataSync = dataSync,
            iamView = iamView,
            inboxCount = inboxCount,
            state = state,
            dispatchers = dispatchers,
            runner = traced.runner
        )

        private fun onIo() = if (dispatchers.markedIo.isRunningHere) " on io" else ""
    }

    @Test
    fun `should register the device with the settled language on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf(
                "settle device language on io",
                "ensure device on io",
                "refresh device language on io"
            ),
            fixture.deviceCalls
        )
    }

    @Test
    fun `should fetch in-app messages without waiting for the device language to settle`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.languageSettled = CompletableDeferred()

        whenn()
        workScope().launch { fixture.sut() }
        runCurrent()

        then()
        assertTrue("fetch in-apps on io" in fixture.calls)
        assertEquals(listOf("settle device language on io"), fixture.deviceCalls)
    }

    @Test
    fun `should finish starting only after the device is registered`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.languageSettled = CompletableDeferred()
        val start = workScope().launch { fixture.sut() }
        runCurrent()

        whenn()
        fixture.languageSettled.complete(Unit)
        runCurrent()

        then()
        assertTrue(start.isCompleted)
        assertEquals("refresh device language on io", fixture.deviceCalls.last())
    }

    @Test
    fun `should start sdk parts in order on their threads`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf(
                "start session",
                "fetch in-apps on io",
                "schedule in-apps [7] on io",
                "start scheduler",
                "start in-app view",
                "resume inbox count polling"
            ),
            fixture.calls
        )
        assertTrue(fixture.state.isStarted)
    }

    @Test
    fun `should schedule no in-apps when they cannot be fetched`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.fetched = Result.failure(IllegalStateException("database closed"))

        whenn()
        fixture.sut()

        then()
        assertTrue("schedule in-apps [] on io" in fixture.calls)
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
        assertEquals(
            listOf(
                "start session",
                "start scheduler",
                "start in-app view",
                "resume inbox count polling"
            ),
            fixture.calls
        )
        assertTrue(
            fixture.traced.sink.lines.any { it.contains("→ sdk already started, in-app messages are fetched") }
        )
    }
}
