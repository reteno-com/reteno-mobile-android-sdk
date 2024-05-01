package com.reteno.core

import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.features.appinbox.AppInbox
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.features.recommendation.Recommendation
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.view.iam.callback.InAppLifecycleCallback


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
     * @see com.reteno.core.domain.model.user.User
     */
    @Throws(java.lang.IllegalArgumentException::class)
    fun setUserAttributes(externalUserId: String, user: User?)

    /**
     * Add or modify user attributes without providing externalUserId.
     * User attributes are added/modified for anonymous contact
     *
     * @see com.reteno.core.domain.model.user.UserAttributesAnonymous
     */
    fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous)

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
     *  Enable/Disable automatic tracking app lifecycle events, which includes:
     *
     *  ApplicationInstalled
     *  ApplicationUpdated
     *  ApplicationOpened
     *  ApplicationBackgrounded
     *  PushNotificationsSubscribed
     *  PushNotificationsUnsubscribed
     *  SessionStarted
     *  SessionEnded
     *
     *  @param isEnabled - enable/disable automatic lifecycle event tracking
     */
    fun enableLifecycleEvents(isEnabled: Boolean)

    /**
     * Enable/disable automatic screen tracking.
     * Screen view event happens on Fragment's onStart() by default lifecycle callback.
     * @see com.reteno.core.lifecycle.ScreenTrackingConfig for additional configuration
     *
     * @param config parameters for auto screen tracking
     */
    fun autoScreenTracking(config: ScreenTrackingConfig)

    /**
     *  Updates status of POST_NOTIFICATIONS permission and pushes it to backend if status was changed.
     *  Call this function after acquiring result from runtime permission on Android 13 and above
     */
    fun updatePushPermissionStatus()

    /**
     * Pause or unpause In-App messages showing during app runtime.
     */
    fun pauseInAppMessages(isPaused: Boolean)

    /**
     * Add a callback to callbacks each time In-App message is displayed.
     *
     * @param inAppLifecycleCallback pass an implementation of [InAppLifecycleCallback] interface
     * or [null] to stop listening to callbacks.
     */
    fun setInAppLifecycleCallback(inAppLifecycleCallback: InAppLifecycleCallback?)

    /**
     *  Sends stored data without waiting for a send queue.
     *  But not more often than [com.reteno.core.domain.controller.ScheduleController.Companion.FORCE_PUSH_MIN_DELAY] millis.
     */
    fun forcePushData()

    /**
     * Change logic of In-App messages in paused state
     * Default state is [InAppPauseBehaviour.POSTPONE_IN_APPS]
     *
     * @param behaviour - new behaviour
     * @see InAppPauseBehaviour for detailed explanation
     */
    fun setInAppMessagesPauseBehaviour(behaviour: InAppPauseBehaviour)

    companion object {
        private val TAG: String = Reteno::class.java.simpleName
    }
}