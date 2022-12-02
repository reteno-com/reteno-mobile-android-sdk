package com.reteno.core.domain.controller

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class ScheduleControllerTest : BaseRobolectricTest() {

    companion object {
        private const val CLEAR_OLD_DATA_DELAY = 3000L
    }

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var interactionController: InteractionController

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var appInboxController: AppInboxController

    @RelaxedMockK
    private lateinit var recommendationController: RecommendationController

    @RelaxedMockK
    private lateinit var scheduler: ScheduledExecutorService

    private lateinit var SUT: ScheduleController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()

        mockkStatic(Executors::class)
        mockkObject(PushOperationQueue)
        val currentThreadExecutor = Executor(Runnable::run)
        every { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) } answers {
            currentThreadExecutor.execute(firstArg())
            mockk()
        }
        every { Executors.newScheduledThreadPool(any(), any()) } returns scheduler

        SUT = ScheduleControllerImpl(contactController, interactionController, eventController, appInboxController, recommendationController, mockk(relaxed = true))
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
        unmockkStatic(Executors::class)
    }

    @Test
    fun firstCalled_scheduleNewFixRateTask() {
        SUT.startScheduler()

        verify { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) }
    }

    @Test
    fun executionTask_addedPushOperation() {
        val currentThreadExecutor = Executor(Runnable::run)
        every { PushOperationQueue.addOperation(any()) } answers {
            currentThreadExecutor.execute(firstArg())
            PushOperationQueue.nextOperation()
        }

        SUT.startScheduler()

        verify(exactly = 6) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 7) { PushOperationQueue.nextOperation() }
        verify { contactController.pushDeviceData() }
        verify { contactController.pushUserData() }
        verify { interactionController.pushInteractions() }
        verify { eventController.pushEvents() }
        verify { appInboxController.pushAppInboxMessagesStatus() }
    }

    @Test
    fun stopSchedule() {
        SUT.startScheduler()
        SUT.stopScheduler()

        verify { scheduler.shutdownNow() }
    }

    @Test
    fun forcePush_addPushOperation() {
        SUT.forcePush()

        verify(exactly = 6) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun forcePushCalledTwiceOneSecond_doesNotAddPushOperationTwice() {
        SUT.forcePush()
        SUT.forcePush()

        verify(exactly = 6) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun clearOldOperation_thenAddOperationToQueueWithDelay() {
        SUT.clearOldData()

        verify(exactly = 4) { OperationQueue.addOperationAfterDelay(any(), eq(CLEAR_OLD_DATA_DELAY)) }
        verify { interactionController.clearOldInteractions() }
        verify { eventController.clearOldEvents() }
        verify { appInboxController.clearOldMessagesStatus() }
    }

}