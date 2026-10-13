package com.reteno.push.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import com.reteno.core.util.Constants
import com.reteno.push.fixtures.notificationManager
import com.reteno.push.fixtures.onAndroidSdk
import com.reteno.push.fixtures.testApplication
import com.reteno.push.fixtures.withFakeFacade
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
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class NotificationStateReceiversTest {

    private class SutFixture {
        val application = testApplication()
        val manager = application.notificationManager()
        val receivers: List<BroadcastReceiver> = listOf(AppResumeReceiver(), NotificationChannelStateChangeReceiver(), PushPermissionChangedReceiver())

        fun broadcast(action: String) {
            application.sendBroadcast(Intent(action).setPackage(application.packageName))
            ShadowLooper.idleMainLooper()
        }
    }

    @Test
    fun `should report enabled notifications when permission and default channel allow them`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.receivers.forEach { it.onReceive(fixture.application, Intent()) }

            then()
            assertEquals(List(3) { "notificationsEnabled true" }, facade.calls.filter { it.startsWith("notificationsEnabled") })
        }
    }

    @Test
    fun `should report disabled notifications when app notifications are blocked`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            shadowOf(fixture.manager).setNotificationsEnabled(false)

            whenn()
            NotificationsEnabledManager.onCheckState(fixture.application)

            then()
            assertTrue(facade.calls.contains("notificationsEnabled false"))
        }
    }

    @Test
    fun `should report disabled notifications when default channel is blocked`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            fixture.manager.createNotificationChannel(NotificationChannel("defaultId", "name", NotificationManager.IMPORTANCE_NONE))

            whenn()
            NotificationsEnabledManager.onCheckState(fixture.application)

            then()
            assertEquals(listOf("notificationsEnabled false"), facade.calls)
        }
    }

    @Test
    fun `should check notification state on sdk broadcasts`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.broadcast(Constants.BROADCAST_ACTION_RETENO_APP_RESUME)
            fixture.broadcast(Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED)
            fixture.broadcast(Constants.BROADCAST_ACTION_RETENO_APP_PAUSE)

            then()
            assertEquals(2, facade.calls.count { it.startsWith("notificationsEnabled") })
        }
    }

    @Test
    fun `should ignore broadcast without context`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.receivers.forEach { it.onReceive(null, Intent()) }

            then()
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should not crash when sdk fails to take notification state`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.failure = IllegalStateException("sdk not ready")

            whenn()
            val results = fixture.receivers.map { receiver -> runCatching { receiver.onReceive(fixture.application, Intent()) } }

            then()
            assertTrue(results.all { it.isSuccess })
        }
    }

    @Test
    fun `should ignore broadcasts on unsupported android version`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            onAndroidSdk(Build.VERSION_CODES.N_MR1) { fixture.receivers.forEach { it.onReceive(fixture.application, Intent()) } }

            then()
            assertTrue(facade.calls.isEmpty())
        }
    }
}
