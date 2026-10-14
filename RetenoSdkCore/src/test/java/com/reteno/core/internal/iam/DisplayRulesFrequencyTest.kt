package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class DisplayRulesFrequencyTest {

    private class SutFixture {
        val sut = DisplayRulesEvaluator(clock = { NOW }, sink = FakeLogSink())

        fun decide(
            rule: FrequencyRule,
            lastShowTime: Long? = null,
            showCount: Long = 0,
            showingOnAppStart: Boolean = false
        ): DisplayDecision {
            val message = stubInAppMessage(frequency = listOf(rule), lastShowTime = lastShowTime, showCount = showCount)
            return sut.decide(message, sessionStartTimestamp = SESSION_START, showingOnAppStart = showingOnAppStart)
        }
    }

    @Test
    fun `should show message with empty frequency rules`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(frequency = emptyList(), lastShowTime = NOW)

        whenn()
        val decision = fixture.sut.decide(message, sessionStartTimestamp = SESSION_START, showingOnAppStart = true)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show unlimited message again outside app start`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.NoLimit, lastShowTime = SESSION_START + 1)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show unlimited message only once per session on app start`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.NoLimit, lastShowTime = SESSION_START + 1, showingOnAppStart = true)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show unlimited message on app start when last shown in previous session`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.NoLimit, lastShowTime = SESSION_START - 1, showingOnAppStart = true)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show unlimited message on app start when never shown`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.NoLimit, showingOnAppStart = true)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show once per app message that was never shown`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.OncePerApp)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show once per app message again`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.OncePerApp, lastShowTime = 1L)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show once per session message that was never shown`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.OncePerSession)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show once per session message last shown in previous session`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.OncePerSession, lastShowTime = SESSION_START - 1)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show once per session message shown at session start`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.OncePerSession, lastShowTime = SESSION_START)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show min interval message that was never shown`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.MinInterval(INTERVAL))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show min interval message once interval has passed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.MinInterval(INTERVAL), lastShowTime = NOW - INTERVAL - 1)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show min interval message exactly one interval after last show`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(FrequencyRule.MinInterval(INTERVAL), lastShowTime = NOW - INTERVAL)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show times per time unit message that was never shown`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(TWICE_PER_MINUTE, lastShowTime = NOW, showCount = 0)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show times per time unit message once shown as many times as allowed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(TWICE_PER_MINUTE, lastShowTime = 1L, showCount = 2)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show times per time unit message again after a time unit`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(TWICE_PER_MINUTE, lastShowTime = NOW - MINUTE - 1, showCount = 1)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show times per time unit message exactly one time unit after last show`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(TWICE_PER_MINUTE, lastShowTime = NOW - MINUTE, showCount = 1)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    @Test
    fun `should show times per time unit message with count but no last show time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(TWICE_PER_MINUTE, lastShowTime = null, showCount = 1)

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should require every frequency rule to match`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val message = stubInAppMessage(
            frequency = listOf(FrequencyRule.NoLimit, FrequencyRule.OncePerApp),
            lastShowTime = 1L
        )

        whenn()
        val decision = fixture.sut.decide(message, sessionStartTimestamp = SESSION_START, showingOnAppStart = false)

        then()
        assertEquals(DisplayDecision.FREQUENCY_LIMIT_REACHED, decision)
    }

    private companion object {
        const val SESSION_START = 1_000_000L
        const val NOW = 2_000_000L
        const val INTERVAL = 10_000L
        val MINUTE = TimeUnit.MINUTES.toMillis(1)
        val TWICE_PER_MINUTE = FrequencyRule.TimesPerTimeUnit(TimeUnit.MINUTES, 2)
    }
}
