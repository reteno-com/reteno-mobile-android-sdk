package com.reteno.core.internal.platform

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.R
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
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
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidAppEnvironmentTest {

    private class NoTelephonyContext(base: Context) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any? =
            if (name == Context.TELEPHONY_SERVICE) null else super.getSystemService(name)
    }

    private class SutFixture(context: Context? = null) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val sut = AndroidAppEnvironment(context ?: application)

        fun installedVersion(name: String, code: Int) {
            shadowOf(application.packageManager).installPackage(PackageInfo().apply {
                packageName = application.packageName
                versionName = name
                @Suppress("DEPRECATION")
                versionCode = code
            })
        }

        private val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun notificationsAllowed(allowed: Boolean) {
            shadowOf(notificationManager).setNotificationsEnabled(allowed)
        }

        fun channel(id: String, importance: Int) {
            notificationManager.createNotificationChannel(NotificationChannel(id, "Default", importance))
        }

        fun phoneType(type: Int) {
            val telephony = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            shadowOf(telephony).setPhoneType(type)
        }

        inline fun <T> restoringDefaultLocale(block: () -> T): T {
            val initial = Locale.getDefault()
            try {
                return block()
            } finally {
                Locale.setDefault(initial)
            }
        }

        fun appOverridesLanguage(tag: String) {
            Locale.setDefault(Locale.forLanguageTag(tag))
        }

        fun systemDeliversConfigurationChange() {
            val activityThread = ReflectionHelpers.callStaticMethod<Any>(
                Class.forName("android.app.ActivityThread"),
                "currentActivityThread"
            )
            val newConfiguration = Configuration(application.resources.configuration)
            ReflectionHelpers.callInstanceMethod<Unit>(
                activityThread,
                "handleConfigurationChanged",
                ClassParameter.from(Configuration::class.java, newConfiguration),
                ClassParameter.from(Class.forName("android.content.res.CompatibilityInfo"), null)
            )
        }
    }

    @Test
    fun `should report notifications disabled when the system blocks them for the app`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.notificationsAllowed(false)
        fixture.channel("defaultId", NotificationManager.IMPORTANCE_DEFAULT)

        whenn()
        val enabled = fixture.sut.notificationsEnabled("defaultId")

        then()
        assertFalse(enabled)
    }

    @Test
    fun `should report notifications enabled when the default channel is not created yet`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.notificationsAllowed(true)

        whenn()
        val enabled = fixture.sut.notificationsEnabled("defaultId")

        then()
        assertTrue(enabled)
    }

    @Test
    fun `should report notifications disabled when the default channel is blocked`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.notificationsAllowed(true)
        fixture.channel("defaultId", NotificationManager.IMPORTANCE_NONE)

        whenn()
        val enabled = fixture.sut.notificationsEnabled("defaultId")

        then()
        assertFalse(enabled)
    }

    @Test
    fun `should report notifications enabled when the app and its default channel allow them`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.notificationsAllowed(true)
        fixture.channel("defaultId", NotificationManager.IMPORTANCE_DEFAULT)

        whenn()
        val enabled = fixture.sut.notificationsEnabled("defaultId")

        then()
        assertTrue(enabled)
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
        val device = fixture.sut.createDevice(deviceId, "token", pushSubscribed = false, languageCode = "pl-PL")

        then()
        assertEquals(
            listOf(deviceId.id, "suffix", "user-1", "token", false, "pl-PL", "a@b.c", "+100", null),
            listOf(
                device.deviceId,
                device.deviceIdSuffix,
                device.externalUserId,
                device.pushToken,
                device.pushSubscribed,
                device.languageCode,
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
        val device = fixture.sut.createDevice(
            DeviceId(idBody = "body"),
            "token",
            pushSubscribed = true,
            languageCode = "de-AT"
        )

        then()
        assertEquals("3.1 (31)", device.appVersion)
        assertEquals(DeviceOS.ANDROID, device.osType)
        assertTrue(device.osVersion!!.endsWith("(API 26)"))
        assertTrue(device.deviceModel!!.isNotBlank())
        assertEquals("de-AT", device.languageCode)
        assertEquals(TimeZone.getDefault().toZoneId().id, device.timeZone)
    }

    @Test
    @Config(qualifiers = "uk-rUA")
    fun `should read the same system locale after the app overrides its default locale`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.restoringDefaultLocale {
            whenn()
            fixture.appOverridesLanguage("en")
            val systemLocale = fixture.sut.systemLocale()

            then()
            assertEquals("uk-UA", systemLocale.toLanguageTag())
        }
    }

    @Test
    @Config(qualifiers = "uk-rUA")
    fun `should read the same system locale after a configuration change resets the default locale`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.restoringDefaultLocale {
            fixture.appOverridesLanguage("en")

            whenn()
            fixture.systemDeliversConfigurationChange()
            val systemLocale = fixture.sut.systemLocale()

            then()
            assertEquals("uk-UA", systemLocale.toLanguageTag())
        }
    }

    @Test
    @Config(qualifiers = "uk-rUA")
    fun `should read the locale of the app configuration and not the default locale`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.restoringDefaultLocale {
            fixture.appOverridesLanguage("en")

            whenn()
            val appLocale = fixture.sut.appLocale()

            then()
            assertEquals("uk-UA", appLocale.toLanguageTag())
        }
    }

    @Test
    @Config(qualifiers = "uk-rUA")
    fun `should read the default locale the app set`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.restoringDefaultLocale {
            fixture.appOverridesLanguage("en")

            whenn()
            val defaultLocale = fixture.sut.defaultLocale()

            then()
            assertEquals("en", defaultLocale.toLanguageTag())
        }
    }

    @Test
    fun `should find the likely region of a language`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val region = fixture.sut.likelyRegion("uk")

        then()
        assertEquals("UA", region)
    }

    @Test
    fun `should find no likely region of an unknown language`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val region = fixture.sut.likelyRegion("zz")

        then()
        assertEquals("", region)
    }

    @Test
    fun `should describe device with phone as mobile`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.phoneType(TelephonyManager.PHONE_TYPE_GSM)

        whenn()
        val device = fixture.sut.createDevice(
            DeviceId(idBody = "body"),
            "token",
            pushSubscribed = true,
            languageCode = "de-AT"
        )

        then()
        assertEquals(DeviceCategory.MOBILE, device.category)
    }

    @Test
    fun `should describe device without phone as tablet`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.phoneType(TelephonyManager.PHONE_TYPE_NONE)

        whenn()
        val device = fixture.sut.createDevice(
            DeviceId(idBody = "body"),
            "token",
            pushSubscribed = true,
            languageCode = "de-AT"
        )

        then()
        assertEquals(DeviceCategory.TABLET, device.category)
    }

    @Test
    fun `should describe device without telephony service as mobile`() = runUnitTest {
        given()
        val fixture = SutFixture(NoTelephonyContext(ApplicationProvider.getApplicationContext()))

        whenn()
        val device = fixture.sut.createDevice(
            DeviceId(idBody = "body"),
            "token",
            pushSubscribed = true,
            languageCode = "de-AT"
        )

        then()
        assertEquals(DeviceCategory.MOBILE, device.category)
    }
}
