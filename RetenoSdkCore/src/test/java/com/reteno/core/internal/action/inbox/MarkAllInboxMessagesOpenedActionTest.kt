package com.reteno.core.internal.action.inbox

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MarkAllInboxMessagesOpenedActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val calls = mutableListOf<String>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val inboxDb = mockk<RetenoDatabaseManagerAppInbox>().also {
            every { it.deleteAllAppInboxMessages() } answers {
                calls += "delete local statuses" + if (dispatchers.markedIo.isRunningHere) " on io" else ""
            }
        }
        val inboxCount = mockk<InboxCountPolling>().also {
            every { it.refresh() } answers { calls += "refresh count" }
        }
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val callback = RecordingResultCallback<Unit>(dispatchers.markedMain)
        val sut = MarkAllInboxMessagesOpenedAction(
            rest.api(dispatchers),
            inboxDb,
            inboxCount,
            initGate,
            dispatchers,
            traced.runner
        )
    }

    @Test
    fun `should mark all messages opened on server then clear local statuses and refresh count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(fixture.callback)

        then()
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.AppInbox.MessagesStatus, """{"status":"OPENED"}""")),
            fixture.rest.requests
        )
        assertEquals(listOf("delete local statuses on io", "refresh count"), fixture.calls)
        assertEquals(listOf(Unit), fixture.callback.results)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
    }

    @Test
    fun `should pass server failure to host callback and keep local statuses`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(500, "error")

        whenn()
        fixture.sut(fixture.callback)

        then()
        assertTrue(fixture.calls.isEmpty())
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(500, "error", null)), fixture.callback.failures)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should wait for the host to initialize the sdk before sending`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        launch { fixture.sut(fixture.callback) }
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()
        val requestsBeforeHostInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(0, requestsBeforeHostInit)
        assertEquals(listOf(Unit), fixture.callback.results)
    }
}
