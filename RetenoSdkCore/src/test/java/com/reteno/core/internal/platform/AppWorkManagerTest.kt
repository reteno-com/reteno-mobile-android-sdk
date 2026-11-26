package com.reteno.core.internal.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import com.reteno.core.fixtures.withoutWorkManager
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AppWorkManagerTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
    }

    @Test
    fun `should use the work manager the app already initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (initialized, used) = withoutWorkManager {
            WorkManagerTestInitHelper.initializeTestWorkManager(fixture.application)
            val initialized = WorkManagerImpl.getInstance(fixture.application)
            initialized to lazyAppWorkManager(fixture.application).value
        }

        then()
        assertSame(initialized, used)
    }

    @Test
    fun `should initialize work manager when the app did not`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (used, initialized) = withoutWorkManager {
            val used = appWorkManager(fixture.application)
            used to WorkManagerImpl.getInstance(fixture.application)
        }

        then()
        assertSame(initialized, used)
    }
}
