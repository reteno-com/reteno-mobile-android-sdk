package com.reteno.core.internal.action.inbox

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetInboxMessagesCountActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val callback = RecordingResultCallback<Int>(dispatchers.markedMain)
        val sut = GetInboxMessagesCountAction(rest.api(dispatchers), initGate, dispatchers, traced.runner)
    }

    @Test
    fun `should pass unread messages count to host callback on main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("""{"unreadCount":7}""")

        whenn()
        fixture.sut(fixture.callback)

        then()
        assertEquals(
            listOf(SentRequest(HttpMethod.GET, ApiContract.AppInbox.MessagesCount, null, null)),
            fixture.rest.requests
        )
        assertEquals(listOf(7), fixture.callback.results)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
    }

    @Test
    fun `should pass network failure to host callback`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failOffline()

        whenn()
        fixture.sut(fixture.callback)

        then()
        val (statusCode, _, error) = fixture.callback.failures.single()
        assertEquals(null, statusCode)
        assertTrue(error is IOException)
    }

    @Test
    fun `should wait for sdk initialization before requesting count`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.rest.succeedWith("""{"unreadCount":1}""")
        launch { fixture.sut(fixture.callback) }
        runCurrent()
        val requestsBeforeInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(0, requestsBeforeInit)
        assertEquals(listOf(1), fixture.callback.results)
    }
}
