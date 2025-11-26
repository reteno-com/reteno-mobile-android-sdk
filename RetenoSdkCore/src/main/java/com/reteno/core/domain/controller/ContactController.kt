package com.reteno.core.domain.controller

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.util.Logger
import java.util.concurrent.atomic.AtomicBoolean


class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    //This session means app opening session, not the SDK Session
    private var isDeviceSentThisSession = AtomicBoolean(false)

    fun getDeviceId(): String {
        return configRepository.getDeviceId().id
    }

    fun getDeviceIdSuffix(): String? {
        return configRepository.getDeviceId().idSuffix
    }

    suspend fun awaitDeviceId(): String {
        return configRepository.awaitForDeviceId().id
    }

    suspend fun setExternalUserId(id: String?, pushContact: Boolean = true): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/

        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.externalId != id) {
            configRepository.setExternalUserId(id)
            if (pushContact) {
                isDeviceSentThisSession.set(true)
                val token = configRepository.getFcmToken()
                onNewContactDeprecated(token, toParallelWork = false)
            }
            return true
        }
        return false
    }

    fun setDeviceIdSuffix(suffix: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setDeviceIdSuffix(): ", "suffix = [" , suffix , "]")
        /*@formatter:on*/
        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.idSuffix != suffix) {
            isDeviceSentThisSession.set(false)
            configRepository.setDeviceIdSuffix(suffix)
        }
    }

    suspend fun setUserData(user: User?): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/
        if (user == null) return false
        val validatedUser = Validator.validateUser(user)
        if (validatedUser == null) {
            Logger.captureMessage("ContactController.setUserData(): user = [$user]")
            return false
        }
        contactRepository.saveUserData(validatedUser)
        configRepository.setUserEmail(validatedUser.userAttributes?.email)
        configRepository.setUserPhone(validatedUser.userAttributes?.phone)
        return true
    }

    fun setAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        /*@formatter:off*/ Logger.i(TAG, "setAnonymousUserAttributes(): ", "attributes = [", attributes, "]")
        /*@formatter:on*/

        val validAttributes: UserAttributesAnonymous? =
            Validator.validateAnonymousUserAttributes(attributes)
        validAttributes?.let {
            val userData = User(it.toUserAttributes())
            contactRepository.saveUserDataDeprecated(userData, toParallelWork = false)
        } ?: Logger.captureMessage("setAnonymousUserAttributes(): attributes = [$attributes]")
    }

    suspend fun onNewFcmToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "newToken = [" , token , "]")
        /*@formatter:on*/
        val oldToken = configRepository.getFcmToken()
        if (token != oldToken) {
            configRepository.saveFcmToken(token)
            onNewContactDeprecated(token, toParallelWork = false)
            isDeviceSentThisSession.set(true)
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

    suspend fun registerDevice() {
        /*@formatter:off*/ Logger.i(TAG, "registerDevice(): ")
        /*@formatter:on*/
        val token = configRepository.getFcmToken()
        onNewContact(token)
        contactRepository.pushDeviceDataImmediate()
        isDeviceSentThisSession.set(true)
    }

    suspend fun checkIfDeviceRequestSentThisSession() {
        /*@formatter:off*/ Logger.i(TAG, "checkIfDeviceRequestSentThisSession(): ", "isDeviceSentThisSession = [" , isDeviceSentThisSession , "]")
        /*@formatter:on*/
        if (isDeviceSentThisSession.get().not()) {
            contactRepository.deleteSynchedDevices()
            val token = configRepository.getFcmToken()
            onNewContactDeprecated(token, toParallelWork = false, pushImmediate = true)
            isDeviceSentThisSession.set(true)
        }
    }

    suspend fun notificationsEnabled(notificationsEnabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "notificationsEnabled(): ", "notificationsEnabled = [" , notificationsEnabled , "]")
        /*@formatter:on*/
        val currentState = configRepository.isNotificationsEnabled()
        if (notificationsEnabled != currentState) {
            isDeviceSentThisSession.set(true)
            configRepository.saveNotificationsEnabled(notificationsEnabled)
            val token = configRepository.getFcmToken()
            onNewContactDeprecated(token, notificationsEnabled = notificationsEnabled, toParallelWork = false)
        }
    }

    private suspend fun onNewContact(
        fcmToken: String,
        notificationsEnabled: Boolean? = null,
    ) {
        /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "fcmToken = [", fcmToken, "], notificationsEnabled = [", notificationsEnabled, "]")
        /*@formatter:on*/
        if (fcmToken.isNotEmpty()) {
            /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "token AVAILABLE")
            /*@formatter:on*/
        }
        val deviceId = configRepository.getDeviceId()
        val contact = Device.createDevice(
            deviceId = deviceId.id,
            deviceIdSuffix = deviceId.idSuffix,
            externalUserId = deviceId.externalId,
            pushToken = fcmToken,
            email = deviceId.email,
            phone = deviceId.phone,
            pushSubscribed = notificationsEnabled ?: configRepository.isNotificationsEnabled()
        )
        contactRepository.saveDeviceData(contact)
    }

    private fun onNewContactDeprecated(
        fcmToken: String,
        notificationsEnabled: Boolean? = null,
        toParallelWork: Boolean = false,
        pushImmediate: Boolean = false
    ) {
        /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "fcmToken = [", fcmToken, "], notificationsEnabled = [", notificationsEnabled, "]")
        /*@formatter:on*/
        if (fcmToken.isNotEmpty()) {
            /*@formatter:off*/ Logger.i(TAG, "onNewContact(): ", "token AVAILABLE")
            /*@formatter:on*/
        }
        val deviceId = configRepository.getDeviceId()
        val contact = Device.createDevice(
            deviceId = deviceId.id,
            deviceIdSuffix = deviceId.idSuffix,
            externalUserId = deviceId.externalId,
            pushToken = fcmToken,
            email = deviceId.email,
            phone = deviceId.phone,
            pushSubscribed = notificationsEnabled ?: configRepository.isNotificationsEnabled()
        )
        if (pushImmediate) {
            contactRepository.saveDeviceDataImmediate(contact)
            contactRepository.pushDeviceData()
        } else {
            contactRepository.saveDeviceData(contact, toParallelWork)
        }
    }

    suspend fun setExternalIdAndUserData(externalUserId: String, user: User?) {
        setExternalUserId(externalUserId, pushContact = false)
        setUserData(user)
        val token = configRepository.getFcmToken()
        onNewContact(token)
        contactRepository.pushDeviceDataImmediate()
        isDeviceSentThisSession.set(true)
        contactRepository.pushUserDataImmediate()
    }

    fun saveDefaultNotificationChannel(channel: String) {
        configRepository.saveDefaultNotificationChannel(channel)
    }

    fun getDefaultNotificationChannel(): String {
        return configRepository.getDefaultNotificationChannel()
    }

    companion object {
        private val TAG: String = ContactController::class.java.simpleName
    }
}