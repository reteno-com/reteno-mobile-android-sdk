package com.reteno.core.domain.controller

import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.di.provider.WorkManagerProvider
import com.reteno.core.util.Logger
import com.reteno.core.util.RetenoThreadFactory
import com.reteno.core.util.Util
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

internal class ScheduleControllerImpl(
    private val contactController: ContactController,
    private val interactionController: InteractionController,
    private val eventController: EventController,
    private val appInboxController: AppInboxController,
    private val recommendationController: RecommendationController,
    private val deepLinkController: DeeplinkController,
    private val workManagerProvider: WorkManagerProvider
) : ScheduleController {

    companion object {
        private val TAG: String = ScheduleControllerImpl::class.java.simpleName
        private const val REGULAR_DELAY_DEBUG_VIEW = 10_000L

        /** REGULAR_DELAY should be not less than @see [com.reteno.core.data.remote.api.RestClientImpl.TIMEOUT]
         * to prevent adding new operations while REST queries are ongoing
         **/
        private const val REGULAR_DELAY = 30_000L

        private const val RANDOM_DELAY = 10_000L
        private const val FORCE_PUSH_MIN_DELAY = 1_000L
        private const val CLEAR_OLD_DATA_DELAY = 3_000L
    }

    private var scheduler: ScheduledExecutorService? = null
    private var lastForcePushTime: Long = 0


    /**
     *  Starts fixed scheduler for pushing saved data
     *   ([com.reteno.core.model.device.Device], [com.reteno.core.model.user.User], [com.reteno.core.model.event.Events] etc.)
     *
     *  Scheduler params:
     *  - start delay - [REGULAR_DELAY] + random delay up to [RANDOM_DELAY];
     *  - delay - [REGULAR_DELAY]
     */
    override fun startScheduler() {
        /*@formatter:off*/ Logger.i(TAG, "startScheduler(): ", "")
        /*@formatter:on*/
        val delay = if (Util.isDebugView()) REGULAR_DELAY_DEBUG_VIEW else REGULAR_DELAY
        scheduler?.shutdownNow()
        scheduler = Executors.newScheduledThreadPool(1, RetenoThreadFactory())
        scheduler?.scheduleAtFixedRate(
            {
                sendData()
            },
            delay + Random.nextLong(RANDOM_DELAY),
            delay,
            TimeUnit.MILLISECONDS
        )
        enqueueBackgroundWorker()
    }

    override fun enqueueBackgroundWorker() {
        OperationQueue.addParallelOperation {
            runCatching {
                PushDataWorker.enqueuePeriodicWork(workManagerProvider.get())
            }.getOrElse {
                Logger.e(TAG, "enqueueBackgroundWorker():", it)
            }
        }
    }

    /**
     *  Cancels push data scheduler.
     */
    override fun stopScheduler() {
        /*@formatter:off*/ Logger.i(TAG, "stopScheduler(): ", "")
        /*@formatter:on*/
        scheduler?.shutdownNow()
        scheduler = null
    }

    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [FORCE_PUSH_MIN_DELAY] millis.
     */
    override fun forcePush() {
        /*@formatter:off*/ Logger.i(TAG, "forcePush(): ", "")
        /*@formatter:on*/
        if (System.currentTimeMillis() - lastForcePushTime < FORCE_PUSH_MIN_DELAY) {
            Logger.d(TAG, "forcePush method called to quickly")
            return
        }
        lastForcePushTime = System.currentTimeMillis()

        sendData()
    }

    /**
     *  Deletes [com.reteno.core.model.event.Event] and [com.reteno.core.model.interaction.Interaction]
     *  older than 24 hours from the database
     *
     *  @see com.reteno.core.domain.SchedulerUtils
     */
    override fun clearOldData() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldData(): ", "")
        /*@formatter:on*/
        OperationQueue.addOperationAfterDelay(
            interactionController::clearOldInteractions,
            CLEAR_OLD_DATA_DELAY
        )
        OperationQueue.addOperationAfterDelay(
            eventController::clearOldEvents,
            CLEAR_OLD_DATA_DELAY
        )
        OperationQueue.addOperationAfterDelay(
            appInboxController::clearOldMessagesStatus,
            CLEAR_OLD_DATA_DELAY
        )
        OperationQueue.addOperationAfterDelay(
            recommendationController::clearOldRecommendations,
            CLEAR_OLD_DATA_DELAY
        )
        OperationQueue.addOperationAfterDelay(
            deepLinkController::clearOldDeeplinks,
            CLEAR_OLD_DATA_DELAY
        )
    }

    private fun sendData() {
        /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "") 
        /*@formatter:on*/
        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushDeviceData")
            /*@formatter:on*/
            contactController.pushDeviceData()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushUserData")
            /*@formatter:on*/
            contactController.pushUserData()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushInteractions")
            /*@formatter:on*/
            interactionController.pushInteractions()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushEvents")
            /*@formatter:on*/
            eventController.pushEvents()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushAppInboxStatuses")
            /*@formatter:on*/
            appInboxController.pushAppInboxMessagesStatus()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushRecommendations")
            /*@formatter:on*/
            recommendationController.pushRecommendations()
        }

        PushOperationQueue.addOperation {
            /*@formatter:off*/ Logger.i(TAG, "sendData(): ", "step: pushDeeplink")
            /*@formatter:on*/
            deepLinkController.pushDeeplink()
        }

        PushOperationQueue.nextOperation()
    }

}