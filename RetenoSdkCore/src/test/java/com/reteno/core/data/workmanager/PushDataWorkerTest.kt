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
import com.reteno.core.fixtures.FakeFacadeInfrastructure
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.util.Util
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class PushDataWorkerTest {

    private class SutFixture(scope: TestScope) {
        private val context: Context = ApplicationProvider.getApplicationContext()
        val sync = mockk<BackgroundSync>()
        val sdk = RetenoInternalImpl(
            application = ApplicationProvider.getApplicationContext(),
            mainDispatcher = StandardTestDispatcher(scope.testScheduler),
            appLifecycleOwner = ManualLifecycleOwner(),
            infrastructure = FakeFacadeInfrastructure(FakeSdkActions().also { it.backgroundSyncAnswer = sync }),
            isOsSupported = { true }
        )
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

    @Test
    fun `should succeed when background sync completed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.success(BackgroundSyncOutcome.SYNCED)

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `should retry when background sync aborted`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.success(BackgroundSyncOutcome.ABORTED)

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `should retry when sdk is not initialized yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.success(BackgroundSyncOutcome.NOT_INITIALIZED)

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `should fail when app is in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.success(BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND)

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `should fail when there is nothing to sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.success(BackgroundSyncOutcome.NO_DATA)

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `should fail when background sync action failed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        coEvery { fixture.sync.invoke() } returns Result.failure(IllegalStateException("boom"))

        whenn()
        val result = fixture.doWork()

        then()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `should retry when sdk instance does not exist yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

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
        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                PushDataWorker.PUSH_DATA_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(request)
            )
        }
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
