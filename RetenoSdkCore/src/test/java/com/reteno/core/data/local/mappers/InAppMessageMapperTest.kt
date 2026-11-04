package com.reteno.core.data.local.mappers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.model.iam.SegmentDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRuleRetryParams
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckError
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckRetryModel
import com.reteno.core.data.remote.model.iam.displayrules.async.SegmentRule
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.fixtures.stubInAppContent
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class InAppMessageMapperTest {

    @Test
    fun `should read in-app message from server with its display rules`() = runUnitTest {
        given()
        val response = InAppMessageResponse(messageId = 1, messageInstanceId = 11, displayRules = SEGMENT_RULES)

        whenn()
        val message = response.toInAppMessage()

        then()
        assertEquals(
            listOf(1L, 11L, SEGMENT_RULES),
            listOf(message.messageId, message.messageInstanceId, message.displayRulesJson)
        )
        assertEquals(42L, message.displayRules.async?.segment?.segmentId)
    }

    @Test
    fun `should skip messages from server whose display rules cannot be read`() = runUnitTest {
        given()
        val responses = listOf(
            InAppMessageResponse(1, 11, SEGMENT_RULES),
            InAppMessageResponse(2, 12, json("""{"ASYNC":{"IS_IN_COUNTRY":{}}}"""))
        )

        whenn()
        val messages = responses.mapResponseToInAppMessages()

        then()
        assertEquals(listOf(1L), messages.map { it.messageId })
    }

    @Test
    fun `should read stored message with content layout and position`() = runUnitTest {
        given()
        val stored = stored(layoutType = "SLIDE_UP", model = """{"text":"hi"}""", position = "BOTTOM")

        whenn()
        val content = stored.toInAppMessage().content!!

        then()
        assertEquals(
            listOf(11L, InAppLayoutType.SLIDE_UP, Position.BOTTOM, json("""{"text":"hi"}""")),
            listOf(content.messageInstanceId, content.layoutType, content.layoutParams?.position, content.model)
        )
    }

    @Test
    fun `should read unknown stored layout and position with their defaults`() = runUnitTest {
        given()
        val stored = stored(layoutType = "CAROUSEL", model = "{}", position = "LEFT")

        whenn()
        val content = stored.toInAppMessage().content!!

        then()
        assertEquals(
            listOf(InAppLayoutType.FULL, Position.TOP),
            listOf(content.layoutType, content.layoutParams?.position)
        )
    }

    @Test
    fun `should read stored content without position`() = runUnitTest {
        given()
        val stored = stored(layoutType = "FULL", model = "{}", position = null)

        whenn()
        val content = stored.toInAppMessage().content!!

        then()
        assertNull(content.layoutParams)
    }

    @Test
    fun `should read stored message without content when model or layout is missing`() = runUnitTest {
        given()
        val stored = listOf(stored(layoutType = "FULL", model = null), stored(layoutType = null, model = "{}"))

        whenn()
        val contents = stored.map { it.toInAppMessage().content }

        then()
        assertEquals(listOf(null, null), contents)
    }

    @Test
    fun `should restore stored segment check of the same segment with show history`() = runUnitTest {
        given()
        val stored = stored(lastShowTime = 5L, showCount = 2).apply {
            segment = SegmentDb(
                segmentId = 42,
                isInSegment = true,
                lastCheckTime = 9L,
                checkStatusCode = 429,
                retryAfter = 60_000L
            )
        }

        whenn()
        val message = stored.toInAppMessage()

        then()
        val segment = message.displayRules.async!!.segment!!
        assertEquals(
            listOf(5L, 2L, true, 9L, AsyncRuleRetryParams(429, 60_000L)),
            listOf(
                message.lastShowTime,
                message.showCount,
                segment.isInSegment,
                segment.lastCheckedTimestamp,
                segment.retryParams
            )
        )
    }

    @Test
    fun `should not restore stored check of another segment or without segment rule`() = runUnitTest {
        given()
        val otherSegment = stored().apply { segment = SegmentDb(7, true, 9L, null, null) }
        val noSegmentRule = stored(rules = "{}").apply { segment = SegmentDb(42, true, 9L, null, null) }

        whenn()
        val restored = listOf(otherSegment.toInAppMessage(), noSegmentRule.toInAppMessage())

        then()
        assertEquals(false, restored[0].displayRules.async!!.segment!!.isInSegment)
        assertNull(restored[1].displayRules.async)
    }

    @Test
    fun `should read stored segment without check status as having no retry advice`() = runUnitTest {
        given()
        val stored = stored().apply { segment = SegmentDb(42, false, null, null, null) }

        whenn()
        val segment = stored.toInAppMessage().displayRules.async!!.segment!!

        then()
        assertNull(segment.retryParams)
    }

    @Test
    fun `should skip stored messages that cannot be read`() = runUnitTest {
        given()
        val stored = listOf(stored(), stored(rules = "not json"))

        whenn()
        val messages = stored.mapDbToInAppMessages()

        then()
        assertEquals(1, messages.size)
    }

    @Test
    fun `should store message with content segment check and show history`() = runUnitTest {
        given()
        val message = InAppMessageResponse(1, 11, SEGMENT_RULES).toInAppMessage().apply {
            content = stubInAppContent(json("""{"text":"hi"}"""))
            lastShowTime = 5L
            showCount = 2
            displayRules.async!!.segment!!.apply {
                isInSegment = true
                lastCheckedTimestamp = 9L
                retryParams = AsyncRuleRetryParams(429, 60_000L)
            }
        }

        whenn()
        val stored = message.toDB()

        then()
        assertEquals(
            listOf(1L, 11L, SEGMENT_RULES.toString(), "FULL", """{"text":"hi"}""", null, 5L, 2L),
            listOf(
                stored.messageId, stored.messageInstanceId, stored.displayRules, stored.layoutType, stored.model,
                stored.position, stored.lastShowTime, stored.showCount
            )
        )
        assertEquals(SegmentDb(42, true, 9L, 429, 60_000L), stored.segment)
    }

    @Test
    fun `should store message without content and segment rule`() = runUnitTest {
        given()
        val message = stubInAppMessage(content = null)

        whenn()
        val stored = message.toDB()

        then()
        assertEquals(
            listOf(null, null, null, null),
            listOf(stored.layoutType, stored.model, stored.position, stored.segment)
        )
    }

    @Test
    fun `should store segment without retry advice`() = runUnitTest {
        given()
        val segment = SegmentRule(42)

        whenn()
        val stored = segment.toDb()

        then()
        assertEquals(SegmentDb(42, false, null, null, null), stored)
    }

    @Test
    fun `should update show history and same segment check from storage`() = runUnitTest {
        given()
        val message = InAppMessageResponse(1, 11, SEGMENT_RULES).toInAppMessage()
        val stored = stored(lastShowTime = 5L, showCount = 2).apply { segment = SegmentDb(42, true, 9L, null, 30L) }
        message.displayRules.async!!.segment!!.retryParams = AsyncRuleRetryParams(500, null)

        whenn()
        message.updateFromDb(stored)

        then()
        val segment = message.displayRules.async!!.segment!!
        assertEquals(
            listOf(5L, 2L, true, 9L, AsyncRuleRetryParams(0, 30L)),
            listOf(
                message.lastShowTime,
                message.showCount,
                segment.isInSegment,
                segment.lastCheckedTimestamp,
                segment.retryParams
            )
        )
    }

    @Test
    fun `should keep segment check when storage has another or no segment`() = runUnitTest {
        given()
        val messages = List(2) { InAppMessageResponse(1, 11, SEGMENT_RULES).toInAppMessage() }
        val otherSegment = stored().apply { segment = SegmentDb(7, true, 9L, 429, null) }
        val noSegment = stored()

        whenn()
        messages[0].updateFromDb(otherSegment)
        messages[1].updateFromDb(noSegment)

        then()
        assertEquals(listOf(false, false), messages.map { it.displayRules.async!!.segment!!.isInSegment })
    }

    @Test
    fun `should update only show history of message without segment rule`() = runUnitTest {
        given()
        val message = stubInAppMessage()

        whenn()
        message.updateFromDb(stored(lastShowTime = 5L, showCount = 2))

        then()
        assertEquals(listOf(5L, 2L), listOf(message.lastShowTime, message.showCount))
    }

    @Test
    fun `should read retry advice of a failed segment check`() = runUnitTest {
        given()
        val errors = listOf(
            AsyncRulesCheckError(429, AsyncRulesCheckRetryModel("MINUTE", 2)),
            AsyncRulesCheckError(429, AsyncRulesCheckRetryModel("FORTNIGHT", 2)),
            AsyncRulesCheckError(429, AsyncRulesCheckRetryModel("SECOND", null)),
            AsyncRulesCheckError(500, null)
        )

        whenn()
        val advice = errors.map { it.toDomain() }

        then()
        assertEquals(
            listOf(
                AsyncRuleRetryParams(429, TimeUnit.MINUTES.toMillis(2)),
                AsyncRuleRetryParams(429, null),
                AsyncRuleRetryParams(429, 0L),
                AsyncRuleRetryParams(500, null)
            ),
            advice
        )
    }

    @Test
    fun `should store content the server sent without layout or model as having none`() = runUnitTest {
        given()
        val content = """{"messageInstanceId":11}""".fromJson<InAppMessageContent>()
        val message = stubInAppMessage(content = content)

        whenn()
        val stored = message.toDB()

        then()
        assertEquals(listOf(null, null, null), listOf(stored.layoutType, stored.model, stored.position))
    }

    @Test
    fun `should store content placed without position`() = runUnitTest {
        given()
        val message = stubInAppMessage(content = stubInAppContent().copy(layoutParams = InAppLayoutParams(null)))

        whenn()
        val stored = message.toDB()

        then()
        assertNull(stored.position)
    }

    @Test
    fun `should store and update message whose async rules have no segment`() = runUnitTest {
        given()
        val message = stubInAppMessage(async = AsyncDisplayRules(segment = null))

        whenn()
        val stored = message.toDB()
        message.updateFromDb(stored(showCount = 3).apply { segment = SegmentDb(42, true, 9L, null, null) })

        then()
        assertNull(stored.segment)
        assertEquals(3L, message.showCount)
    }

    @Test
    fun `should update segment check without retry advice from storage`() = runUnitTest {
        given()
        val message = InAppMessageResponse(1, 11, SEGMENT_RULES).toInAppMessage()

        whenn()
        message.updateFromDb(stored().apply { segment = SegmentDb(42, true, 9L, 429, 30L) })

        then()
        val segment = message.displayRules.async!!.segment!!
        assertEquals(
            listOf(true, 9L, null),
            listOf(segment.isInSegment, segment.lastCheckedTimestamp, segment.retryParams)
        )
    }

    @Test
    fun `should store position of content placed at the bottom`() = runUnitTest {
        given()
        val content = stubInAppContent().copy(
            layoutType = InAppLayoutType.SLIDE_UP,
            layoutParams = InAppLayoutParams(Position.BOTTOM)
        )

        whenn()
        val stored = stubInAppMessage(content = content).toDB()

        then()
        assertEquals(listOf("SLIDE_UP", "BOTTOM"), listOf(stored.layoutType, stored.position))
    }

    @Test
    fun `should update retry advice of the same segment from storage`() = runUnitTest {
        given()
        val message = InAppMessageResponse(1, 11, SEGMENT_RULES).toInAppMessage()
        message.displayRules.async!!.segment!!.retryParams = AsyncRuleRetryParams(500, null)

        whenn()
        message.updateFromDb(stored().apply { segment = SegmentDb(42, false, 9L, 429, 30L) })

        then()
        assertEquals(AsyncRuleRetryParams(429, 30L), message.displayRules.async!!.segment!!.retryParams)
    }

    private fun stored(
        rules: String = SEGMENT_RULES.toString(),
        layoutType: String? = null,
        model: String? = null,
        position: String? = null,
        lastShowTime: Long? = null,
        showCount: Long = 0
    ) = InAppMessageDb(
        messageId = 1,
        messageInstanceId = 11,
        displayRules = rules,
        lastShowTime = lastShowTime,
        showCount = showCount,
        layoutType = layoutType,
        model = model,
        position = position
    )

    private fun json(text: String): JsonObject = JsonParser.parseString(text).asJsonObject

    private companion object {
        val SEGMENT_RULES: JsonObject = JsonParser.parseString(
            """{"ASYNC":{"IS_IN_SEGMENT":{"enabled":true,"segmentId":42}}}"""
        ).asJsonObject
    }
}
