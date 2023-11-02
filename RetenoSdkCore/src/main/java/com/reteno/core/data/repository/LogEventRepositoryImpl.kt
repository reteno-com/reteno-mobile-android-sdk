package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.logevent.RetenoLogEventListRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.util.Logger
import com.reteno.core.util.isNonRepeatableError

internal class LogEventRepositoryImpl(
    private val databaseManager: RetenoDatabaseManagerLogEvent,
    private val apiClient: ApiClient
) : LogEventRepository {

    override fun saveLogEvent(logEvent: RetenoLogEvent) {
        /*@formatter:off*/ Logger.i(TAG, "saveLogEvent(): ", "logEvent = [" , logEvent , "]")
        /*@formatter:on*/
        OperationQueue.addParallelOperation {
            val logEventDb = logEvent.toDb()
            databaseManager.insertLogEvent(logEventDb)
            pushLogEvents()
        }
    }

    override fun pushLogEvents(limit: Int?) {
        /*@formatter:off*/ Logger.i(TAG, "pushLogEvents(): ", "limit = [", limit, "]")
        /*@formatter:on*/
        val logEvents: List<RetenoLogEventDb> = databaseManager.getLogEvents(limit)

        if (logEvents.isEmpty()) {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushLogEvents(): ", "logEvents = [" , logEvents , "]")
        /*@formatter:on*/

        val logEventList = RetenoLogEventListRemote(logEvents.map { it.toRemote() })

        apiClient.post(
            ApiContract.LogEvent.Events,
            logEventList.toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteLogEvents(logEvents)
                    PushOperationQueue.nextOperation()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteLogEvents(logEvents)
                        PushOperationQueue.nextOperation()
                    } else {
                        PushOperationQueue.removeAllOperations()
                    }
                }

            }
        )
    }

    companion object {
        private val TAG = AppInboxRepositoryImpl::class.java.simpleName
    }
}