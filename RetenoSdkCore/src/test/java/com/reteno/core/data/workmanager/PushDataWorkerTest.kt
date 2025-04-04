package com.reteno.core.data.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.lifecycle.RetenoActivityHelper
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalCoroutinesApi::class)
class PushDataWorkerTest : BaseRobolectricTest() {

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic("com.reteno.core.util.UtilKt")
            mockkConstructor(ServiceLocator::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic("com.reteno.core.util.UtilKt")
            unmockkConstructor(ServiceLocator::class)
        }
    }

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var retenoActivityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var databaseManager: RetenoDatabaseManager

    @RelaxedMockK
    private lateinit var serviceLocator: ServiceLocator

    private var executor: Executor? = null
    private lateinit var workUuid: UUID

    private var SUT: PushDataWorker? = null
    // endregion helper fields ---------------------------------------------------------------------

    @Test
    fun givenAppInForeground_whenDoWork_thenNothingPushedAndPeriodicWorkContinues() = runTest {
        // Given
        mockInitials()
        mockAppInForeground()

        // When
        val result = SUT!!.doWork()

        // Then
        verify(exactly = 0) { scheduleController.forcePush() }
        assertThat(result, `is`(Result.failure()))

        val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
        assertEquals(WorkInfo.State.ENQUEUED, workState.state)
        RetenoInternalImpl.swapInstance(null)
    }

    @Test
    fun givenDatabaseEmptyAndAppInBackground_whenDoWork_thenNothingPushedAndPeriodicWorkIsCancelled() =
        runTest {
            // Given
            mockInitials()
            mockDatabaseEmpty(true)
            mockAppInBackground()

            // When
            val result = SUT!!.doWork()

            // Then
            verify(exactly = 0) { scheduleController.forcePush() }
            assertThat(result, `is`(Result.failure()))

            val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
            assertEquals(WorkInfo.State.CANCELLED, workState.state)
            RetenoInternalImpl.swapInstance(null)
        }

    @Test
    fun givenDatabaseNotEmptyAndAppInBackground_whenDoWork_thenForcePushTriggeredAndPeriodicWorkContinues() =
        runTest {
            // Given
            mockInitials()
            mockDatabaseEmpty(false)
            mockAppInBackground()

            // When
            val result = SUT!!.doWork()

            // Then
            verify(exactly = 1) { scheduleController.forcePush() }
            assertThat(result, `is`(Result.success()))

            val workState = WorkManager.getInstance(application).getWorkInfoById(workUuid).get()
            assertEquals(WorkInfo.State.ENQUEUED, workState.state)
            RetenoInternalImpl.swapInstance(null)
        }

    @Test
    fun givenConstraintsSatisfied_whenWorkEnqueued_thenWorkIsRunning() = runTest {
        // Given
        mockInitials()
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
        RetenoInternalImpl.swapInstance(null)
    }

    @Test
    fun givenConstraintsNotSatisfied_whenWorkEnqueued_thenWorkIsEnqueued() = runTest {
        // Given
        mockInitials()
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
        RetenoInternalImpl.swapInstance(null)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun TestScope.mockInitials() {
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns retenoActivityHelper
        every { anyConstructed<ServiceLocator>().retenoDatabaseManagerProvider.get() } returns databaseManager
        createRetenoAndAdvanceInit()

        executor = Executors.newSingleThreadExecutor()
        assertNotNull(executor)
        initializeWorkManager(application, executor!!)
        workUuid = PushDataWorker.enqueuePeriodicWork(WorkManager.getInstance(application))

        SUT = TestWorkerBuilder<PushDataWorker>(application, executor!!).build()
        assertNotNull(SUT)
    }

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

    private fun createReteno(): RetenoInternalImpl {
        return mockk()
    }
    // endregion helper methods --------------------------------------------------------------------
}