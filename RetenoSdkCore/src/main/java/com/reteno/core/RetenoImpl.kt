package com.reteno.core

import android.app.Activity
import android.app.Application
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.user.User
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Logger


class RetenoImpl(application: Application, accessKey: String) : RetenoLifecycleCallbacks, Reteno {

    init {
        /*@formatter:off*/ Logger.i(TAG, "RetenoImpl(): ", "context = [" , application , "]")
        /*@formatter:on*/
        Companion.application = application
    }

    val serviceLocator: ServiceLocator = ServiceLocator(application, accessKey)

    private val contactController by lazy { serviceLocator.contactControllerProvider.get() }
    private val scheduleController by lazy { serviceLocator.scheduleControllerProvider.get() }
    private val eventController by lazy { serviceLocator.eventsControllerProvider.get() }

    private val activityHelper: RetenoActivityHelper by lazy { serviceLocator.retenoActivityHelperProvider.get() }

    init {
        try {
            activityHelper.enableLifecycleCallbacks(this)
        } catch (t: Throwable) {
            Logger.e(TAG, "init(): ", t)
        }
    }

    override fun resume(activity: Activity?) {
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        clearOldData()
        startPushScheduler()
        // TODO: Application is in foreground
    }

    override fun pause(activity: Activity?) {
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        stopPushScheduler()
        // TODO: Application is not in foreground
    }

    override fun setUserAttributes(externalUserId: String) {
        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        setUserAttributes(externalUserId, null)
    }

    override fun setUserAttributes(externalUserId: String, user: User?) {
        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "], used = [" , user , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            contactController.setExternalUserId(externalUserId)
            setUserData(user)
        } catch (ex: Throwable) {
            Logger.e(TAG, "setUserAttributes(): ", ex)
        }
    }

    override fun logEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "logEvent(): ", "eventType = [" , event.eventTypeKey , "], date = [" , event.occurred , "], parameters = [" , event.params , "]")
        /*@formatter:on*/
        eventController.trackEvent(event)
    }

    override fun logScreenView(screenName: String) {
        /*@formatter:off*/ Logger.i(TAG, "logScreenView(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        eventController.trackScreenViewEvent(screenName)
    }

    override fun autoScreenTracking(config: ScreenTrackingConfig) {
        /*@formatter:off*/ Logger.i(TAG, "autoScreenTracking(): ", "config = [" , config , "]")
        /*@formatter:on*/
        activityHelper.autoScreenTracking(config)
    }

    override fun forcePushData() {
        /*@formatter:off*/ Logger.i(TAG, "forcePushData(): ", "")
        /*@formatter:on*/
        scheduleController.forcePush()
    }

    private fun setUserData(user: User?) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "used = [" , user , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            user?.let(contactController::setUserData)
        } catch (ex: Throwable) {
            Logger.e(TAG, "setExternalDeviceId(): ", ex)
        }
    }

    private fun clearOldData() {
        scheduleController.clearOldData()
    }

    private fun startPushScheduler() {
        scheduleController.startScheduler()
    }

    private fun stopPushScheduler() {
        scheduleController.stopScheduler()
    }

    /**
     * For testing purposes
     * DON'T EVER CALL THIS METHOD!
     */
    @Deprecated("DON'T EVER CALL THIS METHOD! It is for testing only")
    private fun testCrash() {
        try {
            throw NullPointerException("This is a test crash in SDK")
        } catch (t: Throwable) {
            Logger.e(TAG, "testCrash(): ", t)
        }
    }

    companion object {
        val TAG: String = RetenoImpl::class.java.simpleName

        lateinit var application: Application
            private set
    }
}