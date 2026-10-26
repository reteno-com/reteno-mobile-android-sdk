package com.reteno.push

import android.app.NotificationChannel
import android.app.NotificationManager
import com.reteno.core.util.Constants.BROADCAST_ACTION_CUSTOM_PUSH
import com.reteno.push.fixtures.STUB_INTERACTION_ID
import com.reteno.push.fixtures.notificationManager
import com.reteno.push.fixtures.stubPushBundle
import com.reteno.push.fixtures.testApplication
import com.reteno.push.fixtures.withFakeFacade
import com.reteno.push.receiver.CustomPushReceiver
import com.reteno.push.receiver.PushReceivedReceiver
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationServiceTest {

    private class SutFixture {
        val application = testApplication()
        val manager = application.notificationManager()
        val sut = RetenoNotificationService(application)

        fun broadcastsTo(receiver: Class<*>) =
            shadowOf(application).broadcastIntents.filter { it.component?.className == receiver.name }
    }

    @Test
    fun `should pass new push token to sdk`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.onNewToken("token")

            then()
            assertEquals(listOf("onNewFcmToken token"), facade.calls)
        }
    }

    @Test
    fun `should show reteno notification on default channel`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            val notifications = shadowOf(fixture.manager).allNotifications
            assertEquals(1, notifications.size)
            assertEquals("defaultId", notifications.single().channelId)
            assertTrue(fixture.manager.getNotificationChannel("defaultId") != null)
        }
    }

    @Test
    fun `should record delivery when notifications and default channel are enabled`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            assertTrue(facade.calls.contains("recordInteraction $STUB_INTERACTION_ID DELIVERED forcePush=true"))
        }
    }

    @Test
    fun `should not report notifications state to sdk when handling push`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            val expected = listOf(
                "saveDefaultNotificationChannel",
                "recordInteraction $STUB_INTERACTION_ID DELIVERED forcePush=true"
            )
            assertEquals(expected, facade.calls)
        }
    }

    @Test
    fun `should not record delivery when notifications are disabled`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            shadowOf(fixture.manager).setNotificationsEnabled(false)

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            assertFalse(facade.calls.any { it.startsWith("recordInteraction") })
        }
    }

    @Test
    fun `should not record delivery when default channel is blocked`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            val blockedChannel = NotificationChannel("defaultId", "name", NotificationManager.IMPORTANCE_NONE)
            fixture.manager.createNotificationChannel(blockedChannel)

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            assertFalse(facade.calls.any { it.startsWith("recordInteraction") })
        }
    }

    @Test
    fun `should forward reteno notification to host push received receiver`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.handleNotification(stubPushBundle())

            then()
            val forwarded = fixture.broadcastsTo(PushReceivedReceiver::class.java)
            val forwardedIds = forwarded.map { it.getStringExtra(Constants.KEY_ES_INTERACTION_ID) }
            assertEquals(listOf(STUB_INTERACTION_ID), forwardedIds)
        }
    }

    @Test
    fun `should broadcast push without interaction id as custom push`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.sut.handleNotification(stubPushBundle(interactionId = null, "custom" to "value"))

            then()
            val custom = fixture.broadcastsTo(CustomPushReceiver::class.java).single()
            assertEquals(BROADCAST_ACTION_CUSTOM_PUSH, custom.action)
            assertEquals("value", custom.getStringExtra("custom"))
            assertTrue(shadowOf(fixture.manager).allNotifications.isEmpty())
            assertTrue(facade.calls.isEmpty())
        }
    }
}
