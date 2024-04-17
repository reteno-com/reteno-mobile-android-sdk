package com.reteno.core.data.remote.model.iam.displayrules.frequency

import com.google.gson.JsonObject
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit


class FrequencyRuleValidatorTest : BaseRobolectricTest() {

    @Test
    fun givenInAppOncePerSession_whenLastShowTimeNull_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerSession)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppOncePerSession_whenLastShowTimeBeforeSessionStartTime_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerSession)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() - 10,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppOncePerSession_whenLastShowTimeAfterSessionStartTime_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerSession)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() + 10,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInAppOncePerApp_whenLastShowTimeNull_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerApp)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppOncePerApp_whenLastShowTimeNotNull_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.OncePerApp)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis(),
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInAppNoLimit_whenLastShowTimeNotNull_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.NoLimit)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis(),
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppNoLimit_whenLastShowTimeNull_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.NoLimit)
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppMinInterval_whenLastShowTimeLargerThanInterval_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.MinInterval(2000L))
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() - 2001,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInAppMinInterval_whenLastShowTimeLessThanInterval_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.MinInterval(2000L))
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() - 200,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInAppTimesPerTimeUnit_whenCountLargerThanInTheRule_thenFalse() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.TimesPerTimeUnit(TimeUnit.MINUTES, 2))
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() - 200,
            showCount = 2,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInAppTimesPerTimeUnit_whenCountLessThanInTheRuleAndInTimeInterval_thenTrue() {
        //Given
        val sut = createValidator()
        val inApp = InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = FrequencyDisplayRules().apply {
                    predicates.add(FrequencyRule.TimesPerTimeUnit(TimeUnit.MINUTES, 2))
                },
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = System.currentTimeMillis() - 60000L,
            showCount = 1,
            displayRulesJson = JsonObject()
        )
        //When
        val result = sut.checkInAppMatchesFrequencyRules(
            inAppMessage = inApp,
            sessionStartTimestamp = System.currentTimeMillis(),
            showingOnAppStart = false
        )
        //Then
        assertEquals(true, result)
    }

    private fun createValidator() = FrequencyRuleValidator()

}