package com.reteno.core.domain.action.user

import com.reteno.core.RetenoInternalImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.user.User
import com.reteno.core.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class UpdateUserAttributesAction(
    private val serviceLocator: ServiceLocator,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val userAttributesUpdateRequests = Channel<Pair<String, User?>>(
        capacity = Channel.UNLIMITED,
        onUndeliveredElement = { Logger.w(TAG, "onUndeliveredElement", "Too much user attributes update requested, ignoring update: id = ${it.first}, user = ${it.second}") }
    )
    private val segmentationRefreshRequest = Channel<Unit>(
        capacity = Channel.UNLIMITED,
        onUndeliveredElement = { Logger.w(TAG, "onUndeliveredElement", "Too much user segment update requested, ignoring") }
    )
    private val mutex = Mutex()
    private val contactController by lazy(LazyThreadSafetyMode.NONE) { serviceLocator.contactControllerProvider.get() }
    private val sessionHandler by lazy(LazyThreadSafetyMode.NONE) { serviceLocator.retenoSessionHandlerProvider.get() }
    private val iamController by lazy(LazyThreadSafetyMode.NONE) { serviceLocator.iamControllerProvider.get() }

    fun start(scope: CoroutineScope) {
        userAttributesUpdateRequests
            .receiveAsFlow()
            .onEach { executeRequest(it.first, it.second) }
            .retry {
                Logger.e(TAG, "setUserAttributes():", it)
                true
            }
            .catch { Logger.e(TAG, "setUserAttributes():", it) }
            .launchIn(scope)
        segmentationRefreshRequest
            .receiveAsFlow()
            .debounce(5000L)
            .onEach { iamController.refreshSegmentation() }
            .retry {
                Logger.e(TAG, "refreshSegmentation():", it)
                true
            }
            .catch { Logger.e(TAG, "refreshSegmentation():", it) }
            .launchIn(scope)
    }

    fun postUpdateRequest(externalUserId: String, user: User?) {
        userAttributesUpdateRequests.trySend(externalUserId to user)
    }

    private suspend fun executeRequest(externalUserId: String, user: User?) = mutex.withLock {
        if (contactController.getDeviceIdSuffix() != null) {
            RetenoInternalImpl.instance.stop()
            withContext(ioDispatcher) {
                sessionHandler.clearSessionForced()
                contactController.setDeviceIdSuffix(null)
                contactController.setExternalIdAndUserData(externalUserId, user)
            }
            RetenoInternalImpl.instance.start()
        } else {
            contactController.setExternalIdAndUserData(externalUserId, user)
        }
        if (RetenoInternalImpl.instance.isStarted) {
            segmentationRefreshRequest.send(Unit)
        }
    }

    companion object {
        private const val TAG = "UserAttributesUpdateAction"
    }
}