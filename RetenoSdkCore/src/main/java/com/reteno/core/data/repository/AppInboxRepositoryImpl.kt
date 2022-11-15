package com.reteno.core.data.repository

import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE
import com.reteno.core.data.remote.api.ApiContract.AppInbox.Companion.QUERY_PAGE_SIZE
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toDomain
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.inbox.InboxMessagesCountRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.isNonRepeatableError
import java.time.ZonedDateTime

class AppInboxRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManager,
) : AppInboxRepository {

    // TODO temp
    private val listIds = ArrayDeque<String>()

    override fun saveMessageOpened(messageId: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveMessageOpened(): ", "messageId = [" , messageId , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            // TODO add to db
            listIds.add(messageId)
            pushMessagesStatus()
        }
    }

    override fun saveAllMessageOpened() {
        /*@formatter:off*/ Logger.i(TAG, "saveAllMessageOpened(): ", "")
        /*@formatter:on*/
    }

    override fun getMessages(
        page: Int?,
        pageSize: Int?,
        resultCallback: AppInboxMessagesCallback
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
                    resultCallback.onSuccess(inBoxMessages.messages, inBoxMessages.totalPages)
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

    override fun getMessagesCount(resultCallback: AppInboxMessagesCountCallback) {
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
        val messagesIds: List<String> = listIds.toList().takeIf { it.isNotEmpty() } ?: kotlin.run { // TODO add db
            PushOperationQueue.nextOperation()
            return
        }

        apiClient.post(
            ApiContract.AppInbox.MessagesStatus,
            messagesIds.toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    listIds.clear() // TODO databaseManager.deleteMessagesStatus(messagesIds.size())
                    pushMessagesStatus()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        listIds.clear() // TODO databaseManager.deleteMessagesStatus(messagesIds.size())
                        pushMessagesStatus()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            }
        )
    }

    override fun clearOldMessages(outdatedTime: ZonedDateTime) {
       // TODO("Not yet implemented")
    }

    companion object {
        private val TAG = AppInboxRepositoryImpl::class.java.simpleName
    }
}