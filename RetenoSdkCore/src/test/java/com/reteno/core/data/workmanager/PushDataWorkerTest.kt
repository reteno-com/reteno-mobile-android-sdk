package com.reteno.core.data.workmanager

import android.content.Context
import android.util.Log
import androidx.work.*
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class PushDataWorkerTest : BaseRobolectricTest() {

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var retenoActivityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var databaseManager: RetenoDatabaseManager

    private var executor: Executor? = null
    private lateinit var workUuid: UUID

    private var SUT: PushDataWorker? = null
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        every { reteno.serviceLocator.scheduleControllerProvider.get() } returns scheduleController
        every { reteno.serviceLocator.retenoActivityHelperProvider.get() } returns retenoActivityHelper
        every { reteno.serviceLocator.retenoDatabaseManagerProvider.get() } returns databaseManager

        executor = Executors.newSingleThreadExecutor()
        assertNotNull(executor)
        initializeWorkManager(application, executor!!)
        workUuid = PushDataWorker.enqueuePeriodicWork(WorkManager.getInstance(application))

        SUT = TestWorkerBuilder<PushDataWorker>(application, executor!!).build()
        assertNotNull(SUT)
    }

    @Test
    fun givenAppInForeground_whenDoWork_thenNothingPushedAndPeriodicWorkContinues() {
        // Given
        mockAppInForeground()

        // When
        val result = SUT!!.doWork()

        // Then
        verify(exactly = 0) { scheduleController.forcePush() }
        assertThat(result, `is`(Result.failure()))

        val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
        assertEquals(WorkInfo.State.ENQUEUED, workState.state)
    }

    @Test
    fun givenDatabaseEmptyAndAppInBackground_whenDoWork_thenNothingPushedAndPeriodicWorkIsCancelled() {
        // Given
        mockDatabaseEmpty(true)
        mockAppInBackground()

        // When
        val result = SUT!!.doWork()

        // Then
        verify(exactly = 0) { scheduleController.forcePush() }
        assertThat(result, `is`(Result.failure()))

        val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
        assertEquals(WorkInfo.State.CANCELLED, workState.state)
    }

    @Test
    fun givenDatabaseNotEmptyAndAppInBackground_whenDoWork_thenForcePushTriggeredAndPeriodicWorkContinues() {
        // Given
        mockDatabaseEmpty(false)
        mockAppInBackground()

        // When
        val result = SUT!!.doWork()

        // Then
        verify(exactly = 1) { scheduleController.forcePush() }
        assertThat(result, `is`(Result.success()))

        val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
        assertEquals(WorkInfo.State.ENQUEUED, workState.state)
    }

    @Test
    fun givenConstraintsSatisfied_whenWorkEnqueued_thenWorkIsRunning() {
        // Given
        val workManager = WorkManager.getInstance(application)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(application)!!
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<PushDataWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // When
        val operation = WorkManager.getInstance(application).enqueue(request)
        operation.result.addListener({
            // Then
            val workInfo = workManager.getWorkInfoById(request.id).get()
            assertEquals(workInfo.state, WorkInfo.State.RUNNING)
        }, executor!!)
        with(testDriver) {
            setPeriodDelayMet(request.id)
            setAllConstraintsMet(request.id)
        }
    }

    @Test
    fun givenConstraintsNotSatisfied_whenWorkEnqueued_thenWorkIsEnqueued() {
        // Given
        val workManager = WorkManager.getInstance(application)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(application)!!
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<PushDataWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // When
        WorkManager.getInstance(application).enqueue(request).result.get()
        with(testDriver) {
            setPeriodDelayMet(request.id)
        }

        // Then
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun initializeWorkManager(context: Context, executor: Executor) {
        val config: Configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(executor)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private fun mockAppInForeground() {
        every { retenoActivityHelper.canPresentMessages() } returns true
    }

    private fun mockAppInBackground() {
        every { retenoActivityHelper.canPresentMessages() } returns false
    }

    private fun mockDatabaseEmpty(isEmpty: Boolean) {
        every { databaseManager.isDatabaseEmpty() } returns isEmpty
    }
    // endregion helper methods --------------------------------------------------------------------
}