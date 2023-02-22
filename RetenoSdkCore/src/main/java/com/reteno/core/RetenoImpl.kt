package com.reteno.core

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Constants.BROADCAST_ACTION_RETENO_APP_RESUME
import com.reteno.core.util.Logger
import com.reteno.core.util.isOsVersionSupported
import com.reteno.core.util.queryBroadcastReceivers


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

    override val appInbox by lazy { serviceLocator.appInboxProvider.get() }
    override val recommendation by lazy { serviceLocator.recommendationProvider.get() }

    private val activityHelper: RetenoActivityHelper by lazy { serviceLocator.retenoActivityHelperProvider.get() }

    init {
        if (isOsVersionSupported()) {
            try {
                activityHelper.enableLifecycleCallbacks(this)
            } catch (t: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "init(): ", t)
                /*@formatter:on*/
            }
        }
    }

    override fun resume(activity: Activity?) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        try {
            clearOldData()
            contactController.checkIfDeviceRegistered()
            sendAppResumeBroadcast()
            startPushScheduler()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "resume(): ", ex)
            /*@formatter:on*/
        }
    }

    private fun sendAppResumeBroadcast() {
        val intent = Intent(BROADCAST_ACTION_RETENO_APP_RESUME).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val infoList = application.queryBroadcastReceivers(intent)
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                application.sendBroadcast(intent)
            }
        }
    }

    override fun pause(activity: Activity?) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        try {
            stopPushScheduler()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "pause(): ", ex)
            /*@formatter:on*/
        }
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "]")
        /*@formatter:on*/
        if (externalUserId.isBlank()) {
            val exception = IllegalArgumentException("externalUserId should not be null or blank")
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): ", exception)
            /*@formatter:on*/
            throw exception
        }

        setUserAttributes(externalUserId, null)
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun setUserAttributes(externalUserId: String, user: User?) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "setUserAttributes(): ", "externalUserId = [" , externalUserId , "], used = [" , user , "]")
        /*@formatter:on*/
        if (externalUserId.isBlank()) {
            val exception = IllegalArgumentException("externalUserId should not be null or blank")
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): ", exception)
            /*@formatter:on*/
            throw exception
        }

        try {
            contactController.setExternalUserId(externalUserId)
            contactController.setUserData(user)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "setUserAttributes(): externalUserId = [$externalUserId], user = [$user]", ex)
            /*@formatter:on*/
        }
    }

    override fun setAnonymousUserAttributes(userAttributes: UserAttributesAnonymous) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "setAnonymousUserAttributes(): ", "userAttributes = [", userAttributes, "]")
        /*@formatter:on*/
        try {
            contactController.setAnonymousUserAttributes(userAttributes)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "setAnonymousUserAttributes(): userAttributes = [$userAttributes]", ex)
            /*@formatter:on*/
        }
    }

    override fun logEvent(event: Event) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "logEvent(): ", "eventType = [" , event.eventTypeKey , "], date = [" , event.occurred , "], parameters = [" , event.params , "]")
        /*@formatter:on*/
        try {
            eventController.trackEvent(event)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logEvent(): event = [$event]", ex)
            /*@formatter:on*/
        }
    }

    override fun logEcommerceEvent(ecomEvent: EcomEvent) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "logEcommerceEvent(): ", "ecomEvent = [" , ecomEvent , "]")
        /*@formatter:on*/
        try {
            eventController.trackEcomEvent(ecomEvent)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logEcommerceEvent(): ecomEvent = [$ecomEvent]", ex)
            /*@formatter:on*/
        }
    }

    override fun logScreenView(screenName: String) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "logScreenView(): ", "screenName = [" , screenName , "]")
        /*@formatter:on*/
        try {
            eventController.trackScreenViewEvent(screenName)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "logScreenView(): screenName = [$screenName]", ex)
            /*@formatter:on*/
        }
    }

    override fun autoScreenTracking(config: ScreenTrackingConfig) {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "autoScreenTracking(): ", "config = [" , config , "]")
        /*@formatter:on*/
        try {
            activityHelper.autoScreenTracking(config)
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "autoScreenTracking(): config = [$config]", ex)
            /*@formatter:on*/
        }
    }

    override fun forcePushData() {
        if (!isOsVersionSupported()) {
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "forcePushData(): ", "")
        /*@formatter:on*/
        try {
            scheduleController.forcePush()
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "forcePushData(): ", ex)
            /*@formatter:on*/
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
        } catch (ex: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "testCrash(): ", ex)
            /*@formatter:on*/
        }
    }

    companion object {
        private val TAG: String = RetenoImpl::class.java.simpleName

        lateinit var application: Application
            private set
    }
}