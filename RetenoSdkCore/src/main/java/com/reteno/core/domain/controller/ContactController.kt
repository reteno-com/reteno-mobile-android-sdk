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
import java.util.concurrent.atomic.AtomicBoolean


class ContactController(
    private val contactRepository: ContactRepository,
    private val configRepository: ConfigRepository
) {

    private var isDeviceSentThisSession = AtomicBoolean(false)

    fun getDeviceId(): String {
        return configRepository.getDeviceId().id
    }

    suspend fun awaitDeviceId(): String {
        return configRepository.awaitForDeviceId().id
    }

    fun setExternalUserId(id: String?, pushContact: Boolean = true): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "setExternalUserId(): ", "id = [" , id , "]")
        /*@formatter:on*/

        val oldDeviceId = configRepository.getDeviceId()
        if (oldDeviceId.externalId != id) {
            isDeviceSentThisSession.set(true)
            configRepository.setExternalUserId(id)
            if (pushContact) {
                configRepository.getFcmToken {
                    onNewContact(it, toParallelWork = false)
                }
            }
            return true
        }
        return false
    }

    fun setUserData(user: User?): Boolean {
        /*@formatter:off*/ Logger.i(TAG, "setUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/

        user?.let { userTmp ->
            Validator.validateUser(userTmp)?.let {
                contactRepository.saveUserData(it, toParallelWork = false)
                configRepository.setUserEmail(it.userAttributes?.email)
                configRepository.setUserPhone(it.userAttributes?.phone)
                return true
            } ?: Logger.captureMessage("ContactController.setUserData(): user = [$userTmp]")
        }
        return false
    }

    fun setAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        /*@formatter:off*/ Logger.i(TAG, "setAnonymousUserAttributes(): ", "attributes = [", attributes, "]")
        /*@formatter:on*/

        val validAttributes: UserAttributesAnonymous? =
            Validator.validateAnonymousUserAttributes(attributes)
        validAttributes?.let {
            val userData = User(it.toUserAttributes())
            contactRepository.saveUserData(userData, toParallelWork = false)
        } ?: Logger.captureMessage("setAnonymousUserAttributes(): attributes = [$attributes]")
    }

    fun onNewFcmToken(token: String) {
        /*@formatter:off*/ Logger.i(TAG, "onNewFcmToken(): ", "newToken = [" , token , "]")
        /*@formatter:on*/
        isDeviceSentThisSession.set(true)
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
            isDeviceSentThisSession.set(true)
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
        if (isDeviceSentThisSession.get().not()) {
            contactRepository.deleteSynchedDevices()
            configRepository.getFcmToken {
                onNewContact(it, toParallelWork = false)
            }
            isDeviceSentThisSession.set(true)
        }
    }

    fun notificationsEnabled(notificationsEnabled: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "notificationsEnabled(): ", "notificationsEnabled = [" , notificationsEnabled , "]")
        /*@formatter:on*/
        val currentState = configRepository.isNotificationsEnabled()
        if (notificationsEnabled != currentState) {
            isDeviceSentThisSession.set(true)
            configRepository.saveNotificationsEnabled(notificationsEnabled)
            configRepository.getFcmToken {
                onNewContact(it, notificationsEnabled = notificationsEnabled)
            }
        }
    }

    private fun onNewContact(
        fcmToken: String,
        notificationsEnabled: Boolean? = null,
        toParallelWork: Boolean = false
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
            externalUserId = deviceId.externalId,
            pushToken = fcmToken,
            email = deviceId.email,
            phone = deviceId.phone,
            pushSubscribed = notificationsEnabled ?: configRepository.isNotificationsEnabled()
        )
        contactRepository.saveDeviceData(contact, toParallelWork)
    }

    fun setExternalIdAndUserData(externalUserId: String, user: User?) {
        setExternalUserId(externalUserId, pushContact = false)
        setUserData(user)
        configRepository.getFcmToken { token ->
            onNewContact(token, toParallelWork = false)
        }
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