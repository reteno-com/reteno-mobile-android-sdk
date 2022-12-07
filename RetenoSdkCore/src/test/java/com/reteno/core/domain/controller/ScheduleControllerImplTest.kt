package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

class ScheduleControllerImplTest : BaseUnitTest() {

    companion object {
        private const val CLEAR_OLD_DATA_DELAY = 3000L

        private lateinit var scheduler: ScheduledExecutorService

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockStaticLog()
            mockObjectOperationQueue()
            mockObjectPushOperationQueue()

            scheduler = mockStaticScheduler()
            mockObjectPushDataWorker()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockStaticLog()
            unMockObjectOperationQueue()
            unMockObjectPushOperationQueue()

            unMockStaticScheduler()
            unMockObjectPushDataWorker()
        }
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



    private lateinit var SUT: ScheduleController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = ScheduleControllerImpl(contactController, interactionController, eventController, appInboxController, recommendationController, mockk(relaxed = true))
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