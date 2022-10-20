package com.reteno.core

import android.app.Activity
import android.app.Application
import androidx.annotation.NonNull
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.di.ServiceLocator
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallbacks
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
            Logger.captureException(ex)
        }
    }

    override fun setExternalDeviceId(@NonNull externalDeviceId: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalDeviceId(): ", "externalDeviceId = [" , externalDeviceId , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            contactController.setExternalDeviceId(externalDeviceId)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
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