package com.reteno.core.di

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.fixtures.ActionGraphFixture
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionGraphTest {

    private class SutFixture(scope: TestScope) {
        private val locator = ActionGraphFixture(scope)
        val eventsDb = locator.eventsDb
        val logEventsDb = locator.logEventsDb
        val deviceId = locator.deviceId
        val sink = locator.sink
        val remoteLogs: List<String?>
            get() = locator.remoteLogs.map { it.first }
        val graph = locator.graph
    }

    @Test
    fun `should wire log event action to events database and current device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubCustomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.graph.events.logEvent(event)

        then()
        verify(exactly = 1) { fixture.eventsDb.insertEvents(EventsDb(fixture.deviceId, event.toDb())) }
        assertTrue(fixture.sink.lines.first().contains("LogEvent"))
    }

    @Test
    fun `should wire ecommerce action to events database`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubEcomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.graph.events.logEcomEvent(event)

        then()
        verify(exactly = 1) { fixture.eventsDb.insertEvents(EventsDb(fixture.deviceId, event.toDb())) }
    }

    @Test
    fun `should wire screen view action through log event action`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.graph.events.logScreenView("Home")

        then()
        verify(exactly = 1) {
            fixture.eventsDb.insertEvents(match { it.eventList.single().eventTypeKey == "screenView" })
        }
    }

    @Test
    fun `should wire reteno log event action to log events database`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubRetenoLogEvent()
        justRun { fixture.logEventsDb.insertLogEvent(any()) }

        whenn()
        fixture.graph.events.logRetenoEvent(event)

        then()
        verify(exactly = 1) { fixture.logEventsDb.insertLogEvent(event.toDb()) }
    }

    @Test
    fun `should send one redacted remote log when an action fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val failure = IOException("cannot write https://mobile-api.reteno.com/x")
        every { fixture.eventsDb.insertEvents(any()) } throws failure

        whenn()
        fixture.graph.events.logEvent(stubCustomEvent())
        runCurrent()

        then()
        val expected = "Action LogEvent failed at step 'save event to local database': IOException: cannot write <url>"
        assertEquals(listOf(expected), fixture.remoteLogs)
    }

    @Test
    fun `should not send remote log when log event insert fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.logEventsDb.insertLogEvent(any()) } throws IOException("locked")

        whenn()
        fixture.graph.events.logRetenoEvent(stubRetenoLogEvent())

        then()
        assertTrue(fixture.remoteLogs.isEmpty())
    }

    @Test
    fun `should save messages logged through logger once logging is installed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        try {
            fixture.graph.installLogging()
            Logger.captureMessage("cache cleared = 3 entries")
            runCurrent()
        } finally {
            Logger.install(AndroidLogSink(), null)
        }

        then()
        assertEquals(listOf("cache cleared"), fixture.remoteLogs)
    }
}
