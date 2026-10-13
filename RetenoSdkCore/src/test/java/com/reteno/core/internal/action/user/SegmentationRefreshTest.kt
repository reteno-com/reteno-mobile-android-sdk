package com.reteno.core.internal.action.user

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SegmentationRefreshTest {

    private class SutFixture(scope: TestScope) {
        var refreshes = 0
        var failure: Throwable? = null
        val traced = TestActionRunner(scope)
        val sut = SegmentationRefresh(
            refresh = {
                refreshes++
                failure?.let { throw it }
            },
            scope = scope.workScope(),
            runner = traced.runner
        )
    }

    @Test
    fun `should refresh once after requests stop for debounce period`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        runCurrent()

        whenn()
        repeat(3) {
            fixture.sut.request()
            advanceTimeBy(1_000)
        }
        advanceTimeBy(SegmentationRefresh.DEBOUNCE_MILLIS - 1_001)
        runCurrent()
        val beforeQuietPeriodEnds = fixture.refreshes
        advanceTimeBy(1_001)
        runCurrent()

        then()
        assertEquals(0, beforeQuietPeriodEnds)
        assertEquals(1, fixture.refreshes)
    }

    @Test
    fun `should keep refreshing on later requests after a failed refresh`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.failure = IllegalStateException("no network")
        runCurrent()

        whenn()
        fixture.sut.request()
        advanceTimeBy(SegmentationRefresh.DEBOUNCE_MILLIS + 1)
        runCurrent()
        fixture.failure = null
        fixture.sut.request()
        advanceTimeBy(SegmentationRefresh.DEBOUNCE_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(2, fixture.refreshes)
        assertEquals("RefreshSegmentation", fixture.traced.reports.single().action)
    }
}
