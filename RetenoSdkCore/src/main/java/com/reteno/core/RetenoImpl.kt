package com.reteno.core

import android.app.Activity
import android.app.Application
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.di.ServiceLocator
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
import com.reteno.core.model.user.User
import com.reteno.core.util.Logger


class RetenoImpl(application: Application) : RetenoLifecycleCallbacks, Reteno {

    init {
        /*@formatter:off*/ Logger.i(TAG, "RetenoImpl(): ", "context = [" , application , "]")
        /*@formatter:on*/
        Companion.application = application
    }

    val serviceLocator: ServiceLocator = ServiceLocator()

    private val contactController = serviceLocator.contactControllerProvider.get()
    private val activityHelper: RetenoActivityHelper =
        serviceLocator.retenoActivityHelperProvider.get()

    init {
        try {
            activityHelper.enableLifecycleCallbacks(this)
        } catch (t: Throwable) {
            Logger.e(TAG, "init(): ", t)
        }
    }

    override fun resume(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        // TODO: Application is in foreground
    }

    override fun pause(activity: Activity) {
        /*@formatter:off*/ Logger.i(TAG, "pause(): ", "activity = [" , activity , "]")
        /*@formatter:on*/
        // TODO: Application is not in foreground
    }

    override fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: () -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            contactController.setDeviceIdMode(deviceIdMode, onDeviceIdChanged)
        } catch (ex: Throwable) {
            Logger.e(TAG, "setDeviceIdMode(): ", ex)
        }
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
            user?.let { setUserData(it) }
        } catch (ex: Throwable) {
            Logger.e(TAG, "setUserAttributes(): ", ex)
        }
    }

    private fun setUserData(used: User) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "used = [" , used , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            contactController.setUserData(used)
        } catch (ex: Throwable) {
            Logger.e(TAG, "setExternalDeviceId(): ", ex)
        }
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