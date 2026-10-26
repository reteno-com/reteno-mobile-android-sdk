package com.reteno.core.data.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reteno.core.Reteno
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class PushDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sdk = Reteno.instanceInternal ?: return Result.retry()
        val outcome = sdk.backgroundSync().getOrNull() ?: return Result.failure()
        return when (outcome) {
            BackgroundSyncOutcome.SYNCED -> Result.success()
            BackgroundSyncOutcome.NOT_INITIALIZED,
            BackgroundSyncOutcome.ABORTED -> Result.retry()
            BackgroundSyncOutcome.SKIPPED_IN_FOREGROUND,
            BackgroundSyncOutcome.NO_DATA -> Result.failure()
        }
    }

    companion object {
        private val TAG: String = PushDataWorker::class.java.simpleName

        internal const val PUSH_DATA_WORK_NAME = "PUSH_DATA_TASK_TAG"
        private val EXISTING_PERIODIC_WORK_POLICY = ExistingPeriodicWorkPolicy.KEEP

        private val INITIAL_DELAY_DEBUG = TimeInterval(20L, TimeUnit.SECONDS)
        private val INITIAL_DELAY_DEFAULT = TimeInterval(15L, TimeUnit.MINUTES)
        private val INTERVAL = TimeInterval(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)

        /**
         * Enqueues periodic work provided by PushDataWorker
         * @return UUID of the periodic work that was submitted
         */
        internal fun enqueuePeriodicWork(workManager: WorkManager): UUID {
            /*@formatter:off*/ Logger.i(TAG, "enqueuePushWorkManagerPeriodicWork(): ")
            /*@formatter:on*/
            val workRequest = buildWorker()
            workManager.enqueueUniquePeriodicWork(
                PUSH_DATA_WORK_NAME,
                EXISTING_PERIODIC_WORK_POLICY,
                workRequest
            )
            return workRequest.id
        }

        private fun buildWorker(): PeriodicWorkRequest {
            val initialDelay = if (Util.isDebugView()) {
                INITIAL_DELAY_DEBUG
            } else {
                INITIAL_DELAY_DEFAULT
            }

            return PeriodicWorkRequestBuilder<PushDataWorker>(INTERVAL.duration, INTERVAL.timeUnit)
                .addTag(PUSH_DATA_WORK_NAME)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(initialDelay.duration, initialDelay.timeUnit)
                .build()
        }
    }

    private data class TimeInterval(val duration: Long, val timeUnit: TimeUnit)
}