package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.util.Util
import io.mockk.*
import com.reteno.core.domain.model.event.Event
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime


class EventControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val EVENT_TYPE_KEY = "key"
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
    fun whenSaveEvent_thenEventPassedToRepository() {
        val event = Event(EVENT_TYPE_KEY, ZonedDateTime.now(), emptyList())

        SUT.saveEvent(event)

        verify(exactly = 1) {
            eventsRepository.saveEvent(event)
        }
    }

    @Test
    fun whenPushEvents_thenRepositoryEventsPushCalled() {
        SUT.pushEvents()
        verify(exactly = 1) { eventsRepository.pushEvents() }
    }

    @Test
    fun whenClearOldInteractions_thenRepositoryInteractionPushCalled() {
        mockkStatic(ZonedDateTime::class)
        mockkObject(Util)
        val mockData = mockk<ZonedDateTime>()
        val mockOutDatedData = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns false
        every { ZonedDateTime.now() } returns mockData
        every { mockData.minusHours(any()) } returns mockOutDatedData

        SUT.clearOldEvents()

        verify(exactly = 1) { eventsRepository.clearOldEvents(mockOutDatedData) }
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockData.minusHours(24) }

        unmockkStatic(ZonedDateTime::class)
        unmockkObject(Util)
    }

    @Test
    fun givenDebugMode_whenClearOldInteractions_thenRepositoryInteractionPushCalled() {
        mockkStatic(ZonedDateTime::class)
        mockkObject(Util)
        val mockData = mockk<ZonedDateTime>()
        val mockOutDatedData = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns true
        every { ZonedDateTime.now() } returns mockData
        every { mockData.minusHours(any()) } returns mockOutDatedData

        SUT.clearOldEvents()

        verify(exactly = 1) { eventsRepository.clearOldEvents(mockOutDatedData) }
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockData.minusHours(1) }

        unmockkStatic(ZonedDateTime::class)
        unmockkObject(Util)
    }
}