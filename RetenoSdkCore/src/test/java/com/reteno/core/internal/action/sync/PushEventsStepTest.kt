package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.model.ecom.EVENT_TYPE_CART_UPDATED
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubEventDb
import com.reteno.core.fixtures.stubEventsDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

internal class PushEventsStepTest {

    private class SutFixture(scope: TestScope) {
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushEventsStep(eventsDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            justRun { eventsDb.deleteEvents(any()) }
        }
    }

    @Test
    fun `should continue without request when there are no events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send events of first device and delete them`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val events = stubEventsDb()
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(events), emptyList())

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.MobileApi.Events, events.toRemote().toJson())),
            fixture.rest.requests
        )
        verify(exactly = 1) { fixture.eventsDb.deleteEvents(events) }
    }

    @Test
    fun `should send only latest cart update among events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val cart = EVENT_TYPE_CART_UPDATED
        val olderCart = stubEventDb(rowId = "1", eventTypeKey = cart, occurred = "2026-01-02T03:04:05.000Z")
        val newerCart = stubEventDb(rowId = "2", eventTypeKey = cart, occurred = "2026-01-02T03:04:06.000Z")
        val purchase = stubEventDb(rowId = "3", eventTypeKey = "purchase")
        val events = stubEventsDb(listOf(olderCart, newerCart, purchase))
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(events), emptyList())

        whenn()
        fixture.sut()

        then()
        val expected = events.copy(eventList = listOf(newerCart, purchase))
        assertEquals(expected.toRemote().toJson(), fixture.rest.requests.single().body)
        verify(exactly = 1) { fixture.eventsDb.deleteEvents(events) }
    }

    @Test
    fun `should keep sending while events remain`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(
            listOf(stubEventsDb(listOf(stubEventDb(rowId = "1")))),
            listOf(stubEventsDb(listOf(stubEventDb(rowId = "2")))),
            emptyList()
        )

        whenn()
        fixture.sut()

        then()
        assertEquals(2, fixture.rest.requests.size)
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should delete empty event group without sending`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val empty = stubEventsDb(emptyList())
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(empty), emptyList())

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.eventsDb.deleteEvents(empty) }
    }

    @Test
    fun `should abort and keep events when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returns listOf(stubEventsDb())
        fixture.rest.failWith(429)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.eventsDb.deleteEvents(any()) }
    }

    @Test
    fun `should drop rejected events and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(stubEventsDb()), emptyList())
        fixture.rest.failWith(400)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.eventsDb.deleteEvents(any()) }
    }

    @Test
    fun `should stop after limited number of batches`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returns listOf(stubEventsDb())

        whenn()
        fixture.sut()

        then()
        assertEquals(PushEventsStep.MAX_BATCHES, fixture.rest.requests.size)
    }

    @Test
    fun `should access local database on io dispatcher and trace event count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.eventsDb.getEvents(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ load events from local database") })
        assertFalse(fixture.traced.sink.lines.any { it.contains("external-user") })
    }

    @Test
    fun `should trace sent events with count and outcome without response headers`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(stubEventsDb()), emptyList())
        fixture.rest.succeedWith(body = "", headers = mapOf("Set-Cookie" to listOf(SESSION_COOKIE)))

        whenn()
        fixture.sut()

        then()
        val line = fixture.traced.sink.lines.single { it.contains("send events to server") }
        assertTrue(line.contains("✓ send events to server") && line.contains("count=1  ok"))
        assertFalse(line.contains(SESSION_COOKIE) || line.contains("Set-Cookie"))
    }

    @Test
    fun `should trace rejected events with status without response body`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(stubEventsDb()), emptyList())
        fixture.rest.failWith(400, body = REJECTION_BODY)

        whenn()
        fixture.sut()

        then()
        val line = fixture.traced.sink.lines.single { it.contains("send events to server") }
        assertTrue(line.contains("✗ send events to server") && line.contains("count=1  400 non-repeatable"))
        assertFalse(line.contains(REJECTION_BODY))
    }

    @Test
    fun `should trace unreachable server without its host`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returns listOf(stubEventsDb())
        fixture.rest.failWith(UnknownHostException("Unable to resolve host \"$API_HOST\""))

        whenn()
        fixture.sut()

        then()
        val line = fixture.traced.sink.lines.single { it.contains("send events to server") }
        assertTrue(line.contains("✗ send events to server") && line.contains("count=1  no response repeatable"))
        assertFalse(line.contains(API_HOST))
    }

    @Test
    fun `should load events table once per round and send every group from it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = stubEventsDb(listOf(stubEventDb(rowId = "1")))
        val second = stubEventsDb(listOf(stubEventDb(rowId = "2"))).copy(deviceId = "other-device")
        every { fixture.eventsDb.getEvents(any()) } returnsMany listOf(listOf(first, second), emptyList())

        whenn()
        fixture.sut()

        then()
        assertEquals(2, fixture.rest.requests.size)
        verify(exactly = 2) { fixture.eventsDb.getEvents(any()) }
    }

    @Test
    fun `should abort on repeatable failure without sending remaining groups`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.getEvents(any()) } returns listOf(
            stubEventsDb(),
            stubEventsDb().copy(deviceId = "other-device")
        )
        fixture.rest.failWith(503)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        assertEquals(1, fixture.rest.requests.size)
    }

    companion object {
        private const val SESSION_COOKIE = "session=cookie-value"
        private const val REJECTION_BODY = "{\"errors\":[\"ann@example.com\"]}"
        private const val API_HOST = "mobile-api.example.com"
    }
}
