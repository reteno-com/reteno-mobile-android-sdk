package com.reteno.core.data.remote.model.iam.displayrules.schedule

import com.google.gson.JsonObject
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime

class ScheduleRuleValidatorTest : BaseRobolectricTest() {
    @Test
    fun givenInAppShowAfter_whenTimeAfter_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(ScheduleRule.ShowAfter(ZonedDateTime.now()))
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
    }

    @Test
    fun givenInAppShowAfter_whenTimeBefore_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(ScheduleRule.ShowAfter(ZonedDateTime.now().plusDays(1)))
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
    }

    @Test
    fun givenInAppShowAfter_whenTimeBefore_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(ScheduleRule.HideAfter(ZonedDateTime.now().plusDays(1)))
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
    }

    @Test
    fun givenInAppShowAfter_whenTimeAfter_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(ScheduleRule.HideAfter(ZonedDateTime.now().minusDays(1)))
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenIncorrectDayOfWeek_thenFalse() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-03T10:28:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 10,
                            toHours = 14,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenCorrectDayOfWeek_thenTrue() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T10:28:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 10,
                            toHours = 14,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenIncorrectHours_thenFalse() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T08:28:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 10,
                            toHours = 14,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenIncorrectMinutes_thenFalse() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T08:08:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 10,
                            toHours = 14,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenEndHoursBeforeStartHoursAndInRange_thenTrue() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T08:28:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 14,
                            toHours = 10,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenEndHoursBeforeStartHoursAndNotInRange_thenFalse() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T11:28:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 14,
                            toHours = 10,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenEndHoursBeforeStartHoursAndInRangeEdgeEnd_thenTrue() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T09:30:52+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 14,
                            toHours = 10,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenEndHoursBeforeStartHoursAndInRangeEdgeStart_thenTrue() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T13:10:00+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 14,
                            toHours = 10,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(true, result)
        unmockkStatic(ZonedDateTime::class)
    }

    @Test
    fun givenInAppSpecificDaysAndTime_whenEndHoursBeforeStartHoursAndNotInRangeEdgeStart_thenFalse() {
        //Given
        val sut = createValidator()
        val mocked = ZonedDateTime.parse("2024-04-01T13:09:59+01:00[Europe/Paris]")
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns mocked
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = ScheduleDisplayRules().apply {
                    predicates.add(
                        ScheduleRule.SpecificDaysAndTime(
                            timeZone = "Europe/Paris",
                            daysOfWeek = listOf(
                                DayOfWeek.MONDAY.name.lowercase(),
                                DayOfWeek.FRIDAY.name.lowercase()
                            ),
                            fromHours = 14,
                            toHours = 10,
                            fromMinutes = 10,
                            toMinutes = 30
                        )
                    )
                },
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesScheduleRules(
            inAppMessage = inApp
        )
        //Then
        Assert.assertEquals(false, result)
        unmockkStatic(ZonedDateTime::class)
    }


    private fun createValidator() = ScheduleRuleValidator()
}