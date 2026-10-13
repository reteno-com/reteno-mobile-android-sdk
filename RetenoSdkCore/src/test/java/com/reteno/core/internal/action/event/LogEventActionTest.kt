package com.reteno.core.internal.action.event

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.fixtures.FailureReport
import com.reteno.core.fixtures.RecordingEventBus
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class LogEventActionTest {

    private class SutFixture(scope: TestScope) {
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val deviceId = stubDeviceId()
        val events = RecordingEventBus()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = LogEventAction(eventsDb, { deviceId }, events.bus, dispatchers, traced.runner)
    }

    @Test
    fun `should insert event for current device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubCustomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        val result = fixture.sut(event)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.eventsDb.insertEvents(EventsDb(fixture.deviceId, event.toDb())) }
    }

    @Test
    fun `should insert event on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var insertedOnIo = false
        every { fixture.eventsDb.insertEvents(any()) } answers { insertedOnIo = fixture.dispatchers.markedIo.isRunningHere }

        whenn()
        fixture.sut(stubCustomEvent())

        then()
        assertTrue(insertedOnIo)
    }

    @Test
    fun `should emit event to event bus after insert`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubCustomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut(event)

        then()
        assertEquals(listOf(event), fixture.events.emitted)
    }

    @Test
    fun `should fail without emitting when insert fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("disk full")
        every { fixture.eventsDb.insertEvents(any()) } throws error

        whenn()
        val result = fixture.sut(stubCustomEvent())

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(fixture.events.emitted.isEmpty())
        assertEquals(listOf(FailureReport("LogEvent", "save event to local database", error)), fixture.traced.reports)
    }

    @Test
    fun `should neither insert nor emit when cancelled before running`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        val job = launch { fixture.sut(stubCustomEvent()) }
        job.cancel()
        runCurrent()

        then()
        assertTrue(job.isCancelled)
        verify(exactly = 0) { fixture.eventsDb.insertEvents(any()) }
        assertTrue(fixture.events.emitted.isEmpty())
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should trace event type and steps without parameter values`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut(stubCustomEvent(typeKey = "purchase"))

        then()
        assertEquals(
            fixture.traced.successfulTrace("LogEvent", "type=purchase", "save event to local database", "notify in-app message triggers"),
            fixture.traced.sink.lines
        )
        assertFalse(fixture.traced.sink.lines.any { it.contains("A-1") })
    }
}
