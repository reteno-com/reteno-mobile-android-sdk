package com.reteno.core.internal.action.user

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.user.User
import com.reteno.core.internal.action.device.ContactSession
import com.reteno.core.internal.action.device.CurrentDevice
import com.reteno.core.internal.action.device.DeviceLanguage
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.core.internal.action.sync.SyncTrigger
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.lifecycle.SdkLifecycle
import com.reteno.core.internal.log.Redact
import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlinx.coroutines.withContext

internal class ContactWriter(
    private val configRepository: ConfigRepository,
    private val usersDb: RetenoDatabaseManagerUser,
    private val currentDevice: CurrentDevice,
    private val deviceLanguage: DeviceLanguage,
    private val sync: SyncTrigger,
    private val session: ContactSession,
    private val sdk: SdkLifecycle,
    private val sessionHandler: RetenoSessionHandler,
    private val remoteLog: RemoteLog,
    private val dispatchers: RetenoDispatchers
) {

    suspend fun write(trace: ActionTrace, suffix: String?, externalUserId: String, user: User?) {
        if (configRepository.getDeviceId().idSuffix == suffix) {
            writeContact(trace, externalUserId, user)
        } else {
            switchAccount(trace, suffix, externalUserId, user)
        }
    }

    private suspend fun switchAccount(trace: ActionTrace, suffix: String?, externalUserId: String, user: User?) {
        trace.step("stop sdk for account switch") {
            withContext(dispatchers.main) { sdk.stop() }
        }
        trace.step("clear current session") {
            withContext(dispatchers.io) { sessionHandler.clearSessionForced() }
        }
        trace.step("save device id suffix to local settings") {
            withContext(dispatchers.io) { configRepository.setDeviceIdSuffix(suffix) }
            session.markDeviceNotSent()
        }
        writeContact(trace, externalUserId, user)
        trace.step("start sdk after account switch") {
            withContext(dispatchers.main) { sdk.start() }
        }
    }

    private suspend fun writeContact(trace: ActionTrace, externalUserId: String, user: User?) {
        trace.step(
            "save external user id to local settings",
            detail = { changed -> if (changed) "changed" else "unchanged" }
        ) {
            val changed = configRepository.getDeviceId().externalId != externalUserId
            if (changed) withContext(dispatchers.io) { configRepository.setExternalUserId(externalUserId) }
            changed
        }
        if (user != null) {
            saveUser(trace, user)
        }
        val token = trace.step("read push token", detail = { Redact.token(it) }) {
            configRepository.getFcmToken()
        }
        currentDevice.save(trace, token)
        session.markDeviceSent()
        trace.step("send device and user to server") {
            sync.syncNow(SyncScope.CONTACT)
        }
    }

    private suspend fun saveUser(trace: ActionTrace, user: User) {
        val validated = Validator.validateUser(trace.withLanguageCodeToKeep(user))
        if (validated == null) {
            trace.step("report rejected user to remote log") {
                remoteLog.info("$ACTION_NAME: user rejected by validation")
            }
            return
        }
        trace.step("save user to local database") {
            withContext(dispatchers.io) {
                usersDb.insertUser(validated.toDb(configRepository.getDeviceId()))
                configRepository.setUserEmail(validated.userAttributes?.email)
                configRepository.setUserPhone(validated.userAttributes?.phone)
            }
        }
        val languageCode = validated.userAttributes?.languageCode
        if (languageCode != null) {
            deviceLanguage.useExplicit(trace, languageCode)
        }
    }

    private fun ActionTrace.withLanguageCodeToKeep(user: User): User {
        val attributes = user.userAttributes ?: return user
        val languageCode = languageCodeToKeep(attributes.languageCode, remoteLog, ACTION_NAME)
        return user.copy(userAttributes = attributes.copy(languageCode = languageCode))
    }
}

private const val ACTION_NAME = "SetUserAttributes"
