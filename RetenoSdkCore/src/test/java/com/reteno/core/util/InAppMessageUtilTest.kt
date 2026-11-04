package com.reteno.core.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRulesParsingException
import com.reteno.core.data.remote.model.iam.displayrules.RuleRelation
import com.reteno.core.data.remote.model.iam.displayrules.StringOperator
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.displayrules.schedule.ScheduleRule
import com.reteno.core.data.remote.model.iam.displayrules.targeting.RuleEventParameter
import com.reteno.core.data.remote.model.iam.displayrules.targeting.TargetingRule
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

internal class InAppMessageUtilTest {

    @Test
    fun `should read message without display rules as unrestricted`() = runUnitTest {
        given()
        val json = JsonObject()

        whenn()
        val rules = InAppMessageUtil.parseRules(json)

        then()
        assertEquals(FrequencyDisplayRules(), rules.frequency)
        assertNull(rules.targeting)
        assertNull(rules.schedule)
        assertNull(rules.async)
    }

    @Test
    fun `should read every active frequency rule`() = runUnitTest {
        given()
        val json = rules(
            """"FREQUENCY":{"enabled":true,"predicates":[
                {"name":"NO_LIMIT","isActive":true},
                {"name":"ONCE_PER_APP","isActive":true},
                {"name":"ONCE_PER_SESSION","isActive":true},
                {"name":"MIN_INTERVAL","isActive":true,"params":{"amount":2,"unit":"HOUR"}},
                {"name":"TIMES_PER_TIME_UNIT","isActive":true,"params":{"count":3,"unit":"DAY"}},
                {"name":"ONCE_PER_APP","isActive":false}
            ]}"""
        )

        whenn()
        val predicates = InAppMessageUtil.parseRules(json).frequency!!.predicates

        then()
        assertEquals(
            listOf(FrequencyRule.NoLimit, FrequencyRule.OncePerApp, FrequencyRule.OncePerSession),
            predicates.take(3)
        )
        val interval = predicates[3] as FrequencyRule.MinInterval
        val times = predicates[4] as FrequencyRule.TimesPerTimeUnit
        assertEquals(
            listOf(TimeUnit.HOURS.toMillis(2), TimeUnit.DAYS, 3),
            listOf(interval.intervalMillis, times.timeUnit, times.count)
        )
        assertEquals(5, predicates.size)
    }

    @Test
    fun `should ignore frequency rules that are switched off or not marked enabled`() = runUnitTest {
        given()
        val switchedOff = rules(""""FREQUENCY":{"enabled":false,"predicates":[{"name":"NO_LIMIT","isActive":true}]}""")
        val unmarked = rules(""""FREQUENCY":{"predicates":[{"name":"NO_LIMIT","isActive":true}]}""")

        whenn()
        val parsed = listOf(switchedOff, unmarked).map { InAppMessageUtil.parseRules(it).frequency }

        then()
        assertEquals(listOf(FrequencyDisplayRules(), FrequencyDisplayRules()), parsed)
    }

    @Test
    fun `should reject frequency rules it cannot apply`() = runUnitTest {
        given()
        val invalid = listOf(
            """{"name":"MIN_INTERVAL","isActive":true,"params":{"amount":2,"unit":"WEEK"}}""",
            """{"name":"MIN_INTERVAL","isActive":true,"params":{"amount":0,"unit":"HOUR"}}""",
            """{"name":"TIMES_PER_TIME_UNIT","isActive":true,"params":{"count":2,"unit":"WEEK"}}""",
            """{"name":"TIMES_PER_TIME_UNIT","isActive":true,"params":{"count":0,"unit":"DAY"}}""",
            """{"name":"FOREVER","isActive":true}"""
        )

        whenn()
        val failures = invalid.map { predicate ->
            val frequency = """"FREQUENCY":{"enabled":true,"predicates":[$predicate]}"""
            runCatching { InAppMessageUtil.parseRules(rules(frequency)) }
                .exceptionOrNull()
        }

        then()
        assertTrue(failures.all { it is DisplayRulesParsingException })
    }

    @Test
    fun `should read time spent and event targeting with parameters`() = runUnitTest {
        given()
        val json = rules(
            """"TARGETING":{"enabled":true,"schema":{
                "include":{"relation":"OR","groups":[{"relation":"AND","conditions":[$TIME_SPENT,$PURCHASE]}]},
                "exclude":{"relation":"AND","groups":[{"relation":"OR","conditions":[$SIGN_UP]}]}
            }}"""
        )

        whenn()
        val targeting = InAppMessageUtil.parseRules(json).targeting!!

        then()
        val include = targeting.include!!
        val exclude = targeting.exclude!!
        assertEquals(listOf(RuleRelation.OR, RuleRelation.AND), listOf(include.relation, exclude.relation))
        val conditions = include.groups.single()
        assertEquals(RuleRelation.AND, conditions.relation)
        val timeSpent = conditions.conditions[0] as TargetingRule.TimeSpentInApp
        val purchase = conditions.conditions[1] as TargetingRule.Event
        assertEquals(TimeUnit.SECONDS.toMillis(30), timeSpent.timeSpentMillis)
        assertEquals(
            listOf(
                "purchase",
                RuleRelation.OR,
                listOf(RuleEventParameter("sku", StringOperator.EQUALS, listOf("A-1", "B-2")))
            ),
            listOf(purchase.name, purchase.paramsRelation, purchase.params)
        )
        val signUp = exclude.groups.single().conditions.single() as TargetingRule.Event
        assertEquals(
            listOf("sign_up", RuleRelation.AND, null),
            listOf(signUp.name, signUp.paramsRelation, signUp.params)
        )
    }

    @Test
    fun `should skip targeting parts it does not recognise`() = runUnitTest {
        given()
        val unknownTimeUnit = """{"operand":{"name":"USER"},"operator":"TIME_SPENT_IN_APP",""" +
            """"values":[{"unit":"WEEK","amount":1}]}"""
        val unknownOperator = """{"operand":{"name":"EVENT"},""" +
            """"values":[{"event":"purchase","parameters":{"relation":"AND",""" +
            """"values":[{"name":"sku","operator":"LIKE","value":["A"]}]}}]}"""
        val json = rules(
            """"TARGETING":{"enabled":true,"schema":{
                "include":{"relation":"AND","groups":[
                    {"relation":"AND","conditions":[$unknownTimeUnit]},
                    {"relation":"XOR","conditions":[$SIGN_UP]},
                    {"relation":"AND","conditions":[$unknownOperator]}
                ]},
                "exclude":{"relation":"XOR","groups":[{"relation":"AND","conditions":[$SIGN_UP]}]}
            }}"""
        )

        whenn()
        val targeting = InAppMessageUtil.parseRules(json).targeting!!

        then()
        val event = targeting.include!!.groups.single().conditions.single() as TargetingRule.Event
        assertEquals(emptyList<RuleEventParameter>(), event.params)
        assertNull(targeting.exclude)
    }

    @Test
    fun `should ignore targeting without usable groups or switched off`() = runUnitTest {
        given()
        val noUsableGroups = rules(
            """"TARGETING":{"enabled":true,"schema":{
                "include":{"relation":"AND","groups":[]},
                "exclude":{"relation":"AND","groups":[]}
            }}"""
        )
        val switchedOff = rules(""""TARGETING":{"enabled":false}""")

        whenn()
        val parsed = listOf(noUsableGroups, switchedOff).map { InAppMessageUtil.parseRules(it).targeting }

        then()
        assertEquals(listOf(null, null), parsed)
    }

    @Test
    fun `should reject targeting it cannot apply`() = runUnitTest {
        given()
        val invalid = listOf(
            """{"operand":{"name":"USER"},"operator":"AGE","values":[]}""",
            """{"operand":{"name":"DEVICE"}}""",
            """{"operand":{"name":"EVENT"},"values":[{"event":"purchase","parameters":{"relation":"XOR",""" +
                """"values":[{"name":"sku","operator":"EQUALS","value":["A"]}]}}]}"""
        )

        whenn()
        val failures = invalid.map { condition ->
            runCatching {
                InAppMessageUtil.parseRules(
                    rules(
                        """"TARGETING":{"enabled":true,"schema":{
                            "include":{"relation":"AND","groups":[{"relation":"AND","conditions":[$condition]}]},
                            "exclude":{"relation":"AND","groups":[]}
                        }}"""
                    )
                )
            }.exceptionOrNull()
        }

        then()
        assertTrue(failures.all { it is DisplayRulesParsingException })
    }

    @Test
    fun `should read segment rule when segment check is on`() = runUnitTest {
        given()
        val json = rules(""""ASYNC":{"IS_IN_SEGMENT":{"enabled":true,"segmentId":42}}""")

        whenn()
        val async = InAppMessageUtil.parseRules(json).async

        then()
        assertEquals(42L, async?.segment?.segmentId)
    }

    @Test
    fun `should ignore segment rule that is switched off or empty`() = runUnitTest {
        given()
        val switchedOff = rules(""""ASYNC":{"IS_IN_SEGMENT":{"enabled":false,"segmentId":42}}""")
        val empty = rules(""""ASYNC":{}""")

        whenn()
        val parsed = listOf(switchedOff, empty).map { InAppMessageUtil.parseRules(it).async }

        then()
        assertEquals(listOf(null, null), parsed)
    }

    @Test
    fun `should reject unknown or several async rules`() = runUnitTest {
        given()
        val invalid = listOf(
            """"ASYNC":{"IS_IN_COUNTRY":{"enabled":true}}""",
            """"ASYNC":{"IS_IN_SEGMENT":{"enabled":true,"segmentId":1},"IS_IN_COUNTRY":{}}"""
        )

        whenn()
        val failures = invalid.map { runCatching { InAppMessageUtil.parseRules(rules(it)) }.exceptionOrNull() }

        then()
        assertTrue(failures.all { it is DisplayRulesParsingException })
    }

    @Test
    fun `should read every active schedule rule in its time zone`() = runUnitTest {
        given()
        val json = rules(
            """"SCHEDULE":{"enabled":true,"predicates":[
                {"name":"SHOW_AFTER","isActive":true,"params":{"date":"2026-03-01T10:00","timeZone":"Europe/Warsaw"}},
                {"name":"HIDE_AFTER","isActive":true,"params":{"date":"2026-04-01T10:00","timeZone":null}},
                {"name":"SPECIFIC_DAYS_AND_TIME","isActive":true,"params":{"days":["MONDAY"],
                    "time":{"from":{"hours":9,"minutes":30},"to":{"hours":18,"minutes":0}}}},
                {"name":"SHOW_AFTER","isActive":false,"params":{"date":"2026-03-01T10:00"}}
            ]}"""
        )

        whenn()
        val predicates = InAppMessageUtil.parseRules(json).schedule!!.predicates

        then()
        val showAfter = predicates[0] as ScheduleRule.ShowAfter
        val hideAfter = predicates[1] as ScheduleRule.HideAfter
        val days = predicates[2] as ScheduleRule.SpecificDaysAndTime
        assertEquals(ZonedDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneId.of("Europe/Warsaw")), showAfter.zonedDateTime)
        assertEquals(ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneId.systemDefault()), hideAfter.zonedDateTime)
        assertEquals(
            listOf(ZoneId.systemDefault().id, listOf("MONDAY"), 9, 30, 18, 0),
            listOf(days.timeZone, days.daysOfWeek, days.fromHours, days.fromMinutes, days.toHours, days.toMinutes)
        )
        assertEquals(3, predicates.size)
    }

    @Test
    fun `should ignore schedule without active rules or switched off`() = runUnitTest {
        given()
        val inactive = rules(""""SCHEDULE":{"enabled":true,"predicates":[{"name":"SHOW_AFTER","isActive":false}]}""")
        val switchedOff = rules(""""SCHEDULE":{"enabled":false,"predicates":[]}""")

        whenn()
        val parsed = listOf(inactive, switchedOff).map { InAppMessageUtil.parseRules(it).schedule }

        then()
        assertEquals(listOf(null, null), parsed)
    }

    @Test
    fun `should reject unknown schedule rule`() = runUnitTest {
        given()
        val json = rules(""""SCHEDULE":{"enabled":true,"predicates":[{"name":"EVERY_FULL_MOON","isActive":true}]}""")

        whenn()
        val failure = runCatching { InAppMessageUtil.parseRules(json) }.exceptionOrNull()

        then()
        assertTrue(failure is DisplayRulesParsingException)
    }

    @Test
    fun `should ignore targeting segment and schedule rules not marked enabled`() = runUnitTest {
        given()
        val json = rules(
            """"TARGETING":{"schema":{}},"ASYNC":{"IS_IN_SEGMENT":{"segmentId":42}},""" +
                """"SCHEDULE":{"predicates":[{"name":"SHOW_AFTER","isActive":true,""" +
                """"params":{"date":"2026-03-01T10:00"}}]}"""
        )

        whenn()
        val parsed = InAppMessageUtil.parseRules(json)

        then()
        assertEquals(listOf(null, null, null), listOf(parsed.targeting, parsed.async, parsed.schedule))
    }

    @Test
    fun `should read targeting with exclude group only`() = runUnitTest {
        given()
        val json = rules(
            """"TARGETING":{"enabled":true,"schema":{
                "include":{"relation":"AND","groups":[]},
                "exclude":{"relation":"OR","groups":[{"relation":"AND","conditions":[$SIGN_UP]}]}
            }}"""
        )

        whenn()
        val targeting = InAppMessageUtil.parseRules(json).targeting!!

        then()
        assertNull(targeting.include)
        assertEquals(RuleRelation.OR, targeting.exclude!!.relation)
    }

    @Test
    fun `should combine event parameters with and when no relation is given`() = runUnitTest {
        given()
        val event = """{"operand":{"name":"EVENT"},""" +
            """"values":[{"event":"sign_up","parameters":{"relation":"XOR","values":[]}}]}"""
        val json = rules(
            """"TARGETING":{"enabled":true,"schema":{
                "include":{"relation":"AND","groups":[{"relation":"AND","conditions":[$event]}]},
                "exclude":{"relation":"AND","groups":[]}
            }}"""
        )

        whenn()
        val rule = InAppMessageUtil.parseRules(json).targeting!!.include!!.groups.single().conditions.single()

        then()
        assertEquals(RuleRelation.AND, (rule as TargetingRule.Event).paramsRelation)
    }

    private fun rules(body: String): JsonObject = JsonParser.parseString("{$body}").asJsonObject

    private companion object {
        const val TIME_SPENT = """{"operand":{"name":"USER"},"operator":"TIME_SPENT_IN_APP",""" +
            """"values":[{"unit":"SECOND","amount":30}]}"""
        const val PURCHASE = """{"operand":{"name":"EVENT"},""" +
            """"values":[{"event":"purchase","parameters":{"relation":"OR",""" +
            """"values":[{"name":"sku","operator":"EQUALS","value":["A-1","B-2"]}]}}]}"""
        const val SIGN_UP = """{"operand":{"name":"EVENT"},""" +
            """"values":[{"event":"sign_up","parameters":{"relation":"AND","values":[]}}]}"""
    }
}
