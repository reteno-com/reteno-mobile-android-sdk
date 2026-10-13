package com.reteno.fcm

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.RemoteMessage
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.RetenoInternalFacade
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Constants.BROADCAST_ACTION_CUSTOM_PUSH
import com.reteno.core.util.Procedure
import com.reteno.push.Constants
import com.reteno.push.RetenoNotificationService
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoFirebaseMessagingServiceTest {

    private class SutFixture {
        val application: Application = ApplicationProvider.getApplicationContext()
        val facade = mockk<RetenoInternalFacade>(relaxed = true).also {
            every { it.getDefaultNotificationChannelConfig() } returns null
            every { it.getDefaultNotificationChannel() } returns ""
        }
        val notifications = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun createService(): RetenoFirebaseMessagingService =
            Robolectric.buildService(RetenoFirebaseMessagingService::class.java).create().get()

        fun message(vararg data: Pair<String, String>): RemoteMessage =
            RemoteMessage.Builder("sender@fcm.googleapis.com").setData(mapOf(*data)).build()

        inline fun <T> withFacade(block: () -> T): T {
            RetenoInternalEntryPoint.install(facade)
            try {
                return block()
            } finally {
                RetenoInternalEntryPoint.install(null)
            }
        }

        inline fun <T> onAndroidSdk(sdk: Int, block: () -> T): T {
            val actual = Build.VERSION.SDK_INT
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
            try {
                return block()
            } finally {
                ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", actual)
            }
        }
    }

    @Test
    fun `should show reteno push and record its delivery`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val service = fixture.createService()

        whenn()
        fixture.withFacade { service.onMessageReceived(fixture.message(Constants.KEY_ES_INTERACTION_ID to "interaction-1", Constants.KEY_ES_TITLE to "Title")) }

        then()
        assertEquals(1, shadowOf(fixture.notifications).allNotifications.size)
        verify(exactly = 1) { fixture.facade.recordInteraction("interaction-1", InteractionStatus.DELIVERED, true) }
    }

    @Test
    fun `should broadcast message without interaction id as custom push`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val service = fixture.createService()

        whenn()
        fixture.withFacade { service.onMessageReceived(fixture.message("custom" to "value")) }

        then()
        val custom = shadowOf(fixture.application).broadcastIntents.single { it.action == BROADCAST_ACTION_CUSTOM_PUSH }
        assertEquals("value", custom.getStringExtra("custom"))
        assertTrue(shadowOf(fixture.notifications).allNotifications.isEmpty())
    }

    @Test
    fun `should pass new token to sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val service = fixture.createService()

        whenn()
        fixture.withFacade { service.onNewToken("new-token") }

        then()
        verify(exactly = 1) { fixture.facade.onNewFcmToken("new-token") }
    }

    @Test
    fun `should recognise reteno messages by interaction id`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val service = fixture.createService()

        whenn()
        val results = listOf(
            fixture.message(Constants.KEY_ES_INTERACTION_ID to "interaction-1"),
            fixture.message("custom" to "value")
        ).map(service::isRetenoMessage)

        then()
        assertEquals(listOf(true, false), results)
    }

    @Test
    fun `should not crash when sdk fails to handle push`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val service = fixture.createService()
        every { fixture.facade.onNewFcmToken(any()) } throws IllegalStateException("sdk not ready")
        every { fixture.facade.getDefaultNotificationChannelConfig() } throws IllegalStateException("sdk not ready")

        whenn()
        val results = fixture.withFacade {
            listOf(
                runCatching { service.onNewToken("new-token") },
                runCatching { service.onMessageReceived(fixture.message(Constants.KEY_ES_INTERACTION_ID to "interaction-1")) }
            )
        }

        then()
        assertTrue(results.all { it.isSuccess })
        assertTrue(shadowOf(fixture.notifications).allNotifications.isEmpty())
    }

    @Test
    fun `should ignore push events on unsupported android version`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withFacade {
            fixture.onAndroidSdk(Build.VERSION_CODES.N_MR1) {
                val service = fixture.createService()
                service.onNewToken("new-token")
                service.onMessageReceived(fixture.message(Constants.KEY_ES_INTERACTION_ID to "interaction-1"))
            }
        }

        then()
        verify(exactly = 0) { fixture.facade.onNewFcmToken(any()) }
        assertTrue(shadowOf(fixture.notifications).allNotifications.isEmpty())
    }

    @Test
    fun `should let host services forward their events through the handler`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val handler = RetenoFirebaseServiceHandler(RetenoNotificationService(fixture.application))
        var channelConfigRead = false
        every { fixture.facade.getDefaultNotificationChannelConfig() } answers {
            channelConfigRead = true
            Procedure<NotificationChannelCompat.Builder> { }
        }

        whenn()
        fixture.withFacade {
            handler.onCreate()
            handler.onNewToken("new-token")
            handler.onMessageReceived(fixture.message(Constants.KEY_ES_INTERACTION_ID to "interaction-1"))
        }

        then()
        verify(exactly = 1) { fixture.facade.onNewFcmToken("new-token") }
        assertTrue(channelConfigRead)
        assertFalse(shadowOf(fixture.notifications).allNotifications.isEmpty())
    }
}
