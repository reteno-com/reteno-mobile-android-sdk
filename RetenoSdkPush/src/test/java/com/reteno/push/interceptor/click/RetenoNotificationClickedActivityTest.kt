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
import com.reteno.push.fixtures.STUB_NOTIFICATION_ID
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationClickedActivityTest {

    private class SutFixture(launchable: Boolean = true) {
        val application = testApplication()

        init {
            if (launchable) application.registerLaunchActivity()
        }

        fun click(extras: Bundle?): RetenoNotificationClickedActivity {
            val intent = Intent(application, RetenoNotificationClickedActivity::class.java)
                .apply { extras?.let(::putExtras) }
            return Robolectric.buildActivity(RetenoNotificationClickedActivity::class.java, intent).create().get()
        }

        fun startedActivities(activity: RetenoNotificationClickedActivity): List<Intent> =
            generateSequence { shadowOf(activity).nextStartedActivity }.toList()

        fun hostBroadcasts(): List<Intent> = shadowOf(application).broadcastIntents
            .filter { it.component?.className == ReceiverNotificationClicked::class.java.name }
    }

    @Test
    fun `should record notification click and finish`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            val activity = fixture.click(stubPushBundle())

            then()
            assertEquals(listOf("recordInteraction $STUB_INTERACTION_ID CLICKED forcePush=true"), facade.calls)
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `should record action button click as open url action`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubActionButtonBundle())

            then()
            val expected = "recordInteractionAction $STUB_INTERACTION_ID OPEN_URL $STUB_BUTTON_ID " +
                "$STUB_BUTTON_URL forcePush=true"
            assertEquals(listOf(expected), facade.calls.filter { it.startsWith("recordInteraction") })
        }
    }

    @Test
    fun `should dismiss notification of clicked action button`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            fixture.application.postStubNotification()

            whenn()
            fixture.click(stubActionButtonBundle())

            then()
            assertTrue(shadowOf(fixture.application.notificationManager()).allNotifications.isEmpty())
        }
    }

    @Test
    fun `should finish after action button click`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            val activity = fixture.click(stubActionButtonBundle())

            then()
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `should forward click to host receiver`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            fixture.click(stubPushBundle())

            then()
            val forwarded = fixture.hostBroadcasts().single()
            assertEquals(STUB_INTERACTION_ID, forwarded.getStringExtra(Constants.KEY_ES_INTERACTION_ID))
        }
    }

    @Test
    fun `should not forward notification id to host receiver`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            val extras = stubPushBundle().apply { putInt(Constants.KEY_NOTIFICATION_ID, STUB_NOTIFICATION_ID) }

            whenn()
            fixture.click(extras)

            then()
            assertFalse(fixture.hostBroadcasts().single().hasExtra(Constants.KEY_NOTIFICATION_ID))
        }
    }

    @Test
    fun `should open app with notification intent when push has no link`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            val activity = fixture.click(stubPushBundle(STUB_INTERACTION_ID, "custom" to "value"))

            then()
            val started = fixture.startedActivities(activity).single()
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
            val activity = fixture.click(extras)

            then()
            assertTrue(facade.calls.contains("deeplinkClicked https://track.example/c/1 https://example.com/offer"))
            assertEquals(Uri.parse("https://example.com/offer"), fixture.startedActivities(activity).single().data)
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
            val activity = fixture.click(extras)

            then()
            assertEquals(LAUNCH_ACTIVITY, fixture.startedActivities(activity).single().component?.className)
        }
    }

    @Test
    fun `should not open app for in-app message when an activity is already shown`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.activityPresented = true

            whenn()
            val activity = fixture.click(stubPushBundle(STUB_INTERACTION_ID, Constants.KEY_ES_IAM to "1"))

            then()
            assertEquals(listOf("initializeIamView $STUB_INTERACTION_ID"), facade.calls)
            assertTrue(fixture.startedActivities(activity).isEmpty())
        }
    }

    @Test
    fun `should open app when click carries no extras`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            val activity = fixture.click(null)

            then()
            assertEquals(LAUNCH_ACTIVITY, fixture.startedActivities(activity).single().component?.className)
        }
    }

    @Test
    fun `should not open anything when app has no launch activity`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture(launchable = false)

            whenn()
            val activity = fixture.click(stubPushBundle())

            then()
            assertTrue(fixture.startedActivities(activity).isEmpty())
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `should finish without crashing when sdk fails to record click`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.failure = IllegalStateException("sdk not ready")

            whenn()
            val activity = fixture.click(stubPushBundle())

            then()
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `should ignore click on unsupported android version`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            val activity = onAndroidSdk(Build.VERSION_CODES.N_MR1) { fixture.click(stubPushBundle()) }

            then()
            assertTrue(facade.calls.isEmpty())
            assertFalse(activity.isFinishing)
        }
    }
}
