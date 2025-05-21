package com.reteno.core.domain.controller

interface ScheduleController {

    /**
     *  Starts fixed scheduler for pushing saved data
     *   ([com.reteno.core.model.device.Device], [com.reteno.core.model.user.User], [com.reteno.core.model.event.Events] etc.)
     *
     *  Scheduler params:
     *  - start delay - [REGULAR_DELAY] + random delay up to [RANDOM_DELAY];
     *  - delay - [REGULAR_DELAY]
     */
    fun startScheduler()

    /**
     *  Starts WorkManager which syncs data when app is not running in foreground.
     */
    fun enqueueBackgroundWorker()

    /**
     *  Cancels push data scheduler.
     */
    fun stopScheduler()

    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePush()

    /**
     *  Deletes [com.reteno.core.model.event.Event] and [com.reteno.core.model.interaction.Interaction]
     *  older than 24 hours from the database
     *
     *  @see com.reteno.core.domain.SchedulerUtils
     */
    fun clearOldData()
}