package com.reteno.core.data.remote.model.iam

import com.google.gson.JsonParser
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckRequest
import com.reteno.core.data.remote.model.iam.initfailed.Data
import com.reteno.core.data.remote.model.iam.initfailed.IamJsWidgetInitiFailed
import com.reteno.core.data.remote.model.iam.initfailed.Payload
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentRequest
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RequestWireFormatTest {

    @Test
    fun `should ask for contents of in-app message instances`() = runUnitTest {
        given()
        val request = InAppMessagesContentRequest(listOf(11L, 12L))

        whenn()
        val json = JsonParser.parseString(request.toJson())

        then()
        assertEquals(listOf(11L, 12L), request.messageInstanceIds)
        assertEquals(JsonParser.parseString("""{"messageInstanceIds":[11,12]}"""), json)
    }

    @Test
    fun `should ask whether the user is in each segment`() = runUnitTest {
        given()
        val segmentIds = listOf(42L, 43L)

        whenn()
        val request = AsyncRulesCheckRequest.createSegmentRequest(segmentIds)

        then()
        assertEquals(listOf(42L, 43L), request.checks.map { it.params.segmentId })
        assertEquals(
            JsonParser.parseString(
                """{"checks":[{"type":"IS_IN_SEGMENT","params":{"segmentId":42}},""" +
                    """{"type":"IS_IN_SEGMENT","params":{"segmentId":43}}]}"""
            ),
            JsonParser.parseString(request.toJson())
        )
    }

    @Test
    fun `should report widget failure with tenant and failure details`() = runUnitTest {
        given()
        val data = Data(payload = Payload(reason = "script error"), type = "WIDGET_INIT_FAILED")

        whenn()
        val report = IamJsWidgetInitiFailed(tenantId = "11", data = data.toJson())

        then()
        assertEquals(
            listOf(
                "11",
                """{"payload":{"reason":"script error"},"type":"WIDGET_INIT_FAILED"}""",
                "script error",
                "WIDGET_INIT_FAILED"
            ),
            listOf(report.tenantId, report.data, data.payload?.reason, data.type)
        )
    }
}
