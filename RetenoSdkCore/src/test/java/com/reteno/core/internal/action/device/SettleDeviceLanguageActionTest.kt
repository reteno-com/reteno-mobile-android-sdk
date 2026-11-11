package com.reteno.core.internal.action.device

import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.FakeDeviceLanguage
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SettleDeviceLanguageActionTest {

    private class SutFixture(private val scope: TestScope) {
        val platform = FakeAppPlatform().apply {
            displayedLocale = Locale.forLanguageTag("en-US")
        }
        val deviceLanguage = FakeDeviceLanguage()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = SettleDeviceLanguageAction(deviceLanguage, platform, dispatchers, traced.runner)

        fun startSettling() = scope.workScope().launch {
            sut()
        }
    }

    @Test
    fun `should not detect the language before the screen stayed resumed for a second`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.startSettling()
        runCurrent()

        whenn()
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS - 1)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should detect the language of the screen once it stayed resumed for a second`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.startSettling()
        runCurrent()

        whenn()
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf("settle en-US"), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should restart the quiet second when another screen resumes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.startSettling()
        runCurrent()
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS - 200)
        runCurrent()

        whenn()
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS - 1)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should wait while no screen is resumed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.startSettling()
        advanceTimeBy(10 * QUIET_MILLIS)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should detect the language of a screen resumed before waiting started`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.activityResumed = true

        whenn()
        fixture.startSettling()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf("settle en-US"), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should wait for the next resumed screen when none is visible after the quiet second`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.displayedLocale = null
        fixture.startSettling()
        runCurrent()
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        whenn()
        fixture.platform.displayedLocale = Locale.forLanguageTag("fr-FR")
        fixture.platform.resumeActivity()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf("settle fr-FR"), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should read the screen language on the main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var readOnMain: Boolean? = null
        fixture.platform.onDisplayedLocaleRead = { readOnMain = fixture.dispatchers.markedMain.isRunningHere }
        fixture.platform.activityResumed = true

        whenn()
        fixture.startSettling()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(true, readOnMain)
    }

    @Test
    fun `should stop watching screens once the language is detected`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.activityResumed = true

        whenn()
        fixture.startSettling()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf("watch resumed activities", "stop watching resumed activities"), fixture.platform.calls)
    }

    @Test
    fun `should stop watching screens when the wait is cancelled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val settling = fixture.startSettling()
        runCurrent()

        whenn()
        settling.cancel()
        runCurrent()

        then()
        assertEquals(listOf("watch resumed activities", "stop watching resumed activities"), fixture.platform.calls)
    }

    @Test
    fun `should detect the language only once per process`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.activityResumed = true
        fixture.startSettling()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("settle en-US"), fixture.deviceLanguage.calls)
    }

    @Test
    fun `should wait again after a cancelled wait`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = fixture.startSettling()
        runCurrent()
        first.cancel()
        runCurrent()
        fixture.platform.activityResumed = true

        whenn()
        fixture.startSettling()
        advanceTimeBy(QUIET_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf("settle en-US"), fixture.deviceLanguage.calls)
    }

    private companion object {
        const val QUIET_MILLIS = 1_000L
    }
}
