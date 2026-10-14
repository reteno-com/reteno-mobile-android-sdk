package com.reteno.core.di

import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.InAppWithEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.LocatorFixture
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
internal class ActionsLocatorIamWiringTest {

    @Test
    fun `should evaluate in-app display rules with sdk clock`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val shownJustBeforeSdkNow = stubInAppMessage(
            frequency = listOf(FrequencyRule.MinInterval(500L)),
            lastShowTime = 600L
        )

        whenn()
        val decision = fixture.sut.inAppDisplayRules.decide(
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
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.inAppMessagesState.setPaused(true)

        then()
        assertTrue(fixture.sut.inAppMessagesState.isPaused)
    }

    @Test
    fun `should pause in-app messages submitted by host`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitInAppPause(true)
        runCurrent()

        then()
        assertTrue(fixture.sut.inAppMessagesState.isPaused)
    }

    @Test
    fun `should drop postponed in-app when host chose to skip them`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.sut.inAppMessagesState.setPaused(true)
        fixture.sut.inAppMessagesState.postponeIfPaused(stubInAppMessage())

        whenn()
        fixture.sut.submitInAppPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
        fixture.sut.submitInAppPause(false)
        runCurrent()

        then()
        verify(exactly = 0) { fixture.iamController.show(any()) }
    }

    @Test
    fun `should save show of displayed in-app message`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitInAppShown(stubInAppMessage(messageId = 7))
        runCurrent()

        then()
        verify(exactly = 1) { fixture.inAppDb.insertInAppMessages(match { it.single().showCount == 1L }) }
    }

    @Test
    fun `should show waiting in-app when its event is logged`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val waiting = stubInAppMessage(messageId = 5)
        fixture.sut.inAppMessagesState.replaceWaitingForEvent(
            listOf(InAppWithEvent(waiting, stubEventRule(name = "purchase")))
        )
        fixture.sut.inAppTriggers.listen()
        fixture.sut.inAppTriggers.openEventGate()

        whenn()
        fixture.sut.logEvent(stubCustomEvent(typeKey = "purchase"))
        runCurrent()

        then()
        verify(exactly = 1) { fixture.iamController.show(waiting) }
    }
}
