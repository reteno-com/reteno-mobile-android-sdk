package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithTime
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubEventRule
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubTargeting
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ProcessInAppMessagesActionTest {

    private class SutFixture(scope: TestScope) {
        val state = InAppMessagesStateImpl()
        val sessionHandler = mockk<RetenoSessionHandler>()
        val scheduled = mutableListOf<List<Pair<Long, Long>>>()
        var onTimeMatch: ((List<InAppMessage>) -> Unit)? = null
        val calls = mutableListOf<String>()
        var showFailure: Throwable? = null
        val triggers = object : InAppTriggering {
            override fun listen() = Unit

            override fun openEventGate() {
                calls += "open event gate"
            }

            override fun showDue(messages: List<InAppMessage>) {
                calls += "show due ${messages.map { it.messageId }}"
            }
        }
        val showInApp = mockk<ShowInApp>().also {
            coEvery { it(any(), any()) } answers {
                calls += "show ${firstArg<List<InAppMessage>>().map { message -> message.messageId }} " +
                    "onAppStart=${secondArg<Boolean>()}"
                showFailure?.let { error -> throw error }
                Result.success(true)
            }
        }
        val traced = TestActionRunner(scope)
        val sut = ProcessInAppMessagesAction(
            state = state,
            sessionHandler = sessionHandler,
            triggers = triggers,
            showInApp = showInApp,
            runner = traced.runner
        )

        init {
            every { sessionHandler.scheduleInAppMessages(any(), any()) } answers {
                scheduled += firstArg<List<InAppWithTime>>().map { it.inApp.messageId to it.time }
                onTimeMatch = secondArg()
            }
        }
    }

    @Test
    fun `should show in-apps without targeting on app start`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage(messageId = 1), stubInAppMessage(messageId = 2)))

        then()
        assertTrue("show [1, 2] onAppStart=true" in fixture.calls)
    }

    @Test
    fun `should schedule time spent in-apps in session`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val timed = stubInAppMessage(messageId = 4, targeting = stubTargeting(TargetingRule.TimeSpentInApp(30_000L)))

        whenn()
        fixture.sut(listOf(timed))

        then()
        assertEquals(listOf(listOf(4L to 30_000L)), fixture.scheduled)
    }

    @Test
    fun `should show time spent in-apps when session reaches their time`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val timed = stubInAppMessage(messageId = 4, targeting = stubTargeting(TargetingRule.TimeSpentInApp(30_000L)))
        fixture.sut(listOf(timed))

        whenn()
        fixture.onTimeMatch!!.invoke(listOf(timed))

        then()
        assertTrue("show due [4]" in fixture.calls)
    }

    @Test
    fun `should not schedule anything without time spent in-apps`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage()))

        then()
        verify(exactly = 0) { fixture.sessionHandler.scheduleInAppMessages(any(), any()) }
    }

    @Test
    fun `should make event in-apps wait for their events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val rule = stubEventRule(name = "purchase")
        val waiting = stubInAppMessage(messageId = 6, targeting = stubTargeting(rule))

        whenn()
        fixture.sut(listOf(waiting))

        then()
        assertEquals(listOf(6L to rule), fixture.state.waitingForEvent().map { it.inApp.messageId to it.event })
    }

    @Test
    fun `should keep each targeted in-app off the app start list`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val both = stubInAppMessage(
            messageId = 8,
            targeting = stubTargeting(stubEventRule(), TargetingRule.TimeSpentInApp(1_000L))
        )

        whenn()
        fixture.sut(listOf(both, stubInAppMessage(messageId = 1)))

        then()
        assertTrue("show [1] onAppStart=true" in fixture.calls)
        assertEquals(listOf(8L), fixture.state.waitingForEvent().map { it.inApp.messageId })
        assertEquals(listOf(listOf(8L to 1_000L)), fixture.scheduled)
    }

    @Test
    fun `should open event gate once waiting in-apps are known and before app start in-app`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(listOf(stubInAppMessage(messageId = 1)))

        then()
        assertEquals(listOf("open event gate", "show [1] onAppStart=true"), fixture.calls)
    }

    @Test
    fun `should keep event gate closed when processing is cancelled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.sessionHandler.scheduleInAppMessages(any(), any()) } throws CancellationException("stopped")
        val timed = stubInAppMessage(messageId = 4, targeting = stubTargeting(TargetingRule.TimeSpentInApp(30_000L)))

        whenn()
        val cancelled = runCatching { fixture.sut(listOf(timed)) }.exceptionOrNull()

        then()
        assertTrue(cancelled is CancellationException)
        assertTrue("open event gate" !in fixture.calls)
    }

    @Test
    fun `should open event gate when processing fails before waiting in-apps are known`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.sessionHandler.scheduleInAppMessages(any(), any()) } throws
            IllegalStateException("tracker gone")
        val timed = stubInAppMessage(messageId = 4, targeting = stubTargeting(TargetingRule.TimeSpentInApp(30_000L)))

        whenn()
        val result = fixture.sut(listOf(timed))

        then()
        assertTrue(result.isFailure)
        assertEquals(listOf("open event gate"), fixture.calls)
    }

    @Test
    fun `should leave event gate open when showing app start in-app fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.showFailure = IllegalStateException("display unavailable")

        whenn()
        val result = fixture.sut(listOf(stubInAppMessage(messageId = 1)))

        then()
        assertTrue(result.isFailure)
        assertTrue("open event gate" in fixture.calls)
    }

    @Test
    fun `should trace how messages were sorted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val timed = stubInAppMessage(messageId = 4, targeting = stubTargeting(TargetingRule.TimeSpentInApp(30_000L)))

        whenn()
        fixture.sut(listOf(stubInAppMessage(messageId = 1), timed))

        then()
        val sortLine = fixture.traced.sink.lines.first { it.contains("sort in-app messages by trigger") }
        assertTrue(sortLine.contains("onAppStart=1  events=0  timers=1"))
    }
}
