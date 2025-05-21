package com.reteno.core.domain.controller

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import io.mockk.clearMocks
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ScheduledExecutorService

class ScheduleControllerImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val CLEAR_OLD_DATA_DELAY = 3000L

        private lateinit var scheduler: ScheduledExecutorService
    }
    // endregion constants -------------------------------------------------------------------------

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
    private lateinit var deeplinkController: DeeplinkController


    private lateinit var SUT: ScheduleController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = ScheduleControllerImpl(
            contactController = contactController,
            interactionController = interactionController,
            eventController = eventController,
            appInboxController = appInboxController,
            recommendationController = recommendationController,
            deepLinkController = deeplinkController,
            workManagerProvider = mockk(relaxed = true)
        )
        scheduler = application.scheduler
    }

    override fun after() {
        super.after()
        clearMocks(PushOperationQueue)
    }

    @Test
    fun giveSchedulerControllerFirstCalled_whenStartScheduler_thenScheduleNewFixRateTask() {
        // When
        SUT.startScheduler()

        // Then
        verify { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) }
    }

    @Test
    fun whenStartScheduler_thenAddPushOperation() {
        // When
        SUT.startScheduler()

        // Then
        verify(exactly = 7) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
        verify { contactController.pushDeviceData() }
        verify { contactController.pushUserData() }
        verify { interactionController.pushInteractions() }
        verify { eventController.pushEvents() }
        verify { appInboxController.pushAppInboxMessagesStatus() }
        verify { deeplinkController.pushDeeplink() }
    }

    @Test
    fun whenStopSchedule_thenSchedulerShutdown() {
        // When
        SUT.startScheduler()
        SUT.stopScheduler()

        // Then
        verify { scheduler.shutdownNow() }
    }

    @Test
    fun whenForcePush_thenAddPushOperation() {
        // When
        SUT.forcePush()

        // Then
        verify(exactly = 7) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenForcePushCalledTwiceOneSecond_thenDoesNotAddPushOperationTwice() {
        // When
        SUT.forcePush()
        SUT.forcePush()

        // Then
        verify(exactly = 7) { PushOperationQueue.addOperation(any()) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenClearOldOperation_thenAddOperationToQueueWithDelay() {
        // When
        SUT.clearOldData()

        // Then
        verify(exactly = 5) { OperationQueue.addOperationAfterDelay(any(), eq(CLEAR_OLD_DATA_DELAY)) }
        verify { interactionController.clearOldInteractions() }
        verify { eventController.clearOldEvents() }
        verify { appInboxController.clearOldMessagesStatus() }
        verify { recommendationController.clearOldRecommendations() }
        verify { deeplinkController.clearOldDeeplinks() }
    }

    @Test
    fun test() {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneId.of("UTC"))
        println(formatter.format(ZonedDateTime.now()))
    }

}