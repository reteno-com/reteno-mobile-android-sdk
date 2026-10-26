package com.reteno.push.interceptor.click

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.reteno.push.Constants
import com.reteno.push.fixtures.LAUNCH_ACTIVITY
import com.reteno.push.fixtures.STUB_BUTTON_ID
import com.reteno.push.fixtures.STUB_BUTTON_URL
import com.reteno.push.fixtures.STUB_INTERACTION_ID
import com.reteno.push.fixtures.notificationManager
import com.reteno.push.fixtures.onAndroidSdk
import com.reteno.push.fixtures.postStubNotification
import com.reteno.push.fixtures.registerLaunchActivity
import com.reteno.push.fixtures.stubActionButtonBundle
import com.reteno.push.fixtures.stubPushBundle
import com.reteno.push.fixtures.testApplication
import com.reteno.push.fixtures.withFakeFacade
import com.reteno.push.receiver.ReceiverNotificationClicked
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationClickedReceiverTest {

    private class SutFixture(launchable: Boolean = true) {
        val application = testApplication()
        val sut = RetenoNotificationClickedReceiver()

        init {
            if (launchable) application.registerLaunchActivity()
        }

        fun click(extras: Bundle?) = sut.onReceive(application, Intent().apply { extras?.let(::putExtras) })

        fun startedActivities(): List<Intent> = generateSequence { shadowOf(application).nextStartedActivity }.toList()

        fun hostBroadcasts(): List<Intent> = shadowOf(application).broadcastIntents
            .filter { it.component?.className == ReceiverNotificationClicked::class.java.name }
    }

    @Test
    fun `should record notification click`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle())

            then()
            assertEquals(listOf("recordInteraction $STUB_INTERACTION_ID CLICKED forcePush=true"), facade.calls)
        }
    }

    @Test
    fun `should record action button click and close its notification`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            fixture.application.postStubNotification()

            whenn()
            fixture.click(stubActionButtonBundle())

            then()
            val expectedCall = "recordInteractionAction $STUB_INTERACTION_ID OPEN_URL $STUB_BUTTON_ID " +
                "$STUB_BUTTON_URL forcePush=true"
            assertEquals(expectedCall, facade.calls.first())
            assertTrue(shadowOf(fixture.application.notificationManager()).allNotifications.isEmpty())
        }
    }

    @Test
    fun `should not record click without interaction id`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle(interactionId = null))

            then()
            assertTrue(facade.calls.isEmpty())
            assertEquals(LAUNCH_ACTIVITY, fixture.startedActivities().single().component?.className)
        }
    }

    @Test
    fun `should forward click to host receiver without notification id`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            val extras = stubPushBundle().apply { putInt(Constants.KEY_NOTIFICATION_ID, 7) }

            whenn()
            fixture.click(extras)

            then()
            val forwarded = fixture.hostBroadcasts().single()
            assertEquals(STUB_INTERACTION_ID, forwarded.getStringExtra(Constants.KEY_ES_INTERACTION_ID))
            assertFalse(forwarded.hasExtra(Constants.KEY_NOTIFICATION_ID))
        }
    }

    @Test
    fun `should open app with notification extras when push has no link`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle(STUB_INTERACTION_ID, "custom" to "value"))

            then()
            val started = fixture.startedActivities().single()
            assertEquals(LAUNCH_ACTIVITY, started.component?.className)
            assertEquals("value", started.getStringExtra("custom"))
        }
    }

    @Test
    fun `should track deeplink click and open the link`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            val extras = stubPushBundle(
                STUB_INTERACTION_ID,
                Constants.KEY_ES_LINK_WRAPPED to "https://track.example/c/1",
                Constants.KEY_ES_LINK_UNWRAPPED to "https://example.com/offer"
            )

            whenn()
            fixture.click(extras)

            then()
            assertTrue(facade.calls.contains("deeplinkClicked https://track.example/c/1 https://example.com/offer"))
            val started = fixture.startedActivities().single()
            assertEquals(Intent.ACTION_VIEW, started.action)
            assertEquals(Uri.parse("https://example.com/offer"), started.data)
        }
    }

    @Test
    fun `should open app when no activity can handle the link`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            shadowOf(fixture.application).checkActivities(true)
            val extras = stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_LINK_UNWRAPPED to "unknown://offer")

            whenn()
            fixture.click(extras)

            then()
            val started = fixture.startedActivities().single()
            assertEquals(LAUNCH_ACTIVITY, started.component?.className)
            assertEquals("unknown://offer", started.getStringExtra(Constants.KEY_ES_LINK_UNWRAPPED))
        }
    }

    @Test
    fun `should show in-app message from push instead of recording click`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_IAM to "1"))

            then()
            assertEquals(listOf("initializeIamView $STUB_INTERACTION_ID"), facade.calls)
            assertEquals(LAUNCH_ACTIVITY, fixture.startedActivities().single().component?.className)
        }
    }

    @Test
    fun `should not open app for in-app message when an activity is already shown`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.activityPresented = true

            whenn()
            fixture.click(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_IAM to "1"))

            then()
            assertTrue(fixture.startedActivities().isEmpty())
        }
    }

    @Test
    fun `should open app for in-app flag without interaction id`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle(interactionId = null, Constants.KEY_ES_IAM to "1"))

            then()
            assertTrue(facade.calls.isEmpty())
            assertEquals(1, fixture.startedActivities().size)
        }
    }

    @Test
    fun `should open app when click carries no extras`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(null)

            then()
            assertTrue(facade.calls.isEmpty())
            assertEquals(LAUNCH_ACTIVITY, fixture.startedActivities().single().component?.className)
        }
    }

    @Test
    fun `should not forward click without extras to host receiver`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(null)

            then()
            assertTrue(fixture.hostBroadcasts().isEmpty())
        }
    }

    @Test
    fun `should ignore click without intent`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.onReceive(fixture.application, null)

            then()
            assertTrue(facade.calls.isEmpty())
            assertTrue(fixture.startedActivities().isEmpty())
        }
    }

    @Test
    fun `should not open anything when app has no launch activity`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture(launchable = false)

            whenn()
            fixture.click(stubPushBundle())

            then()
            assertTrue(fixture.startedActivities().isEmpty())
        }
    }

    @Test
    fun `should not crash when sdk fails to record click`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.failure = IllegalStateException("sdk not ready")

            whenn()
            val result = runCatching { fixture.click(stubPushBundle()) }

            then()
            assertTrue(result.isSuccess)
            assertTrue(fixture.startedActivities().isEmpty())
        }
    }

    @Test
    fun `should ignore click on unsupported android version`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            onAndroidSdk(Build.VERSION_CODES.N_MR1) { fixture.click(stubPushBundle()) }

            then()
            assertTrue(facade.calls.isEmpty())
            assertNull(shadowOf(fixture.application).nextStartedActivity)
        }
    }
}
