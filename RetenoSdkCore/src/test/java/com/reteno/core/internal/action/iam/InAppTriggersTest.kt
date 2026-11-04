package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEventRule
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubInAppWithEvent
import com.reteno.core.internal.event.EventBus
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppTriggersTest {

    private class SutFixture(scope: TestScope) {
        val events = MutableSharedFlow<Event>(extraBufferCapacity = 100)
        val sink = FakeLogSink()
        val state = InAppMessagesStateImpl()
        val rules = mockk<InAppDisplayRules>()
        val shown = mutableListOf<Pair<List<Long>, Boolean>>()
        val showInApp = mockk<ShowInApp>().also {
            coEvery { it(any(), any()) } answers {
                shown += firstArg<List<InAppMessage>>().map { message -> message.messageId } to secondArg<Boolean>()
                Result.success(true)
            }
        }
        val sut = InAppTriggers(
            eventBus = EventBus(events),
            state = state,
            rules = rules,
            showInApp = lazy { showInApp },
            sink = sink,
            scope = scope.workScope()
        )

        init {
            every { rules.matchesEvent(any(), any()) } answers {
                firstArg<TargetingRule.Event>().name == secondArg<Event>().eventTypeKey
            }
            state.replaceWaitingForEvent(
                listOf(
                    stubInAppWithEvent(messageId = 1, rule = stubEventRule(name = "purchase")),
                    stubInAppWithEvent(messageId = 2, rule = stubEventRule(name = "refund")),
                    stubInAppWithEvent(messageId = 3, rule = stubEventRule(name = "purchase"))
                )
            )
        }
    }

    @Test
    fun `should show in-apps waiting for the event once gate is open`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.listen()
        fixture.sut.openEventGate()

        whenn()
        fixture.events.emit(stubCustomEvent(typeKey = "purchase"))
        runCurrent()

        then()
        assertEquals(listOf(listOf(1L, 3L) to false), fixture.shown)
    }

    @Test
    fun `should hold events until gate opens`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.listen()
        fixture.events.emit(stubCustomEvent(typeKey = "refund"))
        runCurrent()
        val shownBeforeGate = fixture.shown.toList()

        whenn()
        fixture.sut.openEventGate()
        runCurrent()

        then()
        assertTrue(shownBeforeGate.isEmpty())
        assertEquals(listOf(listOf(2L) to false), fixture.shown)
    }

    @Test
    fun `should ignore events no in-app waits for`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.listen()
        fixture.sut.openEventGate()

        whenn()
        fixture.events.emit(stubCustomEvent(typeKey = "login"))
        runCurrent()

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should listen to events only once`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.listen()
        fixture.sut.listen()
        fixture.sut.openEventGate()

        whenn()
        fixture.events.emit(stubCustomEvent(typeKey = "refund"))
        runCurrent()

        then()
        assertEquals(1, fixture.shown.size)
    }

    @Test
    fun `should keep reacting to events after one fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.rules.matchesEvent(any(), match { it.eventTypeKey == "broken" }) } throws
            IllegalStateException("bad rule")
        fixture.sut.listen()
        fixture.sut.openEventGate()

        whenn()
        fixture.events.emit(stubCustomEvent(typeKey = "broken"))
        fixture.events.emit(stubCustomEvent(typeKey = "refund"))
        runCurrent()

        then()
        assertEquals(listOf(listOf(2L) to false), fixture.shown)
        assertTrue(fixture.sink.lines.single().contains("in-app event trigger failed: IllegalStateException: bad rule"))
    }

    @Test
    fun `should stop reacting to events when cancelled while handling one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.rules.matchesEvent(any(), match { it.eventTypeKey == "stop" }) } throws
            CancellationException("stopped")
        fixture.sut.listen()
        fixture.sut.openEventGate()

        whenn()
        fixture.events.emit(stubCustomEvent(typeKey = "stop"))
        runCurrent()
        fixture.events.emit(stubCustomEvent(typeKey = "refund"))
        runCurrent()

        then()
        assertTrue(fixture.shown.isEmpty())
        assertTrue(fixture.sink.lines.isEmpty())
    }

    @Test
    fun `should drop the oldest events while gate is closed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.listen()
        fixture.events.emit(stubCustomEvent(typeKey = "refund"))
        repeat(InAppTriggers.MAX_PENDING_EVENTS) { fixture.events.emit(stubCustomEvent(typeKey = "login")) }
        runCurrent()

        whenn()
        fixture.sut.openEventGate()
        runCurrent()

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should show in-apps whose session time has come`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.showDue(listOf(stubInAppMessage(messageId = 9)))
        runCurrent()

        then()
        assertEquals(listOf(listOf(9L) to false), fixture.shown)
    }
}
