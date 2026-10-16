package com.reteno.core.di

import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.ActionGraphFixture
import com.reteno.core.fixtures.RecordingInAppLifecycleCallback
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEventRule
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionGraphIamWiringTest {

    @Test
    fun `should evaluate in-app display rules with sdk clock`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        val shownJustBeforeSdkNow = stubInAppMessage(
            frequency = listOf(FrequencyRule.MinInterval(500L)),
            lastShowTime = 600L
        )

        whenn()
        val decision = fixture.graph.inApp.inAppDisplayRules.decide(
            message = shownJustBeforeSdkNow,
            sessionStartTimestamp = 0L,
            showingOnAppStart = false
        )

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should keep in-app pause state between calls`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)

        whenn()
        fixture.graph.inApp.inAppMessagesState.setPaused(true)

        then()
        assertTrue(fixture.graph.inApp.inAppMessagesState.isPaused)
    }

    @Test
    fun `should pause in-app messages submitted by host`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)

        whenn()
        fixture.sut.submitInAppPause(true)
        runCurrent()

        then()
        assertTrue(fixture.graph.inApp.inAppMessagesState.isPaused)
    }

    @Test
    fun `should drop postponed in-app when host chose to skip them`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        fixture.graph.inApp.iamView.start()
        fixture.graph.inApp.inAppMessagesState.setPaused(true)
        fixture.graph.inApp.inAppMessagesState.postponeIfPaused(stubInAppMessage())

        whenn()
        fixture.sut.submitInAppPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.sut.submitInAppPause(false)
        runCurrent()

        then()
        assertTrue(fixture.platform.screen.calls.isEmpty())
    }

    @Test
    fun `should save show of displayed in-app message`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)

        whenn()
        fixture.sut.submitInAppShown(stubInAppMessage(messageId = 7))
        runCurrent()

        then()
        verify(exactly = 1) { fixture.inAppDb.insertInAppMessages(match { it.single().showCount == 1L }) }
    }

    @Test
    fun `should show waiting in-app when its event is logged`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        val waiting = stubInAppMessage(messageId = 5)
        val host = RecordingInAppLifecycleCallback(fixture.dispatchers.markedMain)
        fixture.graph.inApp.iamView.setInAppLifecycleCallback(host)
        fixture.graph.inApp.iamView.start()
        fixture.graph.inApp.inAppMessagesState.replaceWaitingForEvent(
            listOf(InAppWithEvent(waiting, stubEventRule(name = "purchase")))
        )
        fixture.graph.inApp.inAppTriggers.listen()
        fixture.graph.inApp.inAppTriggers.openEventGate()

        whenn()
        fixture.graph.events.logEvent(stubCustomEvent(typeKey = "purchase"))
        runCurrent()

        then()
        assertEquals(listOf("beforeDisplay DISPLAY_RULES 5"), host.calls)
    }

    @Test
    fun `should record show and opened interaction once in-app widget is displayed`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        fixture.graph.inApp.iamView.start()
        fixture.graph.inApp.iamView.initialize(stubInAppMessage(messageId = 7))
        runCurrent()

        whenn()
        fixture.platform.screen.postWidgetMessage("""{"type":"WIDGET_INIT_SUCCESS"}""")
        runCurrent()

        then()
        verify(exactly = 1) { fixture.inAppDb.insertInAppMessages(match { it.single().showCount == 1L }) }
        verify(exactly = 1) { fixture.inAppInteractionsDb.insertInteraction(match { it.status == "OPENED" }) }
    }

    @Test
    fun `should skip push in-apps when host pauses them with skip behaviour`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        fixture.sut.submitPushInAppPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.sut.submitPushInAppPause(true)

        whenn()
        fixture.sut.submitIamFromPush("interaction-1")
        runCurrent()

        then()
        assertTrue(fixture.platform.screen.calls.isEmpty())
    }

    @Test
    fun `should tell lifecycle callback set by host about in-app display`() = runUnitTest {
        given()
        val fixture = ActionGraphFixture(this)
        val host = RecordingInAppLifecycleCallback(fixture.dispatchers.markedMain)
        fixture.sut.submitInAppLifecycleCallback(host)

        whenn()
        fixture.sut.submitIamFromPush("interaction-1")
        runCurrent()

        then()
        assertEquals(listOf("beforeDisplay PUSH_NOTIFICATION interaction-1"), host.calls)
    }
}
