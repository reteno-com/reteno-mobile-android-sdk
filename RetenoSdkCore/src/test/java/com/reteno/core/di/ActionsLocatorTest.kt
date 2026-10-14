package com.reteno.core.di

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ActionsLocatorTest {

    private class SutFixture(scope: TestScope) {
        private val locator = LocatorFixture(scope)
        val eventsDb = locator.eventsDb
        val logEventsDb = locator.logEventsDb
        val deviceId = locator.deviceId
        val sink = locator.sink
        val remoteLogs: List<String>
            get() = locator.remoteLogs.map { it.first }
        val sut = locator.sut
    }

    @Test
    fun `should wire log event action to events database and current device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubCustomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut.logEvent(event)

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
        fixture.sut.logEcomEvent(event)

        then()
        verify(exactly = 1) { fixture.eventsDb.insertEvents(EventsDb(fixture.deviceId, event.toDb())) }
    }

    @Test
    fun `should wire screen view action through log event action`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut.logScreenView("Home")

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
        fixture.sut.logRetenoEvent(event)

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
        fixture.sut.logEvent(stubCustomEvent())

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
        fixture.sut.logRetenoEvent(stubRetenoLogEvent())

        then()
        assertTrue(fixture.remoteLogs.isEmpty())
    }
}
