package com.reteno.core.internal.action.event

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.domain.model.event.Event
import com.reteno.core.fixtures.RecordingEventBus
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class LogScreenViewEventActionTest {

    private class SutFixture(scope: TestScope) {
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val inserted = mutableListOf<EventsDb>()
        val events = RecordingEventBus()
        val traced = TestActionRunner(scope)
        private val restConfig = mockk<RestConfig>().also { every { it.deviceId } returns stubDeviceId() }
        private val logEvent = LogEventAction(eventsDb, restConfig, events.bus, scope.testDispatchers(), traced.runner)
        val sut = LogScreenViewEventAction(logEvent)

        init {
            every { eventsDb.insertEvents(capture(inserted)) } returns Unit
        }
    }

    @Test
    fun `should insert screen view event with screen name parameter`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("Checkout")

        then()
        assertTrue(result.isSuccess)
        val stored = fixture.inserted.single().eventList.single()
        assertEquals(Event.SCREEN_VIEW_EVENT_TYPE_KEY, stored.eventTypeKey)
        assertEquals(listOf(ParameterDb(Event.SCREEN_VIEW_PARAM_NAME, "Checkout")), stored.params)
    }

    @Test
    fun `should emit the same screen view event that was stored`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("Checkout")

        then()
        val emitted = fixture.events.emitted.single() as Event.ScreenView
        assertEquals("Checkout", emitted.screenName)
        assertEquals(fixture.inserted.single().eventList.single().eventTypeKey, emitted.eventTypeKey)
    }

    @Test
    fun `should return failure of underlying log event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("disk full")
        every { fixture.eventsDb.insertEvents(any()) } throws error

        whenn()
        val result = fixture.sut("Checkout")

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(1, fixture.traced.reports.size)
    }
}
