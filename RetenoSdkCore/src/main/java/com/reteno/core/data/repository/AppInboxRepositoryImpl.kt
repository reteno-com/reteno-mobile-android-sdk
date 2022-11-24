package com.reteno.core.data.repository

import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE_SIZE
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toDomain
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.appinbox.AppInboxMessagesStatusRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesCountRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.util.Logger
import com.reteno.core.util.RetenoThreadFactory
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import java.lang.ref.WeakReference
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AppInboxRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManager,
    private val configRepository: ConfigRepository
) : AppInboxRepository {

    private val listeners: MutableMap<RetenoResultCallback<Int>, RetenoResultCallback<Int>> =
        Collections.synchronizedMap(WeakHashMap())

    private var scheduler: ScheduledExecutorService? = null
    private var isPollingActive = false

    private var lastCountValue: Int? = null


    override fun saveMessageOpened(messageId: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveMessageOpened(): ", "messageId = [" , messageId , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val inbox = AppInboxMessageDb(
                id = messageId,
                deviceId = configRepository.getDeviceId().id
            )
            databaseManager.insertAppInboxMessage(inbox)
            pushMessagesStatus()
        }
    }

    override fun setAllMessageOpened(callback: RetenoResultCallback<Unit>) {
        /*@formatter:off*/ Logger.i(TAG, "saveAllMessageOpened(): ", "")
        /*@formatter:on*/
        val status = AppInboxMessagesStatusRemote()
        apiClient.post(
            ApiContract.AppInbox.MessagesStatus,
            status.toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteAllAppInboxMessages()
                    fetchCount()
                    OperationQueue.addUiOperation {
                        callback.onSuccess(Unit)
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    OperationQueue.addUiOperation {
                        callback.onFailure(statusCode, response, throwable)
                    }
                }

            }
        )
    }

    override fun getMessages(
        page: Int?,
        pageSize: Int?,
        resultCallback: RetenoResultCallback<AppInboxMessages>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getMessages(): ", "page = [" , page , "], pageSize = [" , pageSize , "], resultCallback = [" , resultCallback , "]")
        /*@formatter:on*/
        val queryParams = mapOf(
            QUERY_PAGE to page?.toString(),
            QUERY_PAGE_SIZE to pageSize?.toString()
        )

        apiClient.get(ApiContract.AppInbox.Messages, queryParams, object : ResponseCallback {
            override fun onSuccess(response: String) {
                /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                /*@formatter:on*/
                val inBoxMessages = response.fromJson<InboxMessagesRemote>().toDomain()
                OperationQueue.addUiOperation {
                    resultCallback.onSuccess(inBoxMessages)
                }
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                /*@formatter:on*/
                OperationQueue.addUiOperation {
                    resultCallback.onFailure(statusCode, response, throwable)
                }
            }

        })
    }

    override fun getMessagesCount(resultCallback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "getMessagesCount(): ", "resultCallback = [" , resultCallback , "]")
        /*@formatter:on*/
        apiClient.get(ApiContract.AppInbox.MessagesCount, null, object : ResponseCallback {
            override fun onSuccess(response: String) {
                /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                /*@formatter:on*/
                val count = response.fromJson<InboxMessagesCountRemote>().unreadCount

                OperationQueue.addUiOperation {
                    resultCallback.onSuccess(count)
                }
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                /*@formatter:on*/
                OperationQueue.addUiOperation {
                    resultCallback.onFailure(statusCode, response, throwable)
                }
            }

        })
    }

    override fun pushMessagesStatus() {
        /*@formatter:off*/ Logger.i(TAG, "pushMessagesStatus(): ", "")
        /*@formatter:on*/
        val messagesIds: List<String> = databaseManager.getAppInboxMessages()
            .map { it.id }
            .takeIf { it.isNotEmpty() } ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }

        val messagesStatus = AppInboxMessagesStatusRemote(ids = messagesIds)

        apiClient.post(
            ApiContract.AppInbox.MessagesStatus,
            messagesStatus.toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteAppInboxMessages(messagesIds.size)
                    fetchCount()
                    pushMessagesStatus()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteAppInboxMessages(messagesIds.size)
                        pushMessagesStatus()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            }
        )
    }

    override fun clearOldMessages(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldMessages(): ", "outdatedTime = [" , outdatedTime , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedInboxCount = databaseManager.deleteAppInboxMessagesByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldMessages(): ", "removedInboxCount = [" , removedInboxCount , "]")
            /*@formatter:on*/
            if (removedInboxCount > 0) {
                val msg = "Outdated Inbox: - $removedInboxCount"
                Logger.captureEvent(msg)
            }
        }
    }

    override fun subscribeOnMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "subscribeOnMessagesCountChanged(): ", "callback = [" , callback , "], listenerSet.size = [", listeners.size, "]")
        /*@formatter:on*/
        synchronized(listeners) {
            listeners[callback] = callback // TODO need test new key
            startPolling()
        }
    }

    override fun unsubscribeMessagesCountChanged(callback: RetenoResultCallback<Int>) {
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeMessagesCountChanged(): ", "callback = [" , callback , "], listenerSet.size = [", listeners.size, "]")
        /*@formatter:on*/
        synchronized(listeners) {
            listeners.remove(callback)
            if (listeners.isEmpty()) {
                stopPolling()
            }
        }
    }

    override fun unsubscribeAllMessagesCountChanged() {
        /*@formatter:off*/ Logger.i(TAG, "unsubscribeAllMessagesCountChanged(): ", "listenerSet.size = [", listeners.size, "]")
        /*@formatter:on*/
        synchronized(listeners) {
            listeners.clear()
        }
        stopPolling()
    }

    private fun startPolling() {
        /*@formatter:off*/ Logger.i(TAG, "startPolling(): ", "isPollingActive = [", isPollingActive, "]")
        /*@formatter:on*/
        if (isPollingActive) return
        isPollingActive = true

        scheduler?.shutdownNow()
        scheduler = Executors.newScheduledThreadPool(1, RetenoThreadFactory())
        scheduler?.scheduleAtFixedRate(
            ::fetchCount,
            INITIAL_DELAY,
            REGULAR_DELAY,
            TimeUnit.MILLISECONDS
        )
    }

    private fun fetchCount() {
        /*@formatter:off*/ Logger.i(TAG, "fetchCount(): ", "listenerSet.size = [", listeners.size, "]")
        /*@formatter:on*/

        if (listeners.isEmpty() || !isPollingActive) {
            stopPolling()
            return
        }

        getMessagesCount(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {
                notifyOnSuccess(result)
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                notifyOnFailure(statusCode, response, throwable)
            }

        })
    }

    private fun notifyOnSuccess(count: Int) {
        /*@formatter:off*/ Logger.i(TAG, "notifyOnSuccess(): ", "count = [" , count , "], lastCountValue = [", lastCountValue, "]")
        /*@formatter:on*/
        synchronized(listeners) {
            if (lastCountValue == count) return
            lastCountValue = count
            listeners.values.forEach { listener ->
                listener.onSuccess(count)
            }
        }
    }

    private fun notifyOnFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
        /*@formatter:off*/ Logger.i(TAG, "notifyOnFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
        /*@formatter:on*/
        synchronized(listeners) {
            listeners.values.forEach { listener ->
                listener.onFailure(statusCode, response, throwable)
            }
        }
    }


    private fun stopPolling() {
        scheduler?.shutdownNow()
        isPollingActive = false
        scheduler = null
        lastCountValue = null
    }

    companion object {
        private val TAG = AppInboxRepositoryImpl::class.java.simpleName
        private const val REGULAR_DELAY = 30_000L
        private const val INITIAL_DELAY = 0L
    }
}