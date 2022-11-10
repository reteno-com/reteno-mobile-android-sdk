package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.event.EventDTO
import com.reteno.core.data.remote.model.event.EventsDTO
import com.reteno.core.data.remote.model.event.ParameterDTO
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.event.Event
import com.reteno.core.model.event.Events
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class EventsRepositoryTest : BaseRobolectricTest() {

    companion object {
        private const val DEVICE_ID = "device_id"
        private const val EXTERNAL_DEVICE_ID = "external_device_id"
        private const val EVENT_TYPE_KEY = "event_type_key"
        private const val OCCURRED = "2022-11-11T20:22:21Z"

    }

    @RelaxedMockK
    private lateinit var apiClient: ApiClient

    @RelaxedMockK
    private lateinit var retenoDatabaseManager: RetenoDatabaseManager

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    private lateinit var SUT: EventsRepositoryImpl

    @Before
    override fun before() {
        super.before()
        mockkObject(PushOperationQueue)
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        SUT = EventsRepositoryImpl(apiClient, retenoDatabaseManager, configRepository)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
    }

    @Test
    fun givenValidEvents_whenEventsSent_thenSaveEvents() {
        val event = Event(EVENT_TYPE_KEY, ZonedDateTime.now())
        val events = Events(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_DEVICE_ID,
            eventList = listOf(event)
        ).toRemote()

        SUT.saveEvent(event)

        verify(exactly = 1) { retenoDatabaseManager.insertEvents(events) }
    }

    @Test
    fun givenValidEvents_whenEventsPush_thenApiClientEventsWithCorrectParameters() {
        val eventDto = getEvents()

        every { retenoDatabaseManager.getEvents(any()) } returns listOf(eventDto) andThen emptyList()

        SUT.pushEvents()


        verify(exactly = 1) { apiClient.post(eq(ApiContract.MobileApi.Events), eq(eventDto.toJson()), any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushSuccessful_thenTryPushNextEvents() {
        val eventDto = getEvents()
        every { retenoDatabaseManager.getEvents(any()) } returnsMany listOf(
            listOf(eventDto),
            listOf(eventDto),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        SUT.pushEvents()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { retenoDatabaseManager.deleteEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidEvents_whenEventsPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val eventDto = getEvents()
        every { retenoDatabaseManager.getEvents(any()) } returns listOf(eventDto)
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
        val eventDto = getEvents()
        every { retenoDatabaseManager.getEvents(any()) } returnsMany listOf(
            listOf(eventDto),
            listOf(eventDto),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        SUT.pushEvents()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { retenoDatabaseManager.getEvents(1) }
        verify(exactly = 2) { retenoDatabaseManager.deleteEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoEventsInDb_whenEventsPush_thenApiClientDoesNotCalled() {
        // Given
        every { retenoDatabaseManager.getEvents(any()) } returns emptyList()

        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

    }

    private fun getEvents() = EventsDTO(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_DEVICE_ID,
        eventList = listOf(
            EventDTO(
                eventTypeKey = EVENT_TYPE_KEY,
                occurred = OCCURRED,
                params = listOf(ParameterDTO("key", "false"))
            )
        )
    )
}