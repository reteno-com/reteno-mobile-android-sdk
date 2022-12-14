package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import java.time.ZonedDateTime

internal class EventsRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManagerEvents,
    private val configRepository: ConfigRepository
) : EventsRepository {

    override fun saveEvent(event: Event) {
        /*@formatter:off*/ Logger.i(TAG, "saveEvent(): ", "event = [" , event , "]")
        /*@formatter:on*/

        val deviceId = configRepository.getDeviceId()
        val events = EventsDb(
            deviceId = deviceId.id,
            externalUserId = deviceId.externalId,
            eventList = listOf(event.toDb())
        )
        OperationQueue.addOperation {
            try {
                databaseManager.insertEvents(events)
            } catch (e: Exception) {
                Logger.e(TAG, "saveEvent()", e)
            }
        }
    }

    override fun saveEcomEvent(ecomEvent: EcomEvent) {
        /*@formatter:off*/ Logger.i(TAG, "saveEcomEvent(): ", "ecomEvent = [" , ecomEvent , "]")
        /*@formatter:on*/

        val deviceId = configRepository.getDeviceId()
        val events = EventsDb(
            deviceId = deviceId.id,
            externalUserId = deviceId.externalId,
            eventList = listOf(ecomEvent.toDb())
        )
        OperationQueue.addOperation {
            try {
                databaseManager.insertEvents(events)
            } catch (e: Exception) {
                Logger.e(TAG, "saveEcomEvent()", e)
            }
        }
    }

    override fun pushEvents(limit: Int?) {
        /*@formatter:off*/ Logger.i(TAG, "pushEvents(): ", "limit = [", limit, "]")
        /*@formatter:on*/

        val events: EventsDb = databaseManager.getEvents(limit).firstOrNull() ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }
        if (events.eventList.isEmpty()) return
        /*@formatter:off*/ Logger.i(TAG, "pushEvents(): ", "events = [" , events , "]")
        /*@formatter:on*/

        val eventListSize = events.eventList.size
        apiClient.post(
            ApiContract.MobileApi.Events,
            events.toRemote().toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteEvents(eventListSize)
                    pushEvents()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteEvents(eventListSize)
                        pushEvents()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            }
        )
    }

    override fun clearOldEvents(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "outdatedTime = [" , outdatedTime , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedEventsCount = databaseManager.deleteEventsByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "removedEventsCount = [" , removedEventsCount , "]")
            /*@formatter:on*/
            if (removedEventsCount > 0) {
                val msg = "Outdated Events: - $removedEventsCount"
                Logger.captureEvent(msg)
            }
        }
    }

    companion object {
        private val TAG = EventsRepositoryImpl::class.java.simpleName
    }

}