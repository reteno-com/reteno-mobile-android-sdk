package com.reteno.core.domain.controller

import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.util.Logger

class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    fun setExternalUserId(id: String) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/
        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.externalId != id) {
            configRepository.setExternalUserId(id)
            onNewContact()
        }
    }

    fun setDeviceIdMode(deviceIdMode: DeviceIdMode, onDeviceIdChanged: () -> Unit) {
        /*@formatter:off*/ Logger.i(TAG, "changeDeviceIdMode(): ", "deviceIdMode = [" , deviceIdMode , "]")
        /*@formatter:on*/
        val oldDeviceId = configRepository.getDeviceId()
        configRepository.setDeviceIdMode(deviceIdMode) {
            if (oldDeviceId.id != it.id) {
                onNewContact()
                onDeviceIdChanged.invoke()
            }
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

        contactRepository.saveUserData(used)
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

            contactRepository.saveDeviceData(contact)
        }
    }

    fun pushDeviceData() {
        /*@formatter:off*/ Logger.i(TAG, "pushDeviceData(): ", "")
        /*@formatter:on*/
        contactRepository.pushDeviceData()
    }

    fun pushUserData() {
        /*@formatter:off*/ Logger.i(TAG, "pushUserData(): ", "")
        /*@formatter:on*/
        contactRepository.pushUserData()
    }

    companion object {
        private val TAG: String = ContactController::class.java.simpleName
    }
}