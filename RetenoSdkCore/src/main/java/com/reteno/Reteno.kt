package com.reteno

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import com.reteno.config.DeviceIdMode
import com.reteno.config.RestConfig
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager


class Reteno(application: Application) : RetenoLifecycleCallbacks {

    internal val applicationContext: Context = application.applicationContext
    internal val activityHelper: RetenoActivityHelper = RetenoActivityHelper()

    init {
        activityHelper.enableLifecycleCallbacks(this, application)
        SharedPrefsManager.init(application.applicationContext)
        RestConfig.deviceId.init(application.applicationContext)
    }


    override fun pause() {
        Logger.d(TAG, "pause(): ")
        // TODO: Application is not in foreground
    }

    override fun resume() {
        Logger.d(TAG, "resume(): ")
        // TODO: Application is in foreground
    }

    fun changeDeviceIdMode(deviceIdMode: DeviceIdMode) {
        Logger.d(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        try {
            // TODO: Move this to background thread later
            RestConfig.deviceId.init(applicationContext, deviceIdMode)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
        }
    }

    fun setExternalDeviceId(@NonNull externalDeviceId: String = "") {
        Logger.d(TAG, "setExternalDeviceId(): ", "externalDeviceId = [" , externalDeviceId , "]")
        try {
            // TODO: Move this to background thread later
            RestConfig.deviceId.setExternalDeviceId(externalDeviceId)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
        }
    }

    companion object {
        val TAG: String = Reteno::class.java.simpleName
    }
}