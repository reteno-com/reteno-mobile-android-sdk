package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PreloadIamHtmlActionTest {

    private class SutFixture(
        scope: TestScope,
        localVersion: String? = "v1",
        baseUrl: String? = null,
        legacyHtml: String = ""
    ) {
        val rest = FakeRestClient()
        val prefs = mockk<SharedPrefsManager>()
        val files = mockk<FileManager>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var settingsReadOnIo = false
        val sut = PreloadIamHtmlAction(rest.api(dispatchers), prefs, files, dispatchers, traced.runner)

        init {
            every { prefs.getIamBaseHtmlContent() } answers {
                settingsReadOnIo = dispatchers.markedIo.isRunningHere
                legacyHtml
            }
            justRun { prefs.clearBaseHtmlContent() }
            every { prefs.getIamBaseUrl() } returns baseUrl
            every { prefs.getIamBaseHtmlVersion() } returns localVersion
            justRun { prefs.saveIamBaseHtmlVersion(any()) }
            coEvery { files.saveBaseHtmlContent(any()) } returns Result.success(Unit)
        }

        fun serverHasVersion(version: String) {
            rest.succeedWith("", headers = mapOf("x-amz-meta-version" to listOf(version)))
        }
    }

    @Test
    fun `should move base html kept in local settings to file`() = runUnitTest {
        given()
        val fixture = SutFixture(this, legacyHtml = "<html>old</html>")
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        coVerify(exactly = 1) { fixture.files.saveBaseHtmlContent("<html>old</html>") }
        verify(exactly = 1) { fixture.prefs.clearBaseHtmlContent() }
    }

    @Test
    fun `should read local settings on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.settingsReadOnIo)
    }

    @Test
    fun `should leave settings alone when they keep no base html`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.prefs.clearBaseHtmlContent() }
    }

    @Test
    fun `should ask default address for base html version`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        val request = fixture.rest.requests.single()
        assertEquals(HttpMethod.HEAD, request.method)
        assertEquals(ApiContract.InAppMessages.BaseHtml, request.contract)
    }

    @Test
    fun `should ask configured address for base html version`() = runUnitTest {
        given()
        val fixture = SutFixture(this, baseUrl = "https://cdn.example.com/base.html")
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        assertEquals("https://cdn.example.com/base.html", fixture.rest.requests.single().contract.url)
    }

    @Test
    fun `should not download base html that is up to date`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.serverHasVersion("v1")

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
        verify(exactly = 0) { fixture.prefs.saveIamBaseHtmlVersion(any()) }
    }

    @Test
    fun `should keep current base html when its version cannot be checked`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.rest.failWith(403)

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
        verify(exactly = 0) { fixture.prefs.saveIamBaseHtmlVersion(any()) }
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ base html version unknown, keeping current html") })
    }

    @Test
    fun `should keep old version so next preload retries when file cannot be written`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.serverHasVersion("v2")
        fixture.rest.succeedWith("<html>new</html>")
        coEvery { fixture.files.saveBaseHtmlContent(any()) } returns Result.failure(IOException("disk full"))

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.prefs.saveIamBaseHtmlVersion(any()) }
    }

    @Test
    fun `should treat version missing from server answer as default version`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = null)
        fixture.rest.succeedWith("")

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should treat empty version header as default version`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = null)
        fixture.rest.succeedWith("", headers = mapOf("x-amz-meta-version" to emptyList()))

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.rest.requests.size)
    }

    @Test
    fun `should download newer base html from the same address`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.serverHasVersion("v2")
        fixture.rest.succeedWith("<html>new</html>")

        whenn()
        fixture.sut()

        then()
        val download = fixture.rest.requests[1]
        assertEquals(HttpMethod.GET, download.method)
        assertEquals(ApiContract.InAppMessages.BaseHtml, download.contract)
    }

    @Test
    fun `should save newer base html to file before its version`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.serverHasVersion("v2")
        fixture.rest.succeedWith("<html>new</html>")

        whenn()
        fixture.sut()

        then()
        coVerify(exactly = 1) { fixture.files.saveBaseHtmlContent("<html>new</html>") }
        verifyOrder {
            fixture.prefs.getIamBaseHtmlVersion()
            fixture.prefs.saveIamBaseHtmlVersion("v2")
        }
    }

    @Test
    fun `should keep old version so next preload retries when download fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1")
        fixture.serverHasVersion("v2")
        fixture.rest.failWith(503)

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.prefs.saveIamBaseHtmlVersion(any()) }
        coVerify(exactly = 0) { fixture.files.saveBaseHtmlContent(any()) }
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ base html not updated, retrying on next preload") })
    }

    @Test
    fun `should not trace base html address or content`() = runUnitTest {
        given()
        val fixture = SutFixture(this, localVersion = "v1", baseUrl = "https://cdn.example.com/base.html")
        fixture.serverHasVersion("v2")
        fixture.rest.succeedWith("<html>new</html>")

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.traced.sink.lines.none { it.contains("cdn.example.com") || it.contains("<html>") })
    }
}
