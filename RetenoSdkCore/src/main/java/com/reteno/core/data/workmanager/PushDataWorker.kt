package com.reteno.core.data.workmanager

import android.content.Context
import androidx.work.*
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import java.util.*
import java.util.concurrent.TimeUnit

internal class PushDataWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        /*@formatter:off*/ Logger.i(TAG, "doWork(): ", "")
        /*@formatter:on*/
        return try {
            return doWorkActual()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "doWork(): ", ex)
            /*@formatter:on*/
            Result.failure()
        }
    }

    private fun doWorkActual(): Result {
        val serviceLocator =
            ((applicationContext as RetenoApplication).getRetenoInstance() as RetenoImpl).serviceLocator
        val activityHelper = serviceLocator.retenoActivityHelperProvider.get()
        val isForeground = activityHelper.canPresentMessages()
        return if (isForeground) {
            /*@formatter:off*/ Logger.i(TAG, "doWork(): ", "App is in foreground, nothing to do")
            /*@formatter:on*/

            Result.failure()
        } else {
            /*@formatter:off*/ Logger.i(TAG, "doWork(): ", "App is in background")
            /*@formatter:on*/
            val databaseManager = serviceLocator.retenoDatabaseManagerProvider.get()
            if (databaseManager.isDatabaseEmpty()) {
                /*@formatter:off*/ Logger.i(TAG, "doWork(): ", "Database is empty, nothing to do, cancelling periodic work")
                /*@formatter:on*/
                WorkManager.getInstance(applicationContext).cancelUniqueWork(PUSH_DATA_WORK_NAME)
                Result.failure()
            } else {
                /*@formatter:off*/ Logger.i(TAG, "doWork(): ", "Database has data, sending to server")
                /*@formatter:on*/
                val scheduleController = serviceLocator.scheduleControllerProvider.get()
                scheduleController.forcePush()
                Result.success()
            }
        }
    }

    companion object {
        private val TAG: String = PushDataWorker::class.java.simpleName

        private const val PUSH_DATA_WORK_NAME = "PUSH_DATA_TASK_TAG"
        private val EXISTING_PERIODIC_WORK_POLICY = ExistingPeriodicWorkPolicy.KEEP

        private val INITIAL_DELAY_DEBUG = TimeInterval(20L, TimeUnit.SECONDS)
        private val INITIAL_DELAY_DEFAULT = TimeInterval(15L, TimeUnit.MINUTES)
        private val INTERVAL = TimeInterval(15L, TimeUnit.SECONDS)

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