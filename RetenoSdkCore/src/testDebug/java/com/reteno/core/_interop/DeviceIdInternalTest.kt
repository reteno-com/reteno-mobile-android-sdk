package com.reteno.core._interop

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class DeviceIdInternalTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val raw = application.getSharedPreferences("reteno_shared_prefs", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
    }

    @Test
    fun `should read device id external user id and push token stored by the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit()
            .putString("config_last_id", DEVICE_ID)
            .putString("external_user_id", EXTERNAL_ID)
            .putString("fcm_token", FCM_TOKEN)
            .commit()

        whenn()
        val values = listOf(
            DeviceIdInternal.deviceId(fixture.application),
            DeviceIdInternal.externalUserId(fixture.application),
            DeviceIdInternal.fcmToken(fixture.application)
        )

        then()
        assertEquals(listOf(DEVICE_ID, EXTERNAL_ID, FCM_TOKEN), values)
    }

    @Test
    fun `should answer empty values before the sdk stored any`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val values = listOf(
            DeviceIdInternal.deviceId(fixture.application),
            DeviceIdInternal.externalUserId(fixture.application),
            DeviceIdInternal.fcmToken(fixture.application)
        )

        then()
        assertEquals(listOf("", null, ""), values)
    }

    companion object {
        private const val DEVICE_ID = "device-body:suffix"
        private const val EXTERNAL_ID = "user-1"
        private const val FCM_TOKEN = "token-1"
    }
}
