package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.data.repository.EventsRepositoryImplTest
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.RemoteConstants
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Event.Companion.SCREEN_VIEW_EVENT_TYPE_KEY
import com.reteno.core.domain.model.event.Event.Companion.SCREEN_VIEW_PARAM_NAME
import com.reteno.core.domain.model.event.Parameter
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertEquals
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime


class EventControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val EVENT_TYPE_KEY = "key"
        private const val SCREEN_NAME = "CustomScreenName"
        private const val ECOM_EVENT_EXTERNAL_ORDER_ID = "external_order_id"
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectSchedulerUtils()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectSchedulerUtils()
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var eventsRepository: EventsRepository

    private lateinit var SUT: EventController
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()

        SUT = EventController(eventsRepository)
    }

    @Test
    fun whenTrackEvent_thenEventPassedToRepository() {
        // Given
        val event = Event.Custom(EVENT_TYPE_KEY, ZonedDateTime.now(), emptyList())

        // When
        SUT.trackEvent(event)

        // Then
        verify(exactly = 1) { eventsRepository.saveEvent(event) }
    }

    @Test
    fun whenTrackEcomEvent_thenEventPassedToRepository() {
        // Given
        val occurred = ZonedDateTime.now()
        val event = EcomEvent.OrderCancelled(ECOM_EVENT_EXTERNAL_ORDER_ID, occurred)

        // When
        SUT.trackEcomEvent(event)

        // Then
        verify(exactly = 1) { eventsRepository.saveEcomEvent(event) }
    }

    @Test
    fun whenTrackScreenViewEvent_thenEventPassedToRepository() {
        // Given
        val eventCaptured = slot<Event>()
        justRun { eventsRepository.saveEvent(capture(eventCaptured)) }

        // When
        SUT.trackScreenViewEvent(SCREEN_NAME)

        // Then
        verify(exactly = 1) { eventsRepository.saveEvent(any()) }
        assertEquals(SCREEN_VIEW_EVENT_TYPE_KEY, eventCaptured.captured.eventTypeKey)
        assertEquals(1, eventCaptured.captured.params?.size)
        val paramsActual = eventCaptured.captured.params?.get(0)
        assertEquals(SCREEN_VIEW_PARAM_NAME, paramsActual?.name)
        assertEquals(SCREEN_NAME, paramsActual?.value)
    }

    @Test
    fun whenPushEvents_thenRepositoryEventsPushCalled() {
        // When
        SUT.pushEvents()

        // Then
        verify(exactly = 1) { eventsRepository.pushEvents() }
    }

    @Test
    fun whenClearOldInteractions_thenRepositoryInteractionPushCalledWithOutdatedDate() {
        // Given
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        // When
        SUT.clearOldEvents()

        // Then
        verify(exactly = 1) { eventsRepository.clearOldEvents(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }
    }

}