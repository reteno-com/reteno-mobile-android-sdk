package com.reteno.core.internal.action.lifecycle

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.internal.action.iam.InAppHtmlPreloading
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

    private class SutFixture(
        scope: TestScope,
        options: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL
    ) {
        val env = LifecycleActionFixture(scope, options)
        val configRepository = mockk<ConfigRepository>()
        val calls = mutableListOf<String>()
        private val platform = mockk<AppPlatform>().also {
            every { it.sendBroadcast(any()) } answers { calls += "broadcast ${firstArg<String>()}"; 1 }
            every { it.installedVersion() } returns AppVersion(INSTALLED_VERSION, INSTALLED_BUILD)
        }
        private val dataSync = mockk<SyncScheduler>().also {
            every { it.clearOldData() } answers { calls += "clear old data" }
        }
        private val htmlPreload = mockk<InAppHtmlPreloading>().also {
            every { it.request() } answers { calls += "preload html" }
        }
        val sut = AppForegroundedAction(
            platform = platform,
            dataSync = dataSync,
            configRepository = configRepository,
            htmlPreload = htmlPreload,
            state = env.state,
            lifecycleEvents = env.lifecycleEvents,
            clock = { 1_000L },
            runner = env.traced.runner
        )

        init {
            every { configRepository.getAppVersion() } returns INSTALLED_VERSION
            every { configRepository.getAppBuildNumber() } returns INSTALLED_BUILD
            justRun { configRepository.saveAppVersion(any()) }
            justRun { configRepository.saveAppBuildNumber(any()) }
        }

        fun paramsOf(eventTypeKey: String): List<Parameter>? =
            env.logged.single { it.eventTypeKey == eventTypeKey }.params
    }

    @Test
    fun `should broadcast resume clear old data and preload html in order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(
            listOf("broadcast ${Constants.BROADCAST_ACTION_RETENO_APP_RESUME}", "clear old data", "preload html"),
            fixture.calls
        )
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
    }

    @Test
    fun `should note in trace that app is already in foreground on second foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut()

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ app already in foreground") })
    }

    @Test
    fun `should log first app opened as not from background`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(Parameter("fromBackground", "false")), fixture.paramsOf("ApplicationOpened"))
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
        assertEquals(listOf(Parameter("fromBackground", "true")), fixture.paramsOf("ApplicationOpened"))
    }

    @Test
    fun `should log install before app opened when no version was saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns ""

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationInstalled", "ApplicationOpened"), fixture.env.loggedKeys)
    }

    @Test
    fun `should log install with installed version and build`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns ""

        whenn()
        fixture.sut()

        then()
        val expected = listOf(
            Parameter("version", INSTALLED_VERSION),
            Parameter("build", INSTALLED_BUILD.toString())
        )
        assertEquals(expected, fixture.paramsOf("ApplicationInstalled"))
    }

    @Test
    fun `should save installed version and build when no version was saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns ""

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.configRepository.saveAppVersion(INSTALLED_VERSION) }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(INSTALLED_BUILD) }
    }

    @Test
    fun `should save installed version and build when install event is disabled by tracking options`() = runUnitTest {
        given()
        val fixture = SutFixture(this, LifecycleTrackingOptions.ALL.copy(appLifecycleEnabled = false))
        every { fixture.configRepository.getAppVersion() } returns ""

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.configRepository.saveAppVersion(INSTALLED_VERSION) }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(INSTALLED_BUILD) }
    }

    @Test
    fun `should log update before app opened when version name changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns PREVIOUS_VERSION

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("ApplicationUpdated", "ApplicationOpened"), fixture.env.loggedKeys)
    }

    @Test
    fun `should log update with installed and previous version and build when version name changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns PREVIOUS_VERSION
        every { fixture.configRepository.getAppBuildNumber() } returns PREVIOUS_BUILD

        whenn()
        fixture.sut()

        then()
        val expected = listOf(
            Parameter("version", INSTALLED_VERSION),
            Parameter("build", INSTALLED_BUILD.toString()),
            Parameter("previousVersion", PREVIOUS_VERSION),
            Parameter("previousBuild", PREVIOUS_BUILD.toString())
        )
        assertEquals(expected, fixture.paramsOf("ApplicationUpdated"))
    }

    @Test
    fun `should save installed version and build when version name changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppVersion() } returns PREVIOUS_VERSION
        every { fixture.configRepository.getAppBuildNumber() } returns PREVIOUS_BUILD

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.configRepository.saveAppVersion(INSTALLED_VERSION) }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(INSTALLED_BUILD) }
    }

    @Test
    fun `should save installed version and build when update event is disabled by tracking options`() = runUnitTest {
        given()
        val fixture = SutFixture(this, LifecycleTrackingOptions.ALL.copy(appLifecycleEnabled = false))
        every { fixture.configRepository.getAppVersion() } returns PREVIOUS_VERSION
        every { fixture.configRepository.getAppBuildNumber() } returns PREVIOUS_BUILD

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.configRepository.saveAppVersion(INSTALLED_VERSION) }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(INSTALLED_BUILD) }
    }

    @Test
    fun `should log update with same version and previous build when only build number changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppBuildNumber() } returns PREVIOUS_BUILD

        whenn()
        fixture.sut()

        then()
        val expected = listOf(
            Parameter("version", INSTALLED_VERSION),
            Parameter("build", INSTALLED_BUILD.toString()),
            Parameter("previousVersion", INSTALLED_VERSION),
            Parameter("previousBuild", PREVIOUS_BUILD.toString())
        )
        assertEquals(expected, fixture.paramsOf("ApplicationUpdated"))
    }

    @Test
    fun `should save installed version and build when only build number changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.configRepository.getAppBuildNumber() } returns PREVIOUS_BUILD

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.configRepository.saveAppVersion(INSTALLED_VERSION) }
        verify(exactly = 1) { fixture.configRepository.saveAppBuildNumber(INSTALLED_BUILD) }
    }

    @Test
    fun `should not save app version when unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 0) { fixture.configRepository.saveAppVersion(any()) }
        verify(exactly = 0) { fixture.configRepository.saveAppBuildNumber(any()) }
    }

    @Test
    fun `should note in trace that app version is unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("→ app version unchanged") })
    }

    @Test
    fun `should trace installed app version`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(
            fixture.env.traced.sink.lines.any {
                it.contains("✓ read installed app version") && it.contains("$INSTALLED_VERSION ($INSTALLED_BUILD)")
            }
        )
    }

    companion object {
        private const val INSTALLED_VERSION = "2.0"
        private const val INSTALLED_BUILD = 20L
        private const val PREVIOUS_VERSION = "1.0"
        private const val PREVIOUS_BUILD = 19L
    }
}
