package com.reteno

import android.app.Activity
import android.app.Application
import androidx.annotation.NonNull
import com.reteno.config.DeviceIdMode
import com.reteno.config.RestConfig
import com.reteno.di.ServiceLocator
import com.reteno.domain.controller.EventController
import com.reteno.lifecycle.RetenoActivityHelper
import com.reteno.lifecycle.RetenoLifecycleCallbacks
import com.reteno.util.Logger


class RetenoImpl(application: Application) : RetenoLifecycleCallbacks, Reteno {

    init {
        /*@formatter:off*/ Logger.i(TAG, "RetenoImpl(): ", "context = [" , application , "]")
        /*@formatter:on*/
        Companion.application = application
    }

    val serviceLocator: ServiceLocator = ServiceLocator()

    private val restConfig: RestConfig = serviceLocator.restConfigProvider.get()
    val activityHelper: RetenoActivityHelper =
        serviceLocator.retenoActivityHelperProvider.get()
    private val eventsController: EventController = serviceLocator.eventsControllerProvider.get()

    private val sharedPrefsManager = serviceLocator.sharedPrefsManagerProvider.get()

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

    override fun changeDeviceIdMode(deviceIdMode: DeviceIdMode) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            restConfig.deviceId.changeDeviceIdMode(deviceIdMode)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
        }
    }

    override fun setExternalDeviceId(@NonNull externalDeviceId: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalDeviceId(): ", "externalDeviceId = [" , externalDeviceId , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            restConfig.deviceId.setExternalDeviceId(externalDeviceId)
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