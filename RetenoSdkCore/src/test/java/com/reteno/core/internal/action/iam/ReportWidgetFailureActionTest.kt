package com.reteno.core.internal.action.iam

import com.google.gson.JsonParser
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.features.iam.IamJsPayload
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ReportWidgetFailureActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        val sut = ReportWidgetFailureAction(rest.api(scope.testDispatchers()), initGate, traced.runner)

        fun sentBody() = JsonParser.parseString(rest.requests.single().body).asJsonObject
    }

    @Test
    fun `should send widget failure to site script endpoint`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("tenant-1", FAILED_WITH_REASON)

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.POST, request.method)
        assertEquals(ApiContract.InAppMessages.WidgetInitFailed, request.contract)
    }

    @Test
    fun `should send tenant and event type with reason`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("tenant-1", FAILED_WITH_REASON)

        then()
        val body = fixture.sentBody()
        assertEquals("tenant-1", body.get("tenantId").asString)
        val data = JsonParser.parseString(body.get("data").asString).asJsonObject
        assertEquals("WIDGET_INIT_FAILED", data.get("type").asString)
        assertEquals("script error", data.getAsJsonObject("payload").get("reason").asString)
    }

    @Test
    fun `should send failure without reason when event has no payload`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("tenant-1", IamJsEvent(IamJsEventType.WIDGET_RUNTIME_ERROR, payload = null))

        then()
        val data = JsonParser.parseString(fixture.sentBody().get("data").asString).asJsonObject
        assertTrue(data.getAsJsonObject("payload").get("reason") == null)
    }

    @Test
    fun `should not fail when server rejects the report`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(500)

        whenn()
        val result = fixture.sut("tenant-1", FAILED_WITH_REASON)

        then()
        assertTrue(result.isSuccess)
        assertTrue(fixture.traced.reports.isEmpty())
    }

    @Test
    fun `should send widget failure only after sdk initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        val report = async { fixture.sut("tenant-1", FAILED_WITH_REASON) }
        runCurrent()
        val sentBeforeInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        report.await()

        then()
        assertEquals(0, sentBeforeInit)
        assertEquals(1, fixture.rest.requests.size)
    }

    private companion object {
        val FAILED_WITH_REASON = IamJsEvent(
            IamJsEventType.WIDGET_INIT_FAILED,
            IamJsPayload(targetComponentId = null, url = null, reason = "script error", contentHeight = null)
        )
    }
}
