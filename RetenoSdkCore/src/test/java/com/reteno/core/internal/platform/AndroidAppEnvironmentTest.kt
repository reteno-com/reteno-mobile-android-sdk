package com.reteno.core.internal.platform

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
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

        fun phoneType(type: Int) {
            val telephony = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            shadowOf(telephony).setPhoneType(type)
        }
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
