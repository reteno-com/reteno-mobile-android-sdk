package com.reteno.core.view.iam.container

import android.app.Application
import android.webkit.ConsoleMessage
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.AndroidInAppScreen
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class InAppWebViewTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val bridge = object : RetenoAndroidHandler() {
            override fun onMessagePosted(event: String?) = Unit
        }
    }

    @Test
    fun `should load in-app html with the sdk bridge and javascript on`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val webView = createInAppWebView(fixture.application, fixture.bridge, HTML)

        then()
        val loaded = shadowOf(webView).lastLoadDataWithBaseURL
        val expected = listOf("", HTML, "text/html", "UTF-8")
        assertEquals(expected, listOf(loaded.baseUrl, loaded.data, loaded.mimeType, loaded.encoding))
        assertSame(fixture.bridge, shadowOf(webView).getJavascriptInterface(AndroidInAppScreen.JS_INTERFACE_NAME))
        assertEquals(listOf(true, true), listOf(webView.settings.javaScriptEnabled, webView.settings.domStorageEnabled))
    }

    @Test
    fun `should keep in-app html away from local files and content providers`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val webView = createInAppWebView(fixture.application, fixture.bridge, HTML)

        then()
        val settings = webView.settings
        assertEquals(listOf(false, false), listOf(settings.allowFileAccess, settings.allowContentAccess))
    }

    @Test
    fun `should log in-app console messages through the sdk log without personal data`() = runUnitTest {
        given()
        val sink = FakeLogSink()
        val message = ConsoleMessage("sent to john@example.com", "", 12, ConsoleMessage.MessageLevel.LOG)

        whenn()
        Logger.install(sink, null)
        val handled = try {
            InAppConsole.onConsoleMessage(message)
        } finally {
            Logger.install(AndroidLogSink(), null)
        }

        then()
        assertTrue(handled)
        assertEquals(listOf("onConsoleMessage(): sent to <email> at line 12"), sink.lines)
    }

    companion object {
        private const val HTML = "<html><body>Sale</body></html>"
    }
}
