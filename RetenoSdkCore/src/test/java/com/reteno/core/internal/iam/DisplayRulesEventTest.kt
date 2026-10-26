package com.reteno.core.internal.iam

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEventRule
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DisplayRulesEventTest {

    private class SutFixture {
        val sink = FakeLogSink()
        val sut = DisplayRulesEvaluator(clock = { 0L }, sink = sink)

        fun matches(
            operator: StringOperator,
            vararg values: String,
            eventParameters: List<Parameter> = listOf(Parameter("sku", "Shoe-42"))
        ): Boolean {
            val rule = stubEventRule(params = listOf(RuleEventParameter("sku", operator, values.toList())))
            return sut.matchesEvent(rule, stubCustomEvent(parameters = eventParameters))
        }
    }

    @Test
    fun `should not match event with another name`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.sut.matchesEvent(stubEventRule(name = "purchase"), stubCustomEvent(typeKey = "refund"))

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match event by name when rule has no parameters`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.sut.matchesEvent(stubEventRule(params = null), stubCustomEvent())

        then()
        assertTrue(matches)
    }

    @Test
    fun `should match event by name when rule has empty parameters`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.sut.matchesEvent(stubEventRule(params = emptyList()), stubCustomEvent())

        then()
        assertTrue(matches)
    }

    @Test
    fun `should match equal parameter value`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.EQUALS, "Boot-1", "Shoe-42")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match different parameter value`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.EQUALS, "Boot-1")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should compare equal values case sensitively`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.EQUALS, "shoe-42")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match equals when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.EQUALS, "Shoe-42", eventParameters = emptyList())

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match equals when event has no parameters`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val rule = stubEventRule(params = listOf(SKU_IS_SHOE))

        whenn()
        val matches = fixture.sut.matchesEvent(rule, stubCustomEvent(parameters = null))

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match does not equal for a different value`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.DOES_NOT_EQUAL, "Boot-1")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match does not equal for one of its values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.DOES_NOT_EQUAL, "Boot-1", "Shoe-42")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match does not equal when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.DOES_NOT_EQUAL, "Shoe-42", eventParameters = emptyList())

        then()
        assertTrue(matches)
    }

    @Test
    fun `should match value containing one of the values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.CONTAINS, "Boot", "oe-4")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match value containing none of the values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.CONTAINS, "Boot")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should check containment case sensitively`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.CONTAINS, "shoe")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match contains when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.CONTAINS, "Shoe", eventParameters = emptyList())

        then()
        assertFalse(matches)
    }

    @Test
    fun `should treat contains one of like contains`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.CONTAINS_ONE_OF, "Boot", "-42")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should match value starting with one of the values ignoring case`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.STARTS_WITH, "boot", "SHOE")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match value starting with none of the values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.STARTS_WITH, "42")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match starts with when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.STARTS_WITH, "Shoe", eventParameters = emptyList())

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match value ending with one of the values ignoring case`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.ENDS_WITH, "boot", "E-42")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match value ending with none of the values`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.ENDS_WITH, "Shoe")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match ends with when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.ENDS_WITH, "42", eventParameters = emptyList())

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match value fully matching one of the patterns`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.REG_EX, "Boot-\\d+", "Shoe-\\d+")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match value matching a pattern only partially`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.REG_EX, "Shoe")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should not match pattern when event lacks the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.REG_EX, ".*", eventParameters = emptyList())

        then()
        assertFalse(matches)
    }

    @Test
    fun `should treat invalid pattern as no match`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.REG_EX, "Shoe-(")

        then()
        assertFalse(matches)
    }

    @Test
    fun `should keep checking other patterns after an invalid one`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val matches = fixture.matches(StringOperator.REG_EX, "Shoe-(", "Shoe-\\d+")

        then()
        assertTrue(matches)
    }

    @Test
    fun `should warn about invalid pattern naming only the parameter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.matches(StringOperator.REG_EX, "Shoe-(")

        then()
        val warning = fixture.sink.entries.single()
        assertEquals(LogPriority.WARN, warning.priority)
        assertEquals("in-app event rule parameter 'sku' has an invalid pattern, treated as no match", warning.message)
    }

    @Test
    fun `should match and relation when every parameter matches`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val rule = stubEventRule(relation = RuleRelation.AND, params = listOf(SKU_IS_SHOE, SIZE_IS_42))
        val event = stubCustomEvent(parameters = listOf(Parameter("sku", "Shoe"), Parameter("size", "42")))

        whenn()
        val matches = fixture.sut.matchesEvent(rule, event)

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match and relation when one parameter differs`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val rule = stubEventRule(relation = RuleRelation.AND, params = listOf(SKU_IS_SHOE, SIZE_IS_42))
        val event = stubCustomEvent(parameters = listOf(Parameter("sku", "Shoe"), Parameter("size", "41")))

        whenn()
        val matches = fixture.sut.matchesEvent(rule, event)

        then()
        assertFalse(matches)
    }

    @Test
    fun `should match or relation when one parameter matches`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val rule = stubEventRule(relation = RuleRelation.OR, params = listOf(SKU_IS_SHOE, SIZE_IS_42))
        val event = stubCustomEvent(parameters = listOf(Parameter("sku", "Boot"), Parameter("size", "42")))

        whenn()
        val matches = fixture.sut.matchesEvent(rule, event)

        then()
        assertTrue(matches)
    }

    @Test
    fun `should not match or relation when no parameter matches`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val rule = stubEventRule(relation = RuleRelation.OR, params = listOf(SKU_IS_SHOE, SIZE_IS_42))
        val event = stubCustomEvent(parameters = listOf(Parameter("sku", "Boot"), Parameter("size", "41")))

        whenn()
        val matches = fixture.sut.matchesEvent(rule, event)

        then()
        assertFalse(matches)
    }

    private companion object {
        val SKU_IS_SHOE = RuleEventParameter("sku", StringOperator.EQUALS, listOf("Shoe"))
        val SIZE_IS_42 = RuleEventParameter("size", StringOperator.EQUALS, listOf("42"))
    }
}
