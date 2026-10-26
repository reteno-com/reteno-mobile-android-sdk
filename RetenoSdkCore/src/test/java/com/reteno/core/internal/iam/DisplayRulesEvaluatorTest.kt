package com.reteno.core.internal.iam

import com.google.gson.JsonNull
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubInAppContent
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubSegment
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class DisplayRulesEvaluatorTest {

    private class SutFixture {
        val sut = DisplayRulesEvaluator(clock = { NOW }, sink = FakeLogSink())

        fun decide(message: InAppMessage) =
            sut.decide(message, sessionStartTimestamp = SESSION_START, showingOnAppStart = false)
    }

    @Test
    fun `should show message without display rules`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage()

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message without content`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(content = null)

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.NO_CONTENT, decision)
    }

    @Test
    fun `should not show message with empty content model`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(content = stubInAppContent(model = JsonNull.INSTANCE))

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.NO_CONTENT, decision)
    }

    @Test
    fun `should show message with async rules but no segment`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(async = AsyncDisplayRules(segment = null))

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show message to user in its segment`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(async = AsyncDisplayRules(segment = stubSegment(inSegment = true)))

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message to user outside its segment`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(async = AsyncDisplayRules(segment = stubSegment(inSegment = false)))

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.NOT_IN_SEGMENT, decision)
    }

    @Test
    fun `should report missing content before segment`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(
            content = null,
            async = AsyncDisplayRules(segment = stubSegment(inSegment = false))
        )

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.NO_CONTENT, decision)
    }

    @Test
    fun `should report segment before frequency limit`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(
            async = AsyncDisplayRules(segment = stubSegment(inSegment = false)),
            frequency = listOf(FrequencyRule.OncePerApp),
            lastShowTime = SESSION_START
        )

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.NOT_IN_SEGMENT, decision)
    }

    @Test
    fun `should report frequency limit before schedule`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(
            frequency = listOf(FrequencyRule.OncePerApp),
            schedule = listOf(ScheduleRule.ShowAfter(ZonedDateTime.parse("2999-01-01T00:00:00Z"))),
            lastShowTime = SESSION_START
        )

        whenn()
        val decision = fixture.decide(message)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    private companion object {
        const val SESSION_START = 1_000_000L
        const val NOW = 2_000_000L
    }
}
