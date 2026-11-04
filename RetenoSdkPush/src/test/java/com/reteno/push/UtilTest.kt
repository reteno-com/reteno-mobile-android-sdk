package com.reteno.push

import android.os.Bundle
import com.reteno.push.fixtures.applicationMetaData
import com.reteno.push.fixtures.stubPushBundle
import com.reteno.push.fixtures.testApplication
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class UtilTest {

    @Test
    fun `should not forward push events when app removed sdk receivers`() = runUnitTest {
        given()
        val application = testApplication()
        application.applicationMetaData().remove(Constants.META_DATA_KEY_CUSTOM_RECEIVER_PUSH_RECEIVED)
        application.applicationMetaData().remove(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_CLICKED)

        whenn()
        Util.tryToSendToCustomReceiverPushReceived(application, stubPushBundle())
        Util.tryToSendToCustomReceiverNotificationClicked(application, stubPushBundle())

        then()
        assertTrue(shadowOf(application).broadcastIntents.isEmpty())
    }

    @Test
    fun `should read button links for action button and notification links otherwise`() = runUnitTest {
        given()
        val button = Bundle().apply {
            putBoolean(Constants.KEY_ACTION_BUTTON, true)
            putString(Constants.KEY_BTN_ACTION_LINK_WRAPPED, "https://track.example/b")
            putString(Constants.KEY_BTN_ACTION_LINK_UNWRAPPED, "https://example.com/b")
        }
        val notification = Bundle().apply {
            putString(Constants.KEY_ES_LINK_WRAPPED, "https://track.example/n")
            putString(Constants.KEY_ES_LINK_UNWRAPPED, "https://example.com/n")
        }
        val buttonWithoutLinks = Bundle().apply { putBoolean(Constants.KEY_ACTION_BUTTON, true) }

        whenn()
        val links = listOf(button, notification, buttonWithoutLinks, Bundle()).map(Util::getLinkFromBundle)

        then()
        assertEquals(
            listOf(
                "https://track.example/b" to "https://example.com/b",
                "https://track.example/n" to "https://example.com/n",
                "" to "",
                "" to ""
            ),
            links
        )
    }
}
