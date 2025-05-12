package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.RemoteConstants
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.Util.fromRemoteExplicitMillis
import com.reteno.core.util.isNonRepeatableError
import java.time.ZonedDateTime

internal class EventsRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManagerEvents,
    private val configRepository: ConfigRepository
) : EventsRepository {

    /**
     * Types that are mentioned in this list will be pushed to backend with collectLatest principle,
     * so the most latest item will be included and others are discarded
     * */
    private val distinctEventTypes = listOf(RemoteConstants.EcomEvent.EVENT_TYPE_CART_UPDATED)

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
            databaseManager.insertEvents(events)
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
            databaseManager.insertEvents(events)
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

        val eventsToSend = events.distinctBy(distinctEventTypes)

        apiClient.post(
            ApiContract.MobileApi.Events,
            eventsToSend.toRemote().toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/

                    databaseManager.deleteEvents(events)
                    if (databaseManager.getEventsCount() > 0) {
                        pushEvents()
                    } else {
                        PushOperationQueue.nextOperation()
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteEvents(events)
                        PushOperationQueue.nextOperation()
                    } else {
                        PushOperationQueue.removeAllOperations()
                    }
                }

            }
        )
    }

    override fun clearOldEvents(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "outdatedTime = [" , outdatedTime , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedEvents: List<EventDb> =
                databaseManager.deleteEventsByTime(outdatedTime.formatToRemote())
            if (removedEvents.isNotEmpty()) {
                /*@formatter:off*/ Logger.i(TAG, "clearOldEvents(): ", "removedEventsCount = [" , removedEvents.count() , "]")
                /*@formatter:on*/
                removedEvents
                    .groupBy { it.eventTypeKey }
                    .map { it.key to "${it.value.size}" }
                    .forEach {
                        val eventKeyType = it.first
                        val count = it.second

                        val msg = "$REMOVED_EVENTS($eventKeyType) - $count"
                        val event = RetenoLogEvent(
                            logLevel = LogLevel.INFO,
                            errorMessage = msg
                        )
                        Logger.captureEvent(event)
                    }
            }
        }
    }

    /**
     * Take latest event for types that exist in [types]
     * @param types - list of types in which only latest event should be selected
     * */
    private fun EventsDb.distinctBy(types: List<String>): EventsDb {
        val distinctEventList = eventList
            .groupBy { it.eventTypeKey }
            .mapValues { entry ->
                if (!types.contains(entry.key)) entry.value
                else listOf(entry.value.maxBy { it.occurred.fromRemoteExplicitMillis() })
            }
            .values
            .flatten()
        return copy(
            eventList = distinctEventList
        )
    }

    companion object {
        private val TAG = EventsRepositoryImpl::class.java.simpleName

        private const val REMOVED_EVENTS = "Removed events"
    }

}