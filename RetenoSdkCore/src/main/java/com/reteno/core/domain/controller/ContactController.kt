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

    fun setExternalUserId(id: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/

        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.externalId != id) {
            configRepository.setExternalUserId(id)
            val fcmToken = configRepository.getFcmToken()
            onNewContact(fcmToken)
        }
    }

    fun setUserData(user: User?) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/

        user?.let {
            val validUser = Validator.validateUser(it)
            validUser?.let(contactRepository::saveUserData) ?: Logger.captureMessage("ContactController.setUserData(): user = [$it]")
        }
    }

    fun onNewFcmToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "newToken = [" , token , "]")
        /*@formatter:on*/
        configRepository.saveFcmToken(token)
        onNewContact(token)
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

    fun checkIfDeviceRegistered() {
        /*@formatter:off*/ Logger.i(TAG, "checkIfDeviceRegistered(): ")
        /*@formatter:on*/

        if (!configRepository.isDeviceRegistered()) {
            val fcmToken = configRepository.getFcmToken()
            onNewContact(fcmToken)
        }
    }

    fun notificationsEnabled(notificationsEnabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "notificationsEnabled(): ", "notificationsEnabled = [" , notificationsEnabled , "]")
        /*@formatter:on*/
        val currentState = configRepository.isNotificationsEnabled()
        if (notificationsEnabled != currentState) {
            configRepository.saveNotificationsEnabled(notificationsEnabled)
            val fcmToken = configRepository.getFcmToken()
            onNewContact(fcmToken, notificationsEnabled)
        }
    }

    private fun onNewContact(fcmToken: String, notificationsEnabled: Boolean? = null) {
        /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "fcmToken = [", fcmToken, "], notificationsEnabled = [", notificationsEnabled, "]")
        /*@formatter:on*/
        if (fcmToken.isNotEmpty()) {
            /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "token AVAILABLE")
            /*@formatter:on*/
            val deviceId = configRepository.getDeviceId()
            val contact = Device.createDevice(
                deviceId = deviceId.id,
                externalUserId = deviceId.externalId,
                pushToken = fcmToken,
                pushSubscribed = notificationsEnabled
            )

            contactRepository.saveDeviceData(contact)
        }
    }

    companion object {
        private val TAG: String = ContactController::class.java.simpleName
    }
}