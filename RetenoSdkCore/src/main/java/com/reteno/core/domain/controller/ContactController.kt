package com.reteno.core.domain.controller

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    @Volatile
    private var isDeviceSentThisSession = false

    fun getDeviceId(): String {
        return configRepository.getDeviceId().id
    }

    fun setExternalUserId(id: String?) {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/

        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.externalId != id) {
            isDeviceSentThisSession = true
            configRepository.setExternalUserId(id)
            configRepository.getFcmToken {
                onNewContact(it, toParallelWork = false)
            }
        }
    }

    fun setUserData(user: User?) {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/

        user?.let { userTmp ->
            Validator.validateUser(userTmp)?.let {
                contactRepository.saveUserData(it, toParallelWork = false)
            } ?: Logger.captureMessage("ContactController.setUserData(): user = [$userTmp]")
        }
    }

    fun setAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        /*@formatter:off*/ Logger.i(TAG, "setAnonymousUserAttributes(): ", "attributes = [", attributes, "]")
        /*@formatter:on*/

        val validAttributes: UserAttributesAnonymous? =
            Validator.validateAnonymousUserAttributes(attributes)
        validAttributes?.let {
            val userData = User(it.toUserAttributes())
            contactRepository.saveUserData(userData)
        } ?: Logger.captureMessage("setAnonymousUserAttributes(): attributes = [$attributes]")
    }

    fun onNewFcmToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "newToken = [" , token , "]")
        /*@formatter:on*/
        isDeviceSentThisSession = true
        configRepository.getFcmToken { oldToken ->
            token.takeIf { it != oldToken }?.let {
                configRepository.saveFcmToken(it)
            }
            onNewContact(token, toParallelWork = false)
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

    suspend fun checkIfDeviceRegistered() {
        /*@formatter:off*/ Logger.i(TAG, "checkIfDeviceRegistered(): ")
        /*@formatter:on*/
        if (!configRepository.isDeviceRegistered()) {
            isDeviceSentThisSession = true
            withContext(Dispatchers.IO) {
                configRepository.awaitForDeviceId()
            }
            configRepository.getFcmToken {
                onNewContact(it, toParallelWork = false)
            }
        }
    }

    fun checkIfDeviceRequestSentThisSession() {
        /*@formatter:off*/ Logger.i(TAG, "checkIfDeviceRequestSentThisSession(): ", "isDeviceSentThisSession = [" , isDeviceSentThisSession , "]")
        /*@formatter:on*/
        if (isDeviceSentThisSession.not()) {
            contactRepository.deleteSynchedDevices()
            configRepository.getFcmToken {
                onNewContact(it, toParallelWork = false)
            }
            isDeviceSentThisSession = true
        }
    }

    fun notificationsEnabled(notificationsEnabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "notificationsEnabled(): ", "notificationsEnabled = [" , notificationsEnabled , "]")
        /*@formatter:on*/
        val currentState = configRepository.isNotificationsEnabled()
        if (notificationsEnabled != currentState) {
            isDeviceSentThisSession = true
            configRepository.saveNotificationsEnabled(notificationsEnabled)
            configRepository.getFcmToken {
                onNewContact(it, notificationsEnabled = notificationsEnabled)
            }
        }
    }

    private fun onNewContact(
        fcmToken: String,
        notificationsEnabled: Boolean? = null,
        toParallelWork: Boolean = true
    ) {
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
                pushSubscribed = notificationsEnabled ?: configRepository.isNotificationsEnabled()
            )
            contactRepository.saveDeviceData(contact, toParallelWork)
        }
    }

    fun setExternalIdAndUserData(externalUserId: String, user: User?) {
        setExternalUserId(externalUserId)
        setUserData(user)
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