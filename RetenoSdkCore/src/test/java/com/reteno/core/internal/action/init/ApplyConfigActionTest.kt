package com.reteno.core.internal.action.init

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.cache.InteractionCacheStore
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.controller.IamController
import com.reteno.core.fixtures.STUB_ACCESS_KEY
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.device.RegisterDevice
import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.lifecycle.AppLifecycleTracking
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.view.iam.IamView
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ApplyConfigActionTest {

    private class SutFixture(scope: TestScope, userConfigInUse: Boolean = false) {
        val calls = mutableListOf<String>()
        val used = mutableListOf<RetenoConfig>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var cacheFailure: Throwable? = null
        private val configuration = mockk<SdkConfiguration>().also {
            every { it.useUserConfig(any()) } answers { used += firstArg<RetenoConfig>(); calls += "use user config" }
            every { it.useCachedConfig(any()) } answers {
                calls += "use cached config"
                if (!userConfigInUse) used += firstArg<RetenoConfig>()
                !userConfigInUse
            }
        }
        private val restConfig = mockk<RestConfig>().also {
            coEvery { it.initDeviceId(any()) } answers { calls += "init device id ${firstArg<DeviceIdMode>()}" + onIo() }
        }
        private val configRepository = mockk<ConfigRepository>().also {
            coEvery { it.awaitForDeviceId() } answers { calls += "await device id" + onIo(); DeviceId("device-1") }
        }
        private val prefs = mockk<SharedPrefsManager>().also {
            every { it.cacheConfiguration(any(), any()) } answers {
                cacheFailure?.let { error -> throw error }
                calls += "cache config for ${firstArg<String>()}" + onIo()
            }
        }
        private val sessionHandler = mockk<RetenoSessionHandler>().also {
            every { it.setSessionResetDuration(any()) } answers { calls += "session duration ${firstArg<Long>()}" }
        }
        private val registerDevice = mockk<RegisterDevice>().also {
            coEvery { it() } answers { calls += "register device"; Result.success(SyncResult.COMPLETED) }
        }
        private val initGate = InitGate()
        private val iamController = mockk<IamController>().also {
            every { it.pauseInAppMessages(any()) } answers {
                calls += "gate ${initGate.state}"
                calls += "pause in-apps ${firstArg<Boolean>()}" + onMain()
            }
        }
        private val iamView = mockk<IamView>().also {
            every { it.pauseIncomingPushInApps(any()) } answers { calls += "pause push in-apps ${firstArg<Boolean>()}" + onMain() }
        }
        private val lifecycleTracking = mockk<AppLifecycleTracking>().also {
            every { it.start() } answers { calls += "start lifecycle tracking" + onMain() }
        }
        private val interactionCache = mockk<InteractionCacheStore>().also {
            every { it.processCachedInteractions() } answers { calls += "process cached interactions" + onIo() }
        }
        val sut = ApplyConfigAction(
            configuration, restConfig, configRepository, prefs, sessionHandler, registerDevice, initGate,
            iamController, iamView, lifecycleTracking, interactionCache, dispatchers, traced.runner
        )

        private fun onIo() = if (dispatchers.markedIo.isRunningHere) " on io" else ""

        private fun onMain() = if (dispatchers.markedMain.isRunningHere) " on main" else ""
    }

    @Test
    fun `should apply user configuration and open gate for user`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig()

        whenn()
        fixture.sut(config, byUser = true)

        then()
        assertEquals(
            listOf(
                "use user config",
                "init device id ${DeviceIdMode.ANDROID_ID} on io",
                "await device id on io",
                "cache config for device-1 on io",
                "session duration 60000",
                "register device",
                "gate BY_USER",
                "pause in-apps false on main",
                "pause push in-apps true on main",
                "start lifecycle tracking on main",
                "process cached interactions on io"
            ),
            fixture.calls
        )
        assertEquals(listOf(config), fixture.used)
    }

    @Test
    fun `should use client id mode when config provides custom device id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubConfig(customDeviceId = true), byUser = true)

        then()
        assertTrue(fixture.calls.contains("init device id ${DeviceIdMode.CLIENT_UUID} on io"))
    }

    @Test
    fun `should apply cached configuration without device registration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubConfig(), byUser = false)

        then()
        assertEquals(
            listOf(
                "use cached config",
                "await device id on io",
                "cache config for device-1 on io",
                "session duration 60000",
                "gate FROM_CACHE",
                "pause in-apps false on main",
                "pause push in-apps true on main",
                "process cached interactions on io"
            ),
            fixture.calls
        )
    }

    @Test
    fun `should keep user configuration when cached one is applied later`() = runUnitTest {
        given()
        val fixture = SutFixture(this, userConfigInUse = true)

        whenn()
        fixture.sut(stubConfig(), byUser = false)

        then()
        assertTrue(fixture.used.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ user configuration already in use") })
    }

    @Test
    fun `should still open gate when applying options fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.cacheFailure = IOException("disk full")

        whenn()
        val result = fixture.sut(stubConfig(), byUser = true)

        then()
        assertTrue(result.isSuccess)
        assertTrue(fixture.calls.containsAll(listOf("register device", "gate BY_USER", "process cached interactions on io")))
        assertEquals("ApplyConfigOptions", fixture.traced.reports.single().action)
    }

    @Test
    fun `should trace configuration without access key`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubConfig(), byUser = true)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("ApplyConfig  source=user  debug=false  customDeviceId=false  sessionDurationMillis=60000"))
        assertTrue(lines.none { it.contains(STUB_ACCESS_KEY) })
    }
}
