package com.reteno.core.domain.controller

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.Validator
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

    fun onNewFcmToken(token: String) {
        val oldToken = configRepository.getFcmToken()
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "oldToken = [" , oldToken , "], newToken = [" , token , "]")
        /*@formatter:on*/
        if (token != oldToken) {
            configRepository.saveFcmToken(token)
            onNewContact()
        }
    }

    fun setUserData(user: User) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/

        val validUser = Validator.validateUser(user)
        validUser?.let(contactRepository::saveUserData) ?: Logger.e(TAG, "setUserData(): user = [$user]")
    }

    private fun onNewContact() {
        onNewContactInternal()
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

    fun notificationsEnabled(enabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "notificationsEnabled(): ", "enabled = [" , enabled , "]")
        /*@formatter:on*/
        val currentState = configRepository.getNotificationsEnabled()
        if (enabled != currentState) {
            configRepository.saveNotificationsEnabled(enabled)
            onNewContactInternal(enabled)
        }
    }

    private fun onNewContactInternal(enabled: Boolean? = null) {
        val token = configRepository.getFcmToken()
        if (token.isNotBlank()) {
            val deviceId = configRepository.getDeviceId()
            val contact = Device.createDevice(
                deviceId = deviceId.id,
                externalUserId = deviceId.externalId,
                pushToken = token,
                pushSubscribed = enabled
            )

            contactRepository.saveDeviceData(contact)
        }
    }

    companion object {
        private val TAG: String = ContactController::class.java.simpleName
    }
}