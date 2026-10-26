package com.reteno.core.data.remote.model.iam

import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResponse
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResult
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentResponse
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ResponseDefaultsTest {

    @Test
    fun `should read responses without their lists as empty`() = runUnitTest {
        given()
        val json = "{}"

        whenn()
        val lists = listOf(
            json.fromJson<InAppMessageListResponse>().messages,
            json.fromJson<InAppMessagesContentResponse>().contents,
            json.fromJson<AsyncRulesCheckResponse>().checks
        )

        then()
        assertEquals(listOf(emptyList<Any>(), emptyList<Any>(), emptyList<Any>()), lists)
    }

    @Test
    fun `should read segment check without result as unknown`() = runUnitTest {
        given()
        val json = """{"segmentId":42}"""

        whenn()
        val result = json.fromJson<AsyncRulesCheckResult>()

        then()
        assertEquals(AsyncRulesCheckResult(segmentId = 42, checkResult = null, error = null), result)
    }
}
