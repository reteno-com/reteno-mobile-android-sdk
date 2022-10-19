package com.reteno.core.domain.controller

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.ds.ConfigRepository
import com.reteno.core.data.remote.ds.ContactRepository
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.util.Logger

class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    fun setExternalDeviceId(id: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalDeviceId(): ", "id = [" , id , "]")
        /*@formatter:on*/
        configRepository.setExternalDeviceId(id)
        onNewContact()
    }

    fun changeDeviceIdMode(deviceIdMode: DeviceIdMode, onIdChangedCallback: () -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        configRepository.changeDeviceIdMode(deviceIdMode) {
            onNewContact()
            onIdChangedCallback.invoke()
        }
    }

    fun onNewFcmToken(token: String) {
        val oldToken = configRepository.getFcmToken()
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "oldToken = [" , oldToken , "], newToken = [" , token , "]")
        /*@formatter:on*/
        if (token != oldToken) {
            configRepository.saveFcmToken(token)
            onNewContact()
        }
    }

    private fun onNewContact() {
        val token = configRepository.getFcmToken()
        if (token.isNotBlank()) {
            val contact = Device.createDevice(
                deviceId = configRepository.getDeviceId(),
                externalUserId = configRepository.getExternalId(),
                pushToken = token
            )

            contactRepository.sendDeviceProperties(contact, object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                }

            })
        }
    }

    companion object {
        val TAG: String = ContactController::class.java.simpleName
    }
}