package com.reteno.core.internal.platform

import android.app.Application
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdClient
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class DeviceIdSourcesTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val sut = AndroidDeviceIdSources(application)

        fun <T> TestScope.withAppSet(info: Task<AppSetIdInfo>, block: suspend () -> T): Result<T> {
            mockkStatic(AppSet::class)
            try {
                val client = mockk<AppSetIdClient>()
                every { client.appSetIdInfo } returns info
                every { AppSet.getClient(any()) } returns client
                val reading = async { runCatching { block() } }
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                runCurrent()
                return reading.getCompleted()
            } finally {
                unmockkStatic(AppSet::class)
            }
        }
    }

    @Test
    fun `should read android id of the device`() = runUnitTest {
        given()
        val fixture = SutFixture()
        Settings.Secure.putString(fixture.application.contentResolver, Settings.Secure.ANDROID_ID, "android-123")

        whenn()
        val androidId = fixture.sut.androidId()

        then()
        assertEquals("android-123", androidId)
    }

    @Test
    fun `should read app set id from play services`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val info = Tasks.forResult(AppSetIdInfo("app-set-1", AppSetIdInfo.SCOPE_APP))
        val appSetId = with(fixture) { withAppSet(info) { sut.appSetId() } }

        then()
        assertEquals("app-set-1", appSetId.getOrThrow())
    }

    @Test
    fun `should fail when play services cannot give app set id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val info = Tasks.forException<AppSetIdInfo>(IllegalStateException("no play services"))
        val appSetId = with(fixture) { withAppSet(info) { sut.appSetId() } }

        then()
        assertTrue(appSetId.exceptionOrNull() is IllegalStateException)
    }
}
