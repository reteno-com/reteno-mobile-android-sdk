package com.reteno.core.internal.action.sync

import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DataSyncTest {

    private class SutFixture(scope: TestScope, debugView: Boolean = false, jitter: Long = 2_000L) {
        var syncs = 0
        var cleanups = 0
        var enqueuedOnIo = mutableListOf<Boolean>()
        var enqueueError: Throwable? = null
        private val clearOldData = mockk<ClearOldData>().also {
            coEvery { it() } answers {
                cleanups++
                Result.success(Unit)
            }
        }
        private val configuration = mockk<SdkConfiguration>().also { every { it.isDebug } returns debugView }
        private val platform = FakeAppPlatform().also {
            it.onScheduleBackgroundSync = {
                enqueuedOnIo += dispatchers.markedIo.isRunningHere
                enqueueError?.let { error -> throw error }
            }
        }
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        private val work = scope.workScope()
        private val readyGate = InitGate().also { it.markInitialized(byUser = true) }
        private val step = SyncStep {
            syncs++
            Result.success(StepOutcome.CONTINUE)
        }
        val sut = DataSync(
            coordinator = SyncCoordinator(SyncDataAction(listOf(step), emptyList(), traced.runner), work, readyGate),
            clearOldDataAction = clearOldData,
            backgroundWork = platform,
            configuration = configuration,
            jitter = { jitter },
            scope = work,
            dispatchers = dispatchers,
            runner = traced.runner
        )
    }

    @Test
    fun `should first sync after regular delay plus jitter`() = runUnitTest {
        given()
        val fixture = SutFixture(this, jitter = 2_000L)

        whenn()
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY + 1_999L)
        runCurrent()
        val beforeDelay = fixture.syncs
        advanceTimeBy(1L)
        runCurrent()

        then()
        assertEquals(0, beforeDelay)
        assertEquals(1, fixture.syncs)
    }

    @Test
    fun `should keep syncing every regular delay`() = runUnitTest {
        given()
        val fixture = SutFixture(this, jitter = 0L)

        whenn()
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY * 3)
        runCurrent()

        then()
        assertEquals(3, fixture.syncs)
    }

    @Test
    fun `should sync more often in debug view`() = runUnitTest {
        given()
        val fixture = SutFixture(this, debugView = true, jitter = 0L)

        whenn()
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.DEBUG_VIEW_DELAY * 3)
        runCurrent()

        then()
        assertEquals(3, fixture.syncs)
    }

    @Test
    fun `should restart instead of running two schedulers when started twice`() = runUnitTest {
        given()
        val fixture = SutFixture(this, jitter = 0L)

        whenn()
        fixture.sut.startScheduler()
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY * 2)
        runCurrent()

        then()
        assertEquals(2, fixture.syncs)
    }

    @Test
    fun `should stop syncing after scheduler stopped`() = runUnitTest {
        given()
        val fixture = SutFixture(this, jitter = 0L)
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY)
        runCurrent()

        whenn()
        fixture.sut.stopScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY * 5)
        runCurrent()

        then()
        assertEquals(1, fixture.syncs)
    }

    @Test
    fun `should tolerate stopping scheduler that was never started`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.stopScheduler()
        runCurrent()

        then()
        assertEquals(0, fixture.syncs)
    }

    @Test
    fun `should enqueue background sync on io dispatcher when scheduler starts`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.startScheduler()
        runCurrent()

        then()
        assertEquals(listOf(true), fixture.enqueuedOnIo)
    }

    @Test
    fun `should report failed background sync enqueue and keep scheduler running`() = runUnitTest {
        given()
        val fixture = SutFixture(this, jitter = 0L)
        fixture.enqueueError = IllegalStateException("WorkManager is not initialized")

        whenn()
        fixture.sut.startScheduler()
        advanceTimeBy(DataSync.REGULAR_DELAY)
        runCurrent()

        then()
        assertEquals("EnqueueBackgroundSync", fixture.traced.reports.single().action)
        assertEquals(1, fixture.syncs)
    }

    @Test
    fun `should clear old data after startup delay`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.clearOldData()
        advanceTimeBy(DataSync.CLEAR_OLD_DATA_DELAY - 1)
        runCurrent()
        val beforeDelay = fixture.cleanups
        advanceTimeBy(1)
        runCurrent()

        then()
        assertEquals(0, beforeDelay)
        assertEquals(1, fixture.cleanups)
    }

    @Test
    fun `should delegate sync requests to coordinator`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.request()
        runCurrent()
        val result = fixture.sut.syncNow(SyncScope.CONTACT)

        then()
        assertEquals(2, fixture.syncs)
        assertEquals(SyncResult.COMPLETED, result)
        assertTrue(fixture.traced.reports.isEmpty())
    }
}
