package com.reteno.core.internal.platform

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.telephony.TelephonyManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.R
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.view.iam.AndroidInAppScreen
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidAppPlatformTest {

    private class CustomWorkManagerApp(base: Context) : ContextWrapper(base), Configuration.Provider {
        override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder().build()
    }

    private class NoTelephonyContext(base: Context) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any? =
            if (name == Context.TELEPHONY_SERVICE) null else super.getSystemService(name)
    }

    private class SutFixture(context: Context? = null) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val owner = ManualLifecycleOwner()
        val activityHelper = mockk<RetenoActivityHelper>()
        val workManager = mockk<WorkManager>(relaxed = true)
        val workManagerAccess = lazy { workManager }
        val sut = AndroidAppPlatform(context ?: application, workManagerAccess, owner, activityHelper)

        init {
            justRun { activityHelper.registerActivityLifecycleCallbacks(any(), any()) }
        }

        fun installedVersion(name: String, code: Int) {
            shadowOf(application.packageManager).installPackage(PackageInfo().apply {
                packageName = application.packageName
                versionName = name
                @Suppress("DEPRECATION")
                versionCode = code
            })
        }

        fun phoneType(type: Int) {
            val telephony = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            shadowOf(telephony).setPhoneType(type)
        }
    }

    @Test
    fun `should broadcast to registered receivers`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = fixture.application.packageName
                name = "com.example.Receiver"
            }
        }
        shadowOf(fixture.application.packageManager).addResolveInfoForIntent(Intent("com.reteno.platform-action"), info)

        whenn()
        val sent = fixture.sut.sendBroadcast("com.reteno.platform-action")

        then()
        assertEquals(1, sent)
    }

    @Test
    fun `should read installed app version`() = runUnitTest {
        given()
        val fixture = SutFixture()
        @Suppress("DEPRECATION")
        val info = PackageInfo().apply {
            packageName = fixture.application.packageName
            versionName = "4.2"
            versionCode = 42
        }
        shadowOf(fixture.application.packageManager).installPackage(info)

        whenn()
        val version = fixture.sut.installedVersion()

        then()
        assertEquals(AppVersion("4.2", 42L), version)
    }

    @Test
    fun `should read package on top of activity task`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val taskInfo = ActivityManager.RecentTaskInfo().apply {
            topActivity = ComponentName("com.example.top", "com.example.top.Top")
        }
        val task = mockk<ActivityManager.AppTask>().also { every { it.taskInfo } returns taskInfo }
        val manager = mockk<ActivityManager>().also { every { it.appTasks } returns listOf(task) }
        val activity = mockk<Activity>().also {
            every { it.getSystemService(Context.ACTIVITY_SERVICE) } returns manager
        }

        whenn()
        val topPackage = fixture.sut.packageOnTop(activity)

        then()
        assertEquals("com.example.top", topPackage)
    }

    @Test
    fun `should return no top package when activity manager access is denied`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val activity = mockk<Activity>().also {
            every { it.getSystemService(any()) } throws SecurityException("denied")
        }

        whenn()
        val topPackage = fixture.sut.packageOnTop(activity)

        then()
        assertNull(topPackage)
    }

    @Test
    fun `should register fragment screen and app lifecycle watchers`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val calls = mutableListOf<String>()

        whenn()
        fixture.sut.watchFragmentScreens { name, _ -> calls += name }
        fixture.sut.watchAppLifecycle(
            onForeground = { calls += "foreground" },
            onBackground = {},
            onActivityPaused = {}
        )
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        then()
        assertEquals(listOf("foreground"), calls)
        verify(exactly = 1) {
            fixture.activityHelper.registerActivityLifecycleCallbacks("FragmentScreenWatcher", any())
        }
        verify(exactly = 1) {
            fixture.activityHelper.registerActivityLifecycleCallbacks("AppLifecycleWatcher", any())
        }
    }

    @Test
    fun `should init WorkManager when app does not configure it`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.initWorkManager()

        then()
        assertTrue(fixture.workManagerAccess.isInitialized())
    }

    @Test
    fun `should leave WorkManager to app that configures it`() = runUnitTest {
        given()
        val fixture = SutFixture(CustomWorkManagerApp(ApplicationProvider.getApplicationContext()))

        whenn()
        fixture.sut.initWorkManager()

        then()
        assertFalse(fixture.workManagerAccess.isInitialized())
    }

    @Test
    fun `should schedule and cancel periodic background sync`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.scheduleBackgroundSync()
        fixture.sut.cancelBackgroundSync()

        then()
        verify(exactly = 1) {
            fixture.workManager.enqueueUniquePeriodicWork(PushDataWorker.PUSH_DATA_WORK_NAME, any(), any())
        }
        verify(exactly = 1) { fixture.workManager.cancelUniqueWork(PushDataWorker.PUSH_DATA_WORK_NAME) }
    }

    @Test
    fun `should read widget bundled with the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val widgetFile = fixture.application.resources.openRawResource(R.raw.widget)
        val expected = widgetFile.bufferedReader().use { it.readText() }

        whenn()
        val widget = fixture.sut.bundledWidget()

        then()
        assertEquals(expected, widget)
    }

    @Test
    fun `should show in-apps on android screen`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val screen = fixture.sut.inAppScreen()

        then()
        assertTrue(screen is AndroidInAppScreen)
    }

    @Test
    fun `should describe app and device for log events`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val packages = shadowOf(fixture.application.packageManager)
        packages.installPackage(PackageInfo().apply {
            packageName = fixture.application.packageName
            versionName = "2.5"
            @Suppress("DEPRECATION")
            versionCode = 25
        })

        whenn()
        val origin = fixture.sut.logEventOrigin()

        then()
        assertEquals("2.5 (25)", origin.appVersion)
        assertTrue(origin.osVersion.endsWith("(API 26)"))
        assertTrue(origin.deviceModel.isNotBlank())
    }

    @Test
    fun `should describe current device for device id and push state`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val deviceId = DeviceId(
            idBody = "body",
            idSuffix = "suffix",
            externalId = "user-1",
            email = "a@b.c",
            phone = "+100"
        )

        whenn()
        val device = fixture.sut.createDevice(deviceId, "token", pushSubscribed = false)

        then()
        assertEquals(
            listOf(deviceId.id, "suffix", "user-1", "token", false, "a@b.c", "+100", null),
            listOf(
                device.deviceId,
                device.deviceIdSuffix,
                device.externalUserId,
                device.pushToken,
                device.pushSubscribed,
                device.email,
                device.phone,
                device.advertisingId
            )
        )
    }

    @Test
    fun `should describe app os locale and time zone of current device`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.installedVersion("3.1", 31)

        whenn()
        val device = fixture.sut.createDevice(DeviceId(idBody = "body"), "token", pushSubscribed = true)

        then()
        assertEquals("3.1 (31)", device.appVersion)
        assertEquals(DeviceOS.ANDROID, device.osType)
        assertTrue(device.osVersion!!.endsWith("(API 26)"))
        assertTrue(device.deviceModel!!.isNotBlank())
        assertEquals(Locale.getDefault().toLanguageTag(), device.languageCode)
        assertEquals(TimeZone.getDefault().toZoneId().id, device.timeZone)
    }

    @Test
    fun `should describe device with phone as mobile`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.phoneType(TelephonyManager.PHONE_TYPE_GSM)

        whenn()
        val device = fixture.sut.createDevice(DeviceId(idBody = "body"), "token", pushSubscribed = true)

        then()
        assertEquals(DeviceCategory.MOBILE, device.category)
    }

    @Test
    fun `should describe device without phone as tablet`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.phoneType(TelephonyManager.PHONE_TYPE_NONE)

        whenn()
        val device = fixture.sut.createDevice(DeviceId(idBody = "body"), "token", pushSubscribed = true)

        then()
        assertEquals(DeviceCategory.TABLET, device.category)
    }

    @Test
    fun `should describe device without telephony service as mobile`() = runUnitTest {
        given()
        val fixture = SutFixture(NoTelephonyContext(ApplicationProvider.getApplicationContext()))

        whenn()
        val device = fixture.sut.createDevice(DeviceId(idBody = "body"), "token", pushSubscribed = true)

        then()
        assertEquals(DeviceCategory.MOBILE, device.category)
    }
}
