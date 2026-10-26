package com.reteno.core.data.remote.model.iam

import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class DisplayRuleModelsTest {

    @Test
    fun `should read every string operator and nothing for unknown ones`() = runUnitTest {
        given()
        val names = StringOperator.values().map { it.name } + listOf("LIKE", null)

        whenn()
        val operators = names.map { StringOperator.fromString(it) }

        then()
        assertEquals(StringOperator.values().toList() + listOf(null, null), operators)
    }

    @Test
    fun `should read rule relations and nothing for unknown ones`() = runUnitTest {
        given()
        val names = listOf("AND", "OR", "XOR", null)

        whenn()
        val relations = names.map { RuleRelation.fromString(it) }

        then()
        assertEquals(listOf(RuleRelation.AND, RuleRelation.OR, null, null), relations)
    }

    @Test
    fun `should compare frequency rules by their predicates`() = runUnitTest {
        given()
        val rules = FrequencyDisplayRules().apply { predicates += FrequencyRule.OncePerApp }
        val same = FrequencyDisplayRules().apply { predicates += FrequencyRule.OncePerApp }

        whenn()
        val comparisons = listOf(
            rules == same,
            rules.equals(FrequencyRule.OncePerApp),
            rules == FrequencyDisplayRules()
        )

        then()
        assertEquals(listOf(true, false, false), comparisons)
        assertEquals(same.hashCode(), rules.hashCode())
        assertNotEquals(FrequencyDisplayRules().hashCode(), rules.hashCode())
    }
}
