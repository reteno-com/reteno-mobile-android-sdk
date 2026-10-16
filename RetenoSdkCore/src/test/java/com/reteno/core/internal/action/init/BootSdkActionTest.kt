package com.reteno.core.internal.action.init

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.DeviceIdentity
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.fixtures.STUB_ACCESS_KEY
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BootSdkActionTest {

    private class SutFixture(scope: TestScope, cached: RetenoConfig?) {
        val calls = mutableListOf<String>()
        val applied = mutableListOf<RetenoConfig>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var warmUpFailure: Throwable? = null
        private val platform = mockk<AppPlatform>().also {
            every { it.initWorkManager() } answers {
                warmUpFailure?.let { error -> throw error }
                calls += "init WorkManager" + onIo()
            }
        }
        private val prefs = mockk<SharedPrefsManager>().also {
            every { it.getEmail() } answers { calls += "load local settings" + onIo(); null }
            every { it.getCachedConfiguration() } answers { calls += "read cached config" + onIo(); cached }
        }
        private val restConfig = mockk<DeviceIdentity>().also {
            coEvery { it.initDeviceId(DeviceIdMode.CACHE) } answers { calls += "restore device id" + onIo() }
        }
        private val applyConfig = mockk<ApplyConfig>().also {
            coEvery { it(any(), byUser = false) } answers {
                applied += firstArg<RetenoConfig>()
                calls += "apply cached config"
                Result.success(Unit)
            }
        }
        val sut = BootSdkAction(platform, prefs, restConfig, applyConfig, dispatchers, traced.runner)

        private fun onIo() = if (dispatchers.markedIo.isRunningHere) " on io" else ""
    }

    @Test
    fun `should restore device id and apply cached configuration`() = runUnitTest {
        given()
        val config = stubConfig()
        val fixture = SutFixture(this, config)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf(
                "init WorkManager on io",
                "load local settings on io",
                "read cached config on io",
                "restore device id on io",
                "apply cached config"
            ),
            fixture.calls
        )
        assertSame(config, fixture.applied.single())
    }

    @Test
    fun `should wait for user configuration when nothing is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this, cached = null)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf("init WorkManager on io", "load local settings on io", "read cached config on io"),
            fixture.calls
        )
        val lines = fixture.traced.sink.lines
        assertTrue(
            lines.any { it.contains("✓ read cached configuration from local settings") && it.contains("none") }
        )
        assertTrue(lines.any { it.contains("→ no cached configuration, waiting for user configuration") })
    }

    @Test
    fun `should continue boot when warm up fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this, stubConfig())
        fixture.warmUpFailure = IllegalStateException("WorkManager is not initialized")

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        assertEquals("apply cached config", fixture.calls.last())
        assertEquals("WarmUpStorage", fixture.traced.reports.single().action)
    }

    @Test
    fun `should not trace cached access key`() = runUnitTest {
        given()
        val fixture = SutFixture(this, stubConfig())

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("read cached configuration from local settings") && it.contains("found") })
        assertTrue(lines.none { it.contains(STUB_ACCESS_KEY) })
    }
}
