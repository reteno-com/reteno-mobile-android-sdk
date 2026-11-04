package com.reteno.push.interceptor.click

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.reteno.push.Constants
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class IntentHandlerTest {

    @Test
    fun `should prefer unwrapped notification link`() = runUnitTest {
        given()
        val bundle = links(
            false,
            Constants.KEY_ES_LINK_UNWRAPPED to "app://raw",
            Constants.KEY_ES_LINK_WRAPPED to "https://wrapped"
        )

        whenn()
        val intent = IntentHandler.getDeepLinkIntent(bundle)!!

        then()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse("app://raw"), intent.data)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags)
        assertEquals("app://raw", intent.getStringExtra(Constants.KEY_ES_LINK_UNWRAPPED))
    }

    @Test
    fun `should fall back through notification links`() = runUnitTest {
        given()
        val wrappedOnly = links(false, Constants.KEY_ES_LINK_WRAPPED to "https://wrapped")

        whenn()
        val results = listOf(wrappedOnly.deeplink(), links(false).deeplink())

        then()
        assertEquals(listOf("https://wrapped", null), results)
    }

    @Test
    fun `should fall back from button links to notification links`() = runUnitTest {
        given()
        val bundles = listOf(
            links(
                true,
                Constants.KEY_BTN_ACTION_LINK_UNWRAPPED to "app://button-raw",
                Constants.KEY_ES_LINK_UNWRAPPED to "app://raw"
            ),
            links(
                true,
                Constants.KEY_BTN_ACTION_LINK_WRAPPED to "https://button-wrapped",
                Constants.KEY_ES_LINK_UNWRAPPED to "app://raw"
            ),
            links(
                true,
                Constants.KEY_ES_LINK_UNWRAPPED to "app://raw",
                Constants.KEY_ES_LINK_WRAPPED to "https://wrapped"
            ),
            links(true, Constants.KEY_ES_LINK_WRAPPED to "https://wrapped")
        )

        whenn()
        val results = bundles.map { it.deeplink() }

        then()
        assertEquals(listOf("app://button-raw", "https://button-wrapped", "app://raw", "https://wrapped"), results)
    }

    @Test
    fun `should have no deeplink for button without any link`() = runUnitTest {
        given()
        val bundle = links(true)

        whenn()
        val intent = IntentHandler.getDeepLinkIntent(bundle)

        then()
        assertNull(intent)
    }

    private fun links(button: Boolean, vararg links: Pair<String, String>) = Bundle().apply {
        if (button) putBoolean(Constants.KEY_ACTION_BUTTON, true)
        links.forEach { (key, value) -> putString(key, value) }
    }

    private fun Bundle.deeplink(): String? = IntentHandler.getDeepLinkIntent(this)?.data?.toString()
}
