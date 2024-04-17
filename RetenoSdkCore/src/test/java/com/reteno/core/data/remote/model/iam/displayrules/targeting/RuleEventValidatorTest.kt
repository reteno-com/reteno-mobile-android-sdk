package com.reteno.core.data.remote.model.iam.displayrules.targeting

import com.google.gson.JsonObject
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.domain.model.event.Event
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEventValidatorTest : BaseRobolectricTest() {

    @Test
    fun givenInApp_whenEventNameEmpty_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(TargetingRule.Event("", RuleRelation.OR))
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithEqualsOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.EQUALS,
                        values = listOf("some")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithEqualsOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.EQUALS,
                        values = listOf("some")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithContainsOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenNoRelationAndWithContainsOperator_thenFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_lon"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithContainsOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_lon"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithEndsWithOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.ENDS_WITH,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.ENDS_WITH,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithEndsWithOperator_thenAlwaysFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.ENDS_WITH,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.ENDS_WITH,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithStartsWithAndEndsWithOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.STARTS_WITH,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.ENDS_WITH,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithStartsWithOperator_thenAlwaysFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.STARTS_WITH,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.STARTS_WITH,
                        values = listOf("long")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithCONTAINS_ONE_OFOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("digital", "test")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithCONTAINS_ONE_OFOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("some")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("digital", "test")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithCONTAINS_ONE_OFOperator_thenFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("non_existent")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("digital", "test")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithCONTAINS_ONE_OFOperator_thenFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("non_existent")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.CONTAINS_ONE_OF,
                        values = listOf("digitall", "test")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationOrWithRegexOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.OR,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf("([a-zA-Z]+_?)+")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf("([0-9]+_?)+")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(true, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithRegexOperator_thenFalse() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf("([a-zA-Z]+_?)+")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf("([0-9]+_?)+")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(false, result)
    }

    @Test
    fun givenInApp_whenRelationAndWithCorrectRegexOperator_thenTrue() {
        //Given
        val sut = createValidator()
        val inapp = createInAppWithEvent(
            TargetingRule.Event(
                name = "",
                paramsRelation = RuleRelation.AND,
                params = listOf(
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf("([a-zA-Z]+_?)+")
                    ),
                    RuleEventParameter(
                        name = Event.SCREEN_VIEW_PARAM_NAME,
                        operator = StringOperator.REG_EX,
                        values = listOf(".+")
                    )
                )
            )
        )
        //When
        val result = sut.checkEventMatchesRules(inapp, Event.ScreenView("some_long_num_digital"))
        //Then
        assertEquals(true, result)
    }

    private fun createValidator() = RuleEventValidator()

    private fun createInAppWithEvent(rule: TargetingRule.Event) = InAppWithEvent(
        InAppMessage(
            messageId = 2,
            messageInstanceId = 2,
            displayRules = DisplayRules(
                frequency = null,
                targeting = null,
                schedule = null,
                async = null
            ),
            content = null,
            lastShowTime = null,
            displayRulesJson = JsonObject()
        ),
        event = rule
    )
}