package com.reteno

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import com.reteno.config.DeviceIdMode
import com.reteno.config.RestConfig
import com.reteno.data.remote.api.ApiClient
import com.reteno.data.remote.api.ApiClientImpl
import com.reteno.data.remote.ds.EventsRepository
import com.reteno.data.remote.ds.EventsRepositoryImpl
import com.reteno.domain.controller.EventController
import com.reteno.util.Logger
import com.reteno.util.SharedPrefsManager


class Reteno(application: Application) : RetenoLifecycleCallbacks {

    internal val applicationContext: Context = application.applicationContext
    internal val activityHelper: RetenoActivityHelper = RetenoActivityHelper()

    private val apiClient: ApiClient = ApiClientImpl()
    private val eventsRepository: EventsRepository = EventsRepositoryImpl(apiClient)
    private val eventController: EventController = EventController(eventsRepository)

    init {
        try {
            activityHelper.enableLifecycleCallbacks(this, application)
            SharedPrefsManager.init(application.applicationContext)
            RestConfig.deviceId.init(application.applicationContext)
        } catch (t: Throwable) {
            Logger.e(TAG, "init(): ", t)
        }
    }


    override fun pause() {
        /*@formatter:off*/ Logger.i(TAG, "pause(): ")
        /*@formatter:on*/
        // TODO: Application is not in foreground
    }

    override fun resume() {
        /*@formatter:off*/ Logger.i(TAG, "resume(): ", "")
        /*@formatter:on*/
        // TODO: Application is in foreground
    }

    fun changeDeviceIdMode(deviceIdMode: DeviceIdMode) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            RestConfig.deviceId.init(applicationContext, deviceIdMode)
        } catch (ex: Throwable) {
            Logger.captureException(ex)
        }
    }

    fun setExternalDeviceId(@NonNull externalDeviceId: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalDeviceId(): ", "externalDeviceId = [" , externalDeviceId , "]")
        /*@formatter:on*/
        try {
            // TODO: Move this to background thread later
            RestConfig.deviceId.setExternalDeviceId(externalDeviceId)
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
        val TAG: String = Reteno::class.java.simpleName
    }
}