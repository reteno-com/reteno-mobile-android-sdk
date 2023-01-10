package com.reteno.core

import com.reteno.core.appinbox.AppInbox
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.user.User
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.recommendation.Recommendation


interface Reteno {

    /**
     *  Get [AppInbox] instance.
     */
    val appInbox: AppInbox

    /**
     *  Get [Recommendation] instance.
     */
    val recommendation: Recommendation

    /**
     *  Set the user ID (Note: id should not be null or empty, or IllegalArgumentException will be thrown)
     */
    @Throws(java.lang.IllegalArgumentException::class)
    fun setUserAttributes(externalUserId: String)

    /**
     * Set the user ID (Note: id should not be null or empty, or IllegalArgumentException will be thrown)
     * and add or modify user attributes.
     *
     * @see com.reteno.core.domain.model.user
     */
    @Throws(java.lang.IllegalArgumentException::class)
    fun setUserAttributes(externalUserId: String, user: User?)

    /**
     *  Tracking events
     *  @param event model to track
     */
    fun logEvent(event: Event)

    /**
     *  Tracking e-commerce events.
     *
     *  @param ecomEvent model to track.
     */
    fun logEcommerceEvent(ecomEvent: EcomEvent)

    /**
     *  Tracking screen view events.
     *  Call this method when screen (fragment, activity, view) appeared for user
     *  @param screenName to track
     */
    fun logScreenView(screenName: String)

    /**
     * Enable/disable automatic screen tracking.
     * Screen view event happens on Fragment's onStart() by default lifecycle callback.
     * @see com.reteno.core.lifecycle.ScreenTrackingConfig for additional configuration
     *
     * @param config parameters for auto screen tracking
     */
    fun autoScreenTracking(config: ScreenTrackingConfig)


    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [com.reteno.core.domain.controller.ScheduleController.Companion.FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePushData()

    companion object {
        private val TAG: String = Reteno::class.java.simpleName
    }
}