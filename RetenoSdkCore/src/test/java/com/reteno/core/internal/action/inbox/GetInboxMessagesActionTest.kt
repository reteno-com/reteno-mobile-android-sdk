package com.reteno.core.internal.action.inbox

import com.google.gson.JsonSyntaxException
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.domain.model.appinbox.AppInboxMessage
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetInboxMessagesActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val callback = RecordingResultCallback<AppInboxMessages>(dispatchers.markedMain)
        val sut = GetInboxMessagesAction(rest.api(dispatchers), initGate, dispatchers, traced.runner)
    }

    @Test
    fun `should request inbox page with its filters`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(MESSAGES_JSON)

        whenn()
        fixture.sut(page = 2, pageSize = 20, status = AppInboxStatus.UNOPENED, callback = fixture.callback)

        then()
        assertEquals(
            listOf(
                SentRequest(
                    HttpMethod.GET,
                    ApiContract.AppInbox.Messages,
                    null,
                    mapOf("page" to "2", "pageSize" to "20", "status" to "UNOPENED")
                )
            ),
            fixture.rest.requests
        )
    }

    @Test
    fun `should request all messages without filters`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(MESSAGES_JSON)

        whenn()
        fixture.sut(page = null, pageSize = null, status = null, callback = fixture.callback)

        then()
        assertEquals(mapOf("page" to null, "pageSize" to null, "status" to null), fixture.rest.requests.single().queryParams)
    }

    @Test
    fun `should pass messages to host callback on main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(MESSAGES_JSON)

        whenn()
        val result = fixture.sut(page = 1, pageSize = 10, status = null, callback = fixture.callback)

        then()
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                AppInboxMessages(
                    messages = listOf(
                        AppInboxMessage(
                            id = "m1",
                            title = "Sale",
                            createdDate = "2026-01-02",
                            isNewMessage = true,
                            content = "Up to 50%",
                            imageUrl = null,
                            linkUrl = null,
                            category = null,
                            status = AppInboxStatus.UNOPENED,
                            customData = null
                        )
                    ),
                    totalPages = 3
                )
            ),
            fixture.callback.results
        )
        assertEquals(listOf(true), fixture.callback.calledOnMain)
    }

    @Test
    fun `should pass server failure to host callback without reporting it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(401, "unauthorized")

        whenn()
        val result = fixture.sut(page = null, pageSize = null, status = null, callback = fixture.callback)

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(401, "unauthorized", null)), fixture.callback.failures)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should pass unreadable response to host callback and report it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("{not json")

        whenn()
        val result = fixture.sut(page = null, pageSize = null, status = null, callback = fixture.callback)

        then()
        assertTrue(result.exceptionOrNull() is JsonSyntaxException)
        assertTrue(fixture.callback.failures.single().third is JsonSyntaxException)
        assertEquals(listOf(true), fixture.callback.calledOnMain)
        assertEquals("read result from server response", fixture.traced.reports.single().step)
    }

    @Test
    fun `should wait for sdk initialization before requesting messages`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.rest.succeedWith(MESSAGES_JSON)
        launch { fixture.sut(page = null, pageSize = null, status = null, callback = fixture.callback) }
        runCurrent()
        val requestsBeforeInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(0, requestsBeforeInit)
        assertEquals(1, fixture.callback.results.size)
    }

    @Test
    fun `should trace filters without server details`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(MESSAGES_JSON)

        whenn()
        fixture.sut(page = 1, pageSize = 10, status = AppInboxStatus.OPENED, callback = fixture.callback)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("GetInboxMessages  page=1  pageSize=10  status=OPENED"))
        assertFalse(lines.any { it.contains("http") || it.contains("Up to 50%") })
    }

    private companion object {
        const val MESSAGES_JSON = """{"list":[{"id":"m1","title":"Sale","createDate":"2026-01-02","newMessage":true,"content":"Up to 50%","status":"UNOPENED"}],"totalPages":3}"""
    }
}
