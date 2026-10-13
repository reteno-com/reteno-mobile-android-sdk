package com.reteno.core.di

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubRetenoLogEvent
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ActionsLocatorTest {

    private class SutFixture(scope: TestScope) {
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val logEventsDb = mockk<RetenoDatabaseManagerLogEvent>()
        val deviceId = stubDeviceId()
        val sink = FakeLogSink()
        val remoteLogs = mutableListOf<String>()
        private val serviceLocator = mockk<ServiceLocator>().also {
            every { it.retenoDatabaseManagerEventsProvider.get() } returns eventsDb
            every { it.retenoDatabaseManagerLogEventProvider.get() } returns logEventsDb
            every { it.restConfigProvider.get().deviceId } returns deviceId
        }
        val sut = ActionsLocator(serviceLocator, scope.testDispatchers(), sink) { remoteLogs += it }
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
        verify(exactly = 1) { fixture.eventsDb.insertEvents(match { it.eventList.single().eventTypeKey == "screenView" }) }
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
    fun `should create each action once`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val first = listOf(fixture.sut.logEvent, fixture.sut.logEcomEvent, fixture.sut.logScreenView, fixture.sut.logRetenoEvent)
        val second = listOf(fixture.sut.logEvent, fixture.sut.logEcomEvent, fixture.sut.logScreenView, fixture.sut.logRetenoEvent)

        then()
        first.zip(second).forEach { (a, b) -> assertSame(a, b) }
    }

    @Test
    fun `should send one redacted remote log when an action fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.eventsDb.insertEvents(any()) } throws IOException("cannot write https://mobile-api.reteno.com/x")

        whenn()
        fixture.sut.logEvent(stubCustomEvent())

        then()
        assertEquals(listOf("Action LogEvent failed at step 'save event to local database': IOException: cannot write <url>"), fixture.remoteLogs)
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
