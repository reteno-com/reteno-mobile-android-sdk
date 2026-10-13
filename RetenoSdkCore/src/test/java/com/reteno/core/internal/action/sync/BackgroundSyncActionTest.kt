package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BackgroundSyncActionTest {

    private class SutFixture(
        scope: TestScope,
        foreground: Boolean = false,
        initialized: Boolean = true,
        hasData: Boolean = true,
        syncResult: SyncResult = SyncResult.COMPLETED
    ) {
        var syncCalls = 0
        var hasDataCheckedOnIo = false
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        private val sync = object : SyncTrigger {
            override fun request() = Unit

            override suspend fun syncNow(scope: SyncScope): SyncResult {
                syncCalls++
                return syncResult
            }
        }
        private val activityHelper = mockk<RetenoActivityHelper>().also { every { it.canPresentMessages() } returns foreground }
        private val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = false) }
        private val databaseManager = mockk<RetenoDatabaseManager>().also {
            every { it.hasDataForSync() } answers {
                hasDataCheckedOnIo = dispatchers.markedIo.isRunningHere
                hasData
            }
        }
        private val platform = FakeAppPlatform()
        val cancelled: Int
            get() = platform.calls.count { it == "cancel background sync" }
        val sut = BackgroundSyncAction(
            activityHelper = activityHelper,
            initGate = initGate,
            databaseManager = databaseManager,
            sync = sync,
            platform = platform,
            dispatchers = dispatchers,
            runner = traced.runner
        )
    }

    @Test
    fun `should skip while app is in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this, foreground = true)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND, outcome)
        assertEquals(0, fixture.syncCalls)
    }

    @Test
    fun `should ask to retry when sdk is not initialized yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.NOT_INITIALIZED, outcome)
        assertEquals(0, fixture.syncCalls)
    }

    @Test
    fun `should cancel periodic work when there is nothing to sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this, hasData = false)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.NO_DATA, outcome)
        assertEquals(1, fixture.cancelled)
        assertEquals(0, fixture.syncCalls)
        assertTrue(fixture.hasDataCheckedOnIo)
    }

    @Test
    fun `should wait for full sync and report synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.SYNCED, outcome)
        assertEquals(1, fixture.syncCalls)
        assertEquals(0, fixture.cancelled)
    }

    @Test
    fun `should report aborted sync so that work is retried`() = runUnitTest {
        given()
        val fixture = SutFixture(this, syncResult = SyncResult.ABORTED)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.ABORTED, outcome)
        assertTrue(fixture.traced.sink.lines.last().contains("BackgroundSync ✗ ABORTED"))
    }

    @Test
    fun `should report incomplete sync as aborted so that work is retried`() = runUnitTest {
        given()
        val fixture = SutFixture(this, syncResult = SyncResult.INCOMPLETE)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(BackgroundSyncOutcome.ABORTED, outcome)
    }
}
