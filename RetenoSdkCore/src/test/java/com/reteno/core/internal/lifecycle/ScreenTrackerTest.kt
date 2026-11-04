package com.reteno.core.internal.lifecycle

import com.reteno.core.fixtures.recordingScreenView
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScreenTrackerTest {

    private class SutFixture(scope: TestScope) {
        val logged = mutableListOf<String>()
        val sut = ScreenTracker(recordingScreenView(logged), scope.workScope())
    }

    @Test
    fun `should not track anything until enabled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.onScreen("HomeFragment", ScreenTrackingTrigger.ON_START)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.logged)
    }

    @Test
    fun `should track screens shown with configured trigger only`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.configure(ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME))

        whenn()
        fixture.sut.onScreen("HomeFragment", ScreenTrackingTrigger.ON_START)
        fixture.sut.onScreen("HomeFragment", ScreenTrackingTrigger.ON_RESUME)
        runCurrent()

        then()
        assertEquals(listOf("HomeFragment"), fixture.logged)
    }

    @Test
    fun `should skip excluded screens`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.configure(ScreenTrackingConfig(enable = true, excludeScreens = listOf("SplashFragment")))

        whenn()
        fixture.sut.onScreen("SplashFragment", ScreenTrackingTrigger.ON_START)
        fixture.sut.onScreen("HomeFragment", ScreenTrackingTrigger.ON_START)
        runCurrent()

        then()
        assertEquals(listOf("HomeFragment"), fixture.logged)
    }

    @Test
    fun `should stop tracking when disabled again`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.configure(ScreenTrackingConfig(enable = true))

        whenn()
        fixture.sut.configure(ScreenTrackingConfig(enable = false))
        fixture.sut.onScreen("HomeFragment", ScreenTrackingTrigger.ON_START)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.logged)
    }
}
