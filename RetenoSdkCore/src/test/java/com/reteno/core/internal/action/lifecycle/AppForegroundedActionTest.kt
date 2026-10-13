package com.reteno.core.internal.action.lifecycle

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.controller.IamController
import com.reteno.core.internal.action.sync.SyncScheduler
import com.reteno.core.internal.platform.AppPlatform
import com.reteno.core.internal.platform.AppVersion
import com.reteno.core.util.Constants
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AppForegroundedActionTest {

    private class SutFixture(scope: TestScope, installed: AppVersion = AppVersion("2.0", 20L)) {
        val env = LifecycleActionFixture(scope)
        val configRepository = mockk<ConfigRepository>()
        val calls = mutableListOf<String>()
        private val platform = mockk<AppPlatform>().also {
            every { it.sendBroadcast(any()) } answers { calls += "broadcast ${firstArg<String>()}"; 1 }
            every { it.installedVersion() } returns installed
        }
        private val dataSync = mockk<SyncScheduler>().also { every { it.clearOldData() } answers { calls += "clear old data" } }
        private val iamController = mockk<IamController>().also { every { it.preloadHtml() } answers { calls += "preload html" } }
        val sut = AppForegroundedAction(
            platform = platform,
            dataSync = dataSync,
            configRepository = configRepository,
            iamController = iamController,
            state = env.state,
            lifecycleEvents = env.lifecycleEvents,
            clock = { 1_000L },
            runner = env.traced.runner
        )

        init {
            every { configRepository.getAppVersion() } returns "2.0"
            every { configRepository.getAppBuildNumber() } returns 20L
            justRun { configRepository.saveAppVersion(any()) }
            justRun { configRepository.saveAppBuildNumber(any()) }
        }
    }

    @Test
    fun `should broadcast resume clear old data and preload html in order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("broadcast ${Constants.BROADCAST_ACTION_RETENO_APP_RESUME}", "clear old data", "preload html"), fixture.calls)
    }

    @Test
    fun `should log app opened on first foreground only`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationOpened"), fixture.env.loggedKeys)
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ app already in foreground") })
    }

    @Test
    fun `should log app opened from background after app was backgrounded`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.env.state.enterForeground(now = 1L)
        fixture.env.state.enterBackground()

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.env.logged.single().params.orEmpty().any { it.name == "fromBackground" && it.value == "true" })
    }

    @Test
    fun `should log install and save version when no version was saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns ""

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationInstalled", "ApplicationOpened"), fixture.env.loggedKeys)
        verify(exactly = 1) { fixture.configRepository.saveAppVersion("2.0") }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(20L) }
    }

    @Test
    fun `should log update when version name changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns "1.0"

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationUpdated", "ApplicationOpened"), fixture.env.loggedKeys)
        verify(exactly = 1) { fixture.configRepository.saveAppVersion("2.0") }
    }

    @Test
    fun `should log update when only build number changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppBuildNumber() } returns 19L

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationUpdated", "ApplicationOpened"), fixture.env.loggedKeys)
    }

    @Test
    fun `should not touch saved version when unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.configRepository.saveAppVersion(any()) }
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ app version unchanged") })
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("✓ read installed app version") && it.contains("2.0 (20)") })
    }
}
