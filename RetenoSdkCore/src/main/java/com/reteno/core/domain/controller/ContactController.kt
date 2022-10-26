package com.reteno.core.domain.controller

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.model.user.User
import com.reteno.core.util.Logger

class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    fun setExternalUserId(id: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/
        configRepository.setExternalUserId(id)
        onNewContact()
    }

    fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: () -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        configRepository.setDeviceIdMode(deviceIdMode) {
            onNewContact()
            onDeviceIdChanged.invoke()
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

    fun setUserData(used: User) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "used = [" , used , "]") 
        /*@formatter:on*/

        contactRepository.sendUserData(used, object : ResponseCallback {
            override fun onSuccess(response: String) {
                /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                /*@formatter:on*/
                // TODO add saving to db
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                /*@formatter:on*/
                // TODO handle error retry strategy(if needed)
            }

        })
    }

    private fun onNewContact() {
        val token = configRepository.getFcmToken()
        if (token.isNotBlank()) {
            val deviceId = configRepository.getDeviceId()
            val contact = Device.createDevice(
                deviceId = deviceId.id,
                externalUserId = deviceId.externalId,
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