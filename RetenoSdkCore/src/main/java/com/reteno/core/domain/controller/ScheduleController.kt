package com.reteno.core.domain.controller

import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.util.Logger
import com.reteno.core.util.RetenoThreadFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ScheduleController(
    private val contactController: ContactController,
    private val interactionController: InteractionController,
    private val eventController: EventController
) {

    companion object {
        private val TAG: String = ScheduleController::class.java.simpleName
        private const val REGULAR_DELAY = 30_000L
        private const val RANDOM_DELAY = 10_000L
        private const val FORCE_PUSH_MIN_DELAY = 1_000L
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
    fun startScheduler() {
        /*@formatter:off*/ Logger.i(TAG, "startScheduler(): ", "")
        /*@formatter:on*/
        scheduler?.shutdownNow()
        scheduler = Executors.newScheduledThreadPool(1, RetenoThreadFactory())
        scheduler?.scheduleAtFixedRate(
            {
                sendData()
            },
            REGULAR_DELAY + Random.nextLong(RANDOM_DELAY),
            REGULAR_DELAY,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     *  Cancels push data scheduler.
     */
    fun stopScheduler() {
        /*@formatter:off*/ Logger.i(TAG, "stopScheduler(): ", "")
        /*@formatter:on*/
        scheduler?.shutdownNow()
    }

    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePush() {
        /*@formatter:off*/ Logger.i(TAG, "forcePush(): ", "")
        /*@formatter:on*/
        if (System.currentTimeMillis() - lastForcePushTime < FORCE_PUSH_MIN_DELAY) {
            Logger.d(TAG, "forcePush method called to quickly")
            return
        }
        lastForcePushTime = System.currentTimeMillis()

        sendData()
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

        PushOperationQueue.nextOperation()
    }

}