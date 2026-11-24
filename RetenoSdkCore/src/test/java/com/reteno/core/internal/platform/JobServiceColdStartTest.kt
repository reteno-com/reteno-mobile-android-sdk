package com.reteno.core.internal.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.impl.background.systemjob.SystemJobService
import com.reteno.core.fixtures.withoutWorkManager
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = JobServiceColdStartTest.HostApplication::class)
internal class JobServiceColdStartTest {

    class HostApplication : Application()

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val backgroundWork = AndroidBackgroundWork(application, lazyAppWorkManager(application))

        fun systemStartsJobService(): Throwable? = runCatching {
            Robolectric.buildService(SystemJobService::class.java).create().destroy()
        }.exceptionOrNull()
    }

    @Test
    fun `should crash the job service the system starts before anything initialized WorkManager`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val failure = withoutWorkManager { fixture.systemStartsJobService() }

        then()
        assertTrue(failure is IllegalStateException)
        assertEquals(UNINITIALIZED_WORK_MANAGER, failure?.message)
    }

    @Test
    fun `should let the system start the job service once the sdk tried to init WorkManager at startup`() =
        runUnitTest {
            given()
            val fixture = SutFixture()

            whenn()
            val failure = withoutWorkManager {
                fixture.backgroundWork.tryInitWorkManager()
                fixture.systemStartsJobService()
            }

            then()
            assertNull(failure)
        }

    companion object {
        private const val UNINITIALIZED_WORK_MANAGER =
            "WorkManager needs to be initialized via a ContentProvider#onCreate() or an Application#onCreate()."
    }
}
