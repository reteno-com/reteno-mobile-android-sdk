package com.reteno.core.data.workmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.reteno.core.Reteno
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.internal.action.sync.BackgroundSyncAction
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.util.Util
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class PushDataWorkerTest {

    private class SutFixture {
        private val context: Context = ApplicationProvider.getApplicationContext()
        val sync = mockk<BackgroundSyncAction>()
        val sdk = mockk<RetenoInternalImpl>().also { every { it.backgroundSync } returns sync }
        val sut = TestListenableWorkerBuilder<PushDataWorker>(context).build()

        suspend fun doWork(instance: RetenoInternalImpl? = sdk): ListenableWorker.Result {
            val previous = Reteno.instanceInternal
            Reteno.instanceInternal = instance
            try {
                return sut.doWork()
            } finally {
                Reteno.instanceInternal = previous
            }
        }
    }

    private fun resultFor(outcome: Result<BackgroundSyncOutcome>): ListenableWorker.Result = runUnitTestResult(outcome)

    private fun runUnitTestResult(outcome: Result<BackgroundSyncOutcome>): ListenableWorker.Result {
        lateinit var result: ListenableWorker.Result
        runUnitTest {
            given()
            val fixture = SutFixture()
            coEvery { fixture.sync.invoke() } returns outcome

            whenn()
            result = fixture.doWork()

            then()
        }
        return result
    }

    @Test
    fun `should succeed when background sync completed`() {
        assertEquals(ListenableWorker.Result.success(), resultFor(Result.success(BackgroundSyncOutcome.SYNCED)))
    }

    @Test
    fun `should retry when background sync aborted`() {
        assertEquals(ListenableWorker.Result.retry(), resultFor(Result.success(BackgroundSyncOutcome.ABORTED)))
    }

    @Test
    fun `should retry when sdk is not initialized yet`() {
        assertEquals(ListenableWorker.Result.retry(), resultFor(Result.success(BackgroundSyncOutcome.NOT_INITIALIZED)))
    }

    @Test
    fun `should fail when app is in foreground`() {
        assertEquals(ListenableWorker.Result.failure(), resultFor(Result.success(BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND)))
    }

    @Test
    fun `should fail when there is nothing to sync`() {
        assertEquals(ListenableWorker.Result.failure(), resultFor(Result.success(BackgroundSyncOutcome.NO_DATA)))
    }

    @Test
    fun `should fail when background sync action failed`() {
        assertEquals(ListenableWorker.Result.failure(), resultFor(Result.failure(IllegalStateException("boom"))))
    }

    @Test
    fun `should retry when sdk instance does not exist yet`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val result = fixture.doWork(instance = null)

        then()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `should enqueue unique periodic work with network constraint and default delay`() = runUnitTest {
        given()
        val workManager = mockk<WorkManager>(relaxed = true)
        val request = slot<PeriodicWorkRequest>()
        val previousDebugView = Util.isDebugView()
        Util.setIsDebug(false)

        whenn()
        val id = try {
            PushDataWorker.enqueuePeriodicWork(workManager)
        } finally {
            Util.setIsDebug(previousDebugView)
        }

        then()
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(PushDataWorker.PUSH_DATA_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, capture(request)) }
        assertEquals(id, request.captured.id)
        assertEquals(TimeUnit.MINUTES.toMillis(15), request.captured.workSpec.initialDelay)
        assertEquals(NetworkType.CONNECTED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `should use short initial delay in debug view`() = runUnitTest {
        given()
        val workManager = mockk<WorkManager>(relaxed = true)
        val request = slot<PeriodicWorkRequest>()
        val previousDebugView = Util.isDebugView()
        Util.setIsDebug(true)

        whenn()
        try {
            PushDataWorker.enqueuePeriodicWork(workManager)
        } finally {
            Util.setIsDebug(previousDebugView)
        }

        then()
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), capture(request)) }
        assertEquals(TimeUnit.SECONDS.toMillis(20), request.captured.workSpec.initialDelay)
    }
}
