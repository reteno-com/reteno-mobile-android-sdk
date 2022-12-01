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
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.util.Logger
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class EventsRepositoryTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_id"
        private const val EXTERNAL_DEVICE_ID = "external_device_id"
        private const val EVENT_TYPE_KEY = "event_type_key"
        private const val OCCURRED = "2022-11-11T20:22:21Z"

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
        mockkObject(PushOperationQueue)
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        SUT = EventsRepositoryImpl(apiClient, databaseManagerEvents, configRepository)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
    }

    @Test
    fun givenValidEvents_whenEventsSent_thenSaveEvents() {
        val event = Event.Custom(EVENT_TYPE_KEY, ZonedDateTime.now())
        val events = Events(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_DEVICE_ID,
            eventList = listOf(event)
        ).toDb()

        SUT.saveEvent(event)

        verify(exactly = 1) { databaseManagerEvents.insertEvents(events) }
    }

    @Test
    fun givenValidEvents_whenEventsPush_thenApiClientEventsWithCorrectParameters() {
        val eventDb = getEvents()

        every { databaseManagerEvents.getEvents(any()) } returns listOf(eventDb) andThen emptyList()

        SUT.pushEvents()

        verify(exactly = 1) { apiClient.post(eq(ApiContract.MobileApi.Events), eq(eventDb.toRemote().toJson()), any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushSuccessful_thenTryPushNextEvents() {
        val eventDb = getEvents()
        every { databaseManagerEvents.getEvents(any()) } returnsMany listOf(
            listOf(eventDb),
            listOf(eventDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        SUT.pushEvents()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerEvents.deleteEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val eventDb = getEvents()
        every { databaseManagerEvents.getEvents(any()) } returns listOf(eventDb)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        SUT.pushEvents()

        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidEvents_whenEventsPushFailedAndErrorIsNonRepeatable_thenTryPushNextEvents() {
        val eventDb = getEvents()
        every { databaseManagerEvents.getEvents(any()) } returnsMany listOf(
            listOf(eventDb),
            listOf(eventDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        SUT.pushEvents()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerEvents.getEvents(1) }
        verify(exactly = 2) { databaseManagerEvents.deleteEvents(1) }
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
    fun noOutdatedInteraction_whenClearOldInteractions_thenSentNothing() {
        every { databaseManagerEvents.deleteEventsByTime(any()) } returns 0

        SUT.clearOldEvents(ZonedDateTime.now())

        verify(exactly = 1) { databaseManagerEvents.deleteEventsByTime(any()) }
        verify(exactly = 0) { Logger.captureEvent(any()) }
    }

    @Test
    fun thereAreOutdatedInteraction_whenClearOldInteractions_thenSentCountDeleted() {
        val deletedEvents = 2
        every { databaseManagerEvents.deleteEventsByTime(any()) } returns deletedEvents
        val expectedMsg = "Outdated Events: - $deletedEvents"

        SUT.clearOldEvents(ZonedDateTime.now())

        verify(exactly = 1) { databaseManagerEvents.deleteEventsByTime(any()) }
        verify(exactly = 1) { Logger.captureEvent(eq(expectedMsg)) }
    }

    private fun getEvents() = EventsDb(
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