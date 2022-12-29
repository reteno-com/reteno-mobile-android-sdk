package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.RemoteConstants
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_CANCELLED
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.util.Logger
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class EventsRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_id"
        private const val EXTERNAL_DEVICE_ID = "external_device_id"
        private const val EVENT_TYPE_KEY = "event_type_key"
        private const val OCCURRED = "2022-11-11T20:22:21Z"
        private const val ECOM_EVENT_KEY = RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID
        private const val ECOM_EVENT_EXTERNAL_ORDER_ID = "external_order_id"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient

    @RelaxedMockK
    private lateinit var databaseManagerEvents: RetenoDatabaseManagerEvents

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    private lateinit var SUT: EventsRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        SUT = EventsRepositoryImpl(apiClient, databaseManagerEvents, configRepository)
    }

    @Test
    fun givenValidEvents_whenEventsSent_thenSaveEvents() {
        // Given
        val event = Event.Custom(EVENT_TYPE_KEY, ZonedDateTime.now())
        val events = Events(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_DEVICE_ID,
            eventList = listOf(event)
        ).toDb()

        // When
        SUT.saveEvent(event)

        // Then
        verify(exactly = 1) { databaseManagerEvents.insertEvents(events) }
    }

    @Test
    fun givenValidEcomEvents_whenEventsSent_thenSaveEvents() {
        // Given
        val occurred = ZonedDateTime.now()
        val ecomEvent = EcomEvent.OrderCancelled(ECOM_EVENT_EXTERNAL_ORDER_ID, occurred)

        val event = Event.Custom(
            EVENT_TYPE_ORDER_CANCELLED,
            occurred,
            listOf<Parameter>(Parameter(ECOM_EVENT_KEY, ECOM_EVENT_EXTERNAL_ORDER_ID))
        )
        val events = Events(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_DEVICE_ID,
            eventList = listOf(event)
        ).toDb()

        // When
        SUT.saveEcomEvent(ecomEvent)

        // Then
        verify(exactly = 1) { databaseManagerEvents.insertEvents(events) }
    }

    @Test
    fun givenValidEvents_whenEventsPush_thenApiClientEventsWithCorrectParameters() {
        // Given
        val eventDb = getEventsDb()

        every { databaseManagerEvents.getEvents(any()) } returns listOf(eventDb) andThen emptyList()

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 1) { apiClient.post(eq(ApiContract.MobileApi.Events), eq(eventDb.toRemote().toJson()), any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushSuccessful_thenTryPushNextEvents() {
        // Given
        val eventDb = getEventsDb()
        every { databaseManagerEvents.getEvents(any()) } returnsMany listOf(
            listOf(eventDb),
            listOf(eventDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerEvents.deleteEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val eventDb = getEventsDb()
        every { databaseManagerEvents.getEvents(any()) } returns listOf(eventDb)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidEvents_whenEventsPushFailedAndErrorIsNonRepeatable_thenTryPushNextEvents() {
        // Given
        val eventDb = getEventsDb()
        every { databaseManagerEvents.getEvents(any()) } returnsMany listOf(
            listOf(eventDb),
            listOf(eventDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerEvents.getEvents(null) }
        verify(exactly = 2) { databaseManagerEvents.deleteEvents(eventDb.eventList.size) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoEventsInDb_whenEventsPush_thenApiClientDoesNotCalled() {
        // Given
        every { databaseManagerEvents.getEvents(any()) } returns emptyList()

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }
    }

    @Test
    fun noOutdatedInteraction_whenClearOldEvents_thenSentNothing() {
        // Given
        every { databaseManagerEvents.deleteEventsByTime(any()) } returns 0

        // When
        SUT.clearOldEvents(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerEvents.deleteEventsByTime(any()) }
        verify(exactly = 0) { Logger.captureEvent(any()) }
    }

    @Test
    fun thereAreOutdatedInteraction_whenClearOldEvents_thenSentCountDeleted() {
        // Given
        val deletedEvents = 2
        every { databaseManagerEvents.deleteEventsByTime(any()) } returns deletedEvents
        val expectedMsg = "Outdated Events: - $deletedEvents"

        // When
        SUT.clearOldEvents(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManagerEvents.deleteEventsByTime(any()) }
        verify(exactly = 1) { Logger.captureEvent(eq(expectedMsg)) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getEventsDb() = EventsDb(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_DEVICE_ID,
        eventList = listOf(
            EventDb(
                eventTypeKey = EVENT_TYPE_KEY,
                occurred = OCCURRED,
                params = listOf(ParameterDb("key", "false"))
            )
        )
    )
    // endregion helper methods --------------------------------------------------------------------
}