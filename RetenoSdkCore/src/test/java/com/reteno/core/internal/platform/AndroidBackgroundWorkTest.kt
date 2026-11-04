package com.reteno.core.internal.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidBackgroundWorkTest {

    private class CustomWorkManagerApp(base: Context) : ContextWrapper(base), Configuration.Provider {
        override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder().build()
    }

    private class SutFixture(context: Context? = null) {
        val workManager = mockk<WorkManager>(relaxed = true)
        val workManagerAccess = lazy { workManager }
        val sut = AndroidBackgroundWork(context ?: ApplicationProvider.getApplicationContext(), workManagerAccess)
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
}
