package com.reteno.core.di

import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.internal.iam.DisplayDecision
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
}
