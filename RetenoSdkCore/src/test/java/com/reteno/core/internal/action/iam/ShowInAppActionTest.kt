package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.stubDeliveryMessage
import com.reteno.core.fixtures.RecordingInAppDisplay
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.core.internal.iam.InAppDisplayRules
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.fixtures.deliveryState
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ShowInAppActionTest {

    private class SutFixture(scope: TestScope) {
        val rules = mockk<InAppDisplayRules>()
        val state = InAppMessagesStateImpl()
        val checkSegments = mockk<CheckInAppSegments>()
        val sessionHandler = mockk<RetenoSessionHandler>()
        val display = RecordingInAppDisplay()
        val shown = display.shown
        val decided = mutableListOf<Triple<Long, Long, Boolean>>()
        val lock = Mutex()
        val traced = TestActionRunner(scope)
        val rest = FakeRestClient()
        val delivery = deliveryState(scope.testDispatchers(), rest, clock = { scope.testScheduler.currentTime })
        val pause = PauseInAppMessagesAction(state, display, traced.runner)
        val sut = ShowInAppAction(
            rules = rules,
            state = state,
            checkSegments = checkSegments,
            sessionHandler = sessionHandler,
            display = display,
            lock = lock,
            runner = traced.runner,
            delivery = delivery
        )

        init {
            every { sessionHandler.getSessionStartTimestamp() } returns SESSION_START
            every { rules.decide(any(), any(), any()) } answers {
                decided += Triple(firstArg<InAppMessage>().messageId, secondArg(), thirdArg())
                DisplayDecision.SHOW
            }
            coEvery { checkSegments(any(), any()) } returns Result.success(Unit)
        }

        fun allowOnly(messageId: Long) {
            every { rules.decide(any(), any(), any()) } answers {
                if (firstArg<InAppMessage>().messageId == messageId) {
                    DisplayDecision.SHOW
                } else {
                    DisplayDecision.FREQUENCY_LIMIT_REACHED
                }
            }
        }
    }

    @Test
    fun `should trace that display skipped the in-app because another one is on screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.display.offer = DisplayOffer.SKIPPED

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        val displayLine = fixture.traced.sink.lines.first { it.contains("✓ pass in-app message to display") }
        assertTrue(displayLine.contains("skipped, another in-app message is on screen"))
    }

    @Test
    fun `should trace that display waits for the app to come to foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.display.offer = DisplayOffer.WAITING_FOR_APP

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        val displayLine = fixture.traced.sink.lines.first { it.contains("✓ pass in-app message to display") }
        assertTrue(displayLine.contains("waiting for the app to come to foreground"))
    }

    @Test
    fun `should trace that display started loading the in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        val displayLine = fixture.traced.sink.lines.first { it.contains("✓ pass in-app message to display") }
        assertTrue(displayLine.contains("loading"))
    }

    @Test
    fun `should show candidate with highest message id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val shown = fixture.sut(CANDIDATES, showingOnAppStart = true).getOrThrow()

        then()
        assertTrue(shown)
        assertEquals(listOf(3L), fixture.shown)
    }

    @Test
    fun `should choose higher backend priority before message id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val preferred = stubInAppMessage(messageId = 1).copy(priority = 20)
        val ordinary = stubInAppMessage(messageId = 99).copy(priority = 10)

        whenn()
        fixture.sut(listOf(ordinary, preferred), showingOnAppStart = true)

        then()
        assertEquals(listOf(1L), fixture.shown)
    }

    @Test
    fun `should fall back when higher priority candidate fails display rules`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.allowOnly(messageId = 99)
        val preferred = stubInAppMessage(messageId = 1).copy(priority = 20)
        val ordinary = stubInAppMessage(messageId = 99).copy(priority = 10)

        whenn()
        fixture.sut(listOf(preferred, ordinary), showingOnAppStart = true)

        then()
        assertEquals(listOf(99L), fixture.shown)
    }

    @Test
    fun `should show next candidate when display rules reject the newer one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.allowOnly(messageId = 2)

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        assertEquals(listOf(2L), fixture.shown)
    }

    @Test
    fun `should show nothing when display rules reject every candidate`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.allowOnly(messageId = 99)

        whenn()
        val shown = fixture.sut(CANDIDATES, showingOnAppStart = true).getOrThrow()

        then()
        assertFalse(shown)
        assertTrue(fixture.shown.isEmpty())
    }

    @Test
    fun `should show nothing without candidates`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val shown = fixture.sut(emptyList(), showingOnAppStart = false).getOrThrow()

        then()
        assertFalse(shown)
        coVerify(exactly = 0) { fixture.checkSegments(any(), any()) }
    }

    @Test
    fun `should decide with session start and app start flag`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage(messageId = 5)), showingOnAppStart = true)

        then()
        assertEquals(listOf(Triple(5L, SESSION_START, true)), fixture.decided)
    }

    @Test
    fun `should check and save segments before showing triggered candidates`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = false)

        then()
        coVerify(exactly = 1) { fixture.checkSegments(CANDIDATES, true) }
    }

    @Test
    fun `should not check segments of candidates shown on app start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        coVerify(exactly = 0) { fixture.checkSegments(any(), any()) }
    }

    @Test
    fun `should postpone chosen candidate instead of showing it while paused`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.state.setPaused(true)

        whenn()
        val shown = fixture.sut(CANDIDATES, showingOnAppStart = true).getOrThrow()

        then()
        assertTrue(shown)
        assertTrue(fixture.shown.isEmpty())
        assertEquals(3L, fixture.state.setPaused(false)?.message?.messageId)
    }

    @Test
    fun `should show one in-app at a time`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.lock.lock()
        val waiting = async { fixture.sut(CANDIDATES, showingOnAppStart = true) }
        runCurrent()
        val shownWhileLocked = fixture.shown.toList()

        whenn()
        fixture.lock.unlock()
        waiting.await()

        then()
        assertTrue(shownWhileLocked.isEmpty())
        assertEquals(listOf(3L), fixture.shown)
    }

    @Test
    fun `should check segments without waiting for another in-app decision`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.lock.lock()
        val waiting = async { fixture.sut(CANDIDATES, showingOnAppStart = false) }

        whenn()
        runCurrent()

        then()
        coVerify(exactly = 1) { fixture.checkSegments(CANDIDATES, true) }
        assertTrue(fixture.shown.isEmpty())
        fixture.lock.unlock()
        waiting.await()
    }

    @Test
    fun `should trace why candidates were skipped`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.allowOnly(messageId = 1)

        whenn()
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("→ message 3: FREQUENCY_LIMIT_REACHED") })
        assertTrue(lines.any { it.contains("→ message 1: SHOW") })
    }

    @Test
    fun `should choose next eligible candidate when postponed server delivery expires`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val preferred = stubDeliveryMessage(now = testScheduler.currentTime)
        val ordinary = stubInAppMessage(messageId = 99)
        fixture.delivery.remember(listOf(preferred))
        fixture.pause(paused = true)
        fixture.sut(listOf(preferred, ordinary), showingOnAppStart = true)

        whenn()
        advanceTimeBy(3_600_001)
        fixture.pause(paused = false)

        then()
        assertEquals(listOf(99L), fixture.shown)
    }

    @Test
    fun `should retain candidate fallback when postponed display is rejected after resume`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val preferred = stubDeliveryMessage(now = testScheduler.currentTime)
        val ordinary = stubInAppMessage(messageId = 99)
        fixture.delivery.remember(listOf(preferred))
        fixture.pause(paused = true)
        fixture.sut(listOf(preferred, ordinary), showingOnAppStart = true)

        whenn()
        fixture.pause(paused = false)
        fixture.display.onRejected?.invoke()

        then()
        assertEquals(listOf(preferred.messageId, 99L), fixture.shown)
    }

    @Test
    fun `should discard postponed candidates after account changes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.pause(paused = true)
        fixture.sut(CANDIDATES, showingOnAppStart = true)

        whenn()
        fixture.rest.context = "another-account"
        fixture.pause(paused = false)

        then()
        assertTrue(fixture.shown.isEmpty())
    }

    private companion object {
        const val SESSION_START = 42L
        val CANDIDATES = listOf(
            stubInAppMessage(messageId = 1),
            stubInAppMessage(messageId = 3),
            stubInAppMessage(messageId = 2)
        )
    }
}
