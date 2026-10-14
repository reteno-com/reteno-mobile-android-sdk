package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LoadPushInAppHtmlActionTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val rest = FakeRestClient()
        val files = mockk<FileManager>()
        val platform = FakeAppPlatform()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val traced = TestActionRunner(scope)
        var baseHtmlReadOnIo = false
        var bundledWidgetReadOnIo = false
        val sut = LoadPushInAppHtmlAction(
            api = rest.api(dispatchers),
            files = files,
            platform = platform,
            initGate = initGate,
            dispatchers = dispatchers,
            newId = { "html-1" },
            runner = traced.runner
        )

        init {
            coEvery { files.getBaseHtmlContent() } answers {
                baseHtmlReadOnIo = dispatchers.markedIo.isRunningHere
                BASE_HTML
            }
            platform.onBundledWidget = { bundledWidgetReadOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should load widget of push interaction from server`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(WIDGET_JSON)

        whenn()
        fixture.sut("interaction-1")

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(ApiContract.InAppMessages.GetInnAppWidgetByInteractionId("interaction-1"), request.contract)
    }

    @Test
    fun `should put widget model and personalisation into base html`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(WIDGET_JSON)

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertEquals("""<html>{"text":"hi"}|{"name":"Ann"}</html>""", result.fullHtml)
        assertEquals("html-1", result.id)
    }

    @Test
    fun `should keep widget layout`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith(
            """{"layoutType":"SLIDE_UP","layoutParams":{"position":"BOTTOM"},"model":{}}"""
        )

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertEquals(InAppLayoutType.SLIDE_UP, result.layoutType)
        assertEquals(InAppLayoutParams(Position.BOTTOM), result.layoutParams)
    }

    @Test
    fun `should show widget full screen at top when it has no layout`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("""{"model":{}}""")

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertEquals(InAppLayoutType.FULL, result.layoutType)
        assertEquals(InAppLayoutParams(Position.TOP), result.layoutParams)
    }

    @Test
    fun `should leave placeholders of a widget without model and personalisation`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.succeedWith("{}")

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertEquals(BASE_HTML, result.fullHtml)
    }

    @Test
    fun `should use bundled widget when server fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.bundledWidget = """{"text":"bundled"}"""
        fixture.rest.failWith(500)

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertEquals("""<html>{"text":"bundled"}|${'$'}{personalisation}</html>""", result.fullHtml)
        assertEquals(InAppLayoutType.FULL, result.layoutType)
    }

    @Test
    fun `should use bundled widget when server answer cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.bundledWidget = """{"text":"bundled"}"""
        fixture.rest.succeedWith("{not json")

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertTrue(result.fullHtml.contains("bundled"))
    }

    @Test
    fun `should use bundled widget when server answer is empty`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.bundledWidget = """{"text":"bundled"}"""
        fixture.rest.succeedWith("")

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertTrue(result.fullHtml.contains("bundled"))
    }

    @Test
    fun `should use empty widget when bundled widget cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.bundledWidget = null
        fixture.rest.failWith(500)

        whenn()
        val result = fixture.sut("interaction-1").getOrThrow()

        then()
        assertTrue(result.fullHtml.startsWith("<html>{}|"))
    }

    @Test
    fun `should load widget only after sdk initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        val load = async { fixture.sut("interaction-1") }
        runCurrent()
        val requestsBeforeInit = fixture.rest.requests.size

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        load.await()

        then()
        assertEquals(0, requestsBeforeInit)
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should read base html and bundled widget on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.rest.failWith(500)

        whenn()
        fixture.sut("interaction-1")

        then()
        assertTrue(fixture.baseHtmlReadOnIo)
        assertTrue(fixture.bundledWidgetReadOnIo)
    }

    private companion object {
        const val BASE_HTML = "<html>\${documentModel}|\${personalisation}</html>"
        const val WIDGET_JSON = """{"model":{"text":"hi"},"personalisation":{"name":"Ann"}}"""
    }
}
