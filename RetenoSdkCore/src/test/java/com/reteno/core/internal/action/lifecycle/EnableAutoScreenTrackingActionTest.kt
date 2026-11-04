package com.reteno.core.internal.action.lifecycle

import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.recordingScreenView
import com.reteno.core.internal.lifecycle.ScreenTracker
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EnableAutoScreenTrackingActionTest {

    private class SutFixture(scope: TestScope) {
        val logged = mutableListOf<String>()
        private val platform = FakeAppPlatform()
        val watchStarts: Int
            get() = platform.calls.count { it == "watch fragment screens" }
        val tracker = ScreenTracker(recordingScreenView(logged), scope.workScope())
        val traced = TestActionRunner(scope)
        val sut = EnableAutoScreenTrackingAction(tracker, platform, traced.runner)
    }

    @Test
    fun `should start watching screens once and apply configuration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(ScreenTrackingConfig(enable = true))
        fixture.sut(ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME))
        fixture.tracker.onScreen("HomeFragment", ScreenTrackingTrigger.ON_RESUME)
        runCurrent()

        then()
        assertEquals(1, fixture.watchStarts)
        assertEquals(listOf("HomeFragment"), fixture.logged)
    }

    @Test
    fun `should trace configuration without screen names`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(ScreenTrackingConfig(enable = true, excludeScreens = listOf("SecretFragment")))
        runCurrent()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("EnableAutoScreenTracking  enable=true  trigger=ON_START  excluded=1"))
        assertTrue(lines.none { it.contains("SecretFragment") })
    }
}
