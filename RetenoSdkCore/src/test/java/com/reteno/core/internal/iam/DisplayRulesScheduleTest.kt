package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubDaysAndTime
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

internal class DisplayRulesScheduleTest {

    private class SutFixture(now: ZonedDateTime = WEDNESDAY_MORNING_IN_PARIS) {
        val sut = DisplayRulesEvaluator(clock = { now.toInstant().toEpochMilli() }, sink = FakeLogSink())

        fun decide(vararg rules: ScheduleRule): DisplayDecision {
            val message = stubInAppMessage(schedule = rules.toList())
            return sut.decide(message, sessionStartTimestamp = 0L, showingOnAppStart = false)
        }
    }

    @Test
    fun `should show message with empty schedule rules`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide()

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show message after its show after date`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(ScheduleRule.ShowAfter(WEDNESDAY_MORNING_IN_PARIS.minusMinutes(1)))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message before its show after date`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(ScheduleRule.ShowAfter(WEDNESDAY_MORNING_IN_PARIS.plusMinutes(1)))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should show message before its hide after date`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(ScheduleRule.HideAfter(WEDNESDAY_MORNING_IN_PARIS.plusMinutes(1)))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message after its hide after date`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(ScheduleRule.HideAfter(WEDNESDAY_MORNING_IN_PARIS.minusMinutes(1)))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should require every schedule rule to match`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(
            ScheduleRule.ShowAfter(WEDNESDAY_MORNING_IN_PARIS.minusMinutes(1)),
            ScheduleRule.HideAfter(WEDNESDAY_MORNING_IN_PARIS.minusMinutes(1))
        )

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should show message on a listed day within its hours`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(
            stubDaysAndTime(days = listOf("monday", "wednesday"), from = 10 to 10, to = 14 to 30)
        )

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message on a day that is not listed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(
            stubDaysAndTime(days = listOf("monday", "friday"), from = 10 to 10, to = 14 to 30)
        )

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should show message on any day when no days are listed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(days = emptyList(), from = 10 to 10, to = 14 to 30))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show message all day when no hours are set`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(days = listOf("wednesday"), from = 0 to 0, to = 0 to 0))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show message from midnight until its end time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 0 to 0, to = 11 to 0))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message from midnight after its end time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 0 to 0, to = 9 to 0))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should not show message before its start time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 10 to 30, to = 14 to 30))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should show message from the first second of its start minute`() = runUnitTest {
        given()
        val fixture = SutFixture(now = WEDNESDAY_MORNING_IN_PARIS.withSecond(0))

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 10 to 28, to = 14 to 30))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show message until the end of its end minute`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 8 to 0, to = 10 to 28))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show message after its end minute`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 8 to 0, to = 10 to 27))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should show overnight message in the morning before its end time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 22 to 0, to = 11 to 0))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should show overnight message in the evening after its start time`() = runUnitTest {
        given()
        val fixture = SutFixture(now = WEDNESDAY_MORNING_IN_PARIS.withHour(23))

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 22 to 0, to = 2 to 0))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should not show overnight message between its end and start time`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(from = 14 to 10, to = 10 to 20))

        then()
        assertEquals(DisplayDecision.OUTSIDE_SCHEDULE, decision)
    }

    @Test
    fun `should check hours in the time zone of the rule`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val decision = fixture.decide(stubDaysAndTime(timeZone = "Asia/Tokyo", from = 17 to 0, to = 18 to 0))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should check day of week in the time zone of the rule`() = runUnitTest {
        given()
        val fixture = SutFixture(now = WEDNESDAY_MORNING_IN_PARIS.withHour(20))

        whenn()
        val decision = fixture.decide(stubDaysAndTime(timeZone = "Asia/Tokyo", days = listOf("thursday")))

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    @Test
    fun `should check hours in device time zone when rule time zone is unknown`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val deviceTime = WEDNESDAY_MORNING_IN_PARIS.withZoneSameInstant(ZoneId.systemDefault())

        whenn()
        val decision = fixture.decide(
            stubDaysAndTime(timeZone = "Not/AZone", from = deviceTime.hour to 0, to = deviceTime.hour to 59)
        )

        then()
        assertEquals(DisplayDecision.SHOW, decision)
    }

    private companion object {
        val WEDNESDAY_MORNING_IN_PARIS: ZonedDateTime =
            ZonedDateTime.parse("2024-04-03T10:28:52+02:00[Europe/Paris]")
    }
}
