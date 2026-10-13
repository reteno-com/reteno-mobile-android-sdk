package com.reteno.push

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.reteno.core.permission.AndroidPermissionChecker
import com.reteno.push.fixtures.FakeRetenoFacade
import com.reteno.push.fixtures.notificationManager
import com.reteno.push.fixtures.onAndroidSdk
import com.reteno.push.fixtures.testApplication
import com.reteno.push.fixtures.withFakeFacade
import com.reteno.push.permission.NotificationStatus
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationsTest {

    private class SutFixture(val facade: FakeRetenoFacade) {
        val application = testApplication()
        val manager = application.notificationManager()
        val activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val checker = mockk<AndroidPermissionChecker>().also {
            coEvery { it.awaitActivity() } returns activity
            coEvery { it.awaitContext() } returns activity
            coEvery { it.check(Manifest.permission.POST_NOTIFICATIONS) } returns false
        }

        fun withChecker() = apply { facade.permissionChecker = checker }

        fun grantNotificationPermission() {
            shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }

        fun showRationale() {
            shadowOf(application.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS, true)
        }

        inline fun <T> onTestMain(scope: TestScope, block: () -> T): T {
            Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))
            try {
                return block()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    @Test
    fun `should not touch default channel when nothing changes`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade)

            whenn()
            RetenoNotifications.updateDefaultNotificationChannel()

            then()
            assertTrue(facade.calls.isEmpty())
            assertNull(fixture.manager.getNotificationChannel("defaultId"))
        }
    }

    @Test
    fun `should rename default channel after sdk initialization`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade)
            fixture.manager.createNotificationChannel(NotificationChannel("defaultId", "name", NotificationManager.IMPORTANCE_HIGH).apply { description = "old" })

            whenn()
            RetenoNotifications.updateDefaultNotificationChannel(name = "Offers")

            then()
            val channel = fixture.manager.getNotificationChannel("defaultId")
            assertEquals(listOf("executeAfterInit"), facade.calls)
            assertEquals("Offers", channel.name)
            assertEquals("old", channel.description)
        }
    }

    @Test
    fun `should describe default channel after sdk initialization`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade)
            fixture.manager.createNotificationChannel(NotificationChannel("defaultId", "name", NotificationManager.IMPORTANCE_HIGH))

            whenn()
            RetenoNotifications.updateDefaultNotificationChannel(description = "Discounts")

            then()
            val channel = fixture.manager.getNotificationChannel("defaultId")
            assertEquals("name", channel.name)
            assertEquals("Discounts", channel.description)
        }
    }

    @Test
    fun `should create default channel with new name when it does not exist yet`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade)

            whenn()
            RetenoNotifications.updateDefaultNotificationChannel(name = "Offers")

            then()
            val channel = fixture.manager.getNotificationChannel("defaultId")
            assertEquals("Offers", channel.name)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        }
    }

    @Test
    fun `should not grant notification permission without an activity to ask from`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade)

            whenn()
            val granted = fixture.onTestMain(this) { RetenoNotifications.requestNotificationPermission() }

            then()
            assertFalse(granted)
        }
    }

    @Test
    fun `should treat notification permission as granted before android 13`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()

            whenn()
            val granted = fixture.onTestMain(this) { RetenoNotifications.requestNotificationPermission() }

            then()
            assertTrue(granted)
            coVerify(exactly = 0) { fixture.checker.check(any<String>()) }
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should not ask again for granted notification permission`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()
            fixture.grantNotificationPermission()

            whenn()
            val granted = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.requestNotificationPermission() } }

            then()
            assertTrue(granted)
            coVerify(exactly = 0) { fixture.checker.check(any<String>()) }
        }
    }

    @Test
    fun `should report push permission status when user grants notification permission`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()
            coEvery { fixture.checker.check(Manifest.permission.POST_NOTIFICATIONS) } returns true

            whenn()
            val granted = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.requestNotificationPermission() } }

            then()
            assertTrue(granted)
            assertEquals(listOf("updatePushPermissionStatus"), facade.calls)
        }
    }

    @Test
    fun `should not report push permission status when user denies notification permission`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()

            whenn()
            val granted = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.requestNotificationPermission() } }

            then()
            assertFalse(granted)
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should resolve permission request future`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            SutFixture(facade)

            whenn()
            val granted = RetenoNotifications.requestNotificationPermissionFuture().await()

            then()
            assertFalse(granted)
        }
    }

    @Test
    fun `should report denied status without an activity to ask from`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            SutFixture(facade)

            whenn()
            val status = RetenoNotifications.getNotificationPermissionStatusFuture().await()

            then()
            assertEquals(NotificationStatus.DENIED, status)
        }
    }

    @Test
    fun `should report allowed status before android 13 when notifications are enabled`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            SutFixture(facade).withChecker()

            whenn()
            val status = RetenoNotifications.getNotificationPermissionStatus()

            then()
            assertEquals(NotificationStatus.ALLOWED, status)
        }
    }

    @Test
    fun `should report permanently denied status before android 13 when notifications are blocked`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()
            shadowOf(fixture.manager).setNotificationsEnabled(false)

            whenn()
            val status = RetenoNotifications.getNotificationPermissionStatus()

            then()
            assertEquals(NotificationStatus.PERMANENTLY_DENIED, status)
        }
    }

    @Test
    fun `should report allowed status when notification permission is granted`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()
            fixture.grantNotificationPermission()

            whenn()
            val status = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.getNotificationPermissionStatus() } }

            then()
            assertEquals(NotificationStatus.ALLOWED, status)
        }
    }

    @Test
    fun `should report denied status when user can still be asked again`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()
            fixture.showRationale()

            whenn()
            val status = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.getNotificationPermissionStatus() } }

            then()
            assertEquals(NotificationStatus.DENIED, status)
        }
    }

    @Test
    fun `should report permanently denied status when user can not be asked again`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture(facade).withChecker()

            whenn()
            val status = fixture.onTestMain(this) { onAndroidSdk(Build.VERSION_CODES.TIRAMISU) { RetenoNotifications.getNotificationPermissionStatus() } }

            then()
            assertEquals(NotificationStatus.PERMANENTLY_DENIED, status)
        }
    }

    @Test
    fun `should keep grouping rule for new notifications`() = runUnitTest {
        given()
        val rule: (Map<String, String?>) -> String? = { "group" }

        whenn()
        RetenoNotifications.setGroupingRule(rule)
        val stored = RetenoNotifications.groupingRule
        RetenoNotifications.setGroupingRule(null)

        then()
        assertEquals(rule, stored)
        assertNull(RetenoNotifications.groupingRule)
    }
}
