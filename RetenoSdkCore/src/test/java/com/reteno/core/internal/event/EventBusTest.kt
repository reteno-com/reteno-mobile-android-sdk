package com.reteno.core.internal.event

import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.event.eventFlow
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EventBusTest {

    private class SutFixture {
        val flow = MutableSharedFlow<Event>()
        val sut = EventBus(flow)
    }

    @Test
    fun `should deliver emitted event to collectors`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val event = Event.ScreenView("Home")
        val received = async { fixture.sut.events.first() }
        runCurrent()

        whenn()
        fixture.sut.emit(event)

        then()
        assertSame(event, received.await())
    }

    @Test
    fun `should use shared event flow by default`() = runUnitTest {
        given()
        val sut = EventBus()

        whenn()
        val events = sut.events

        then()
        assertSame(eventFlow, events)
    }
}
