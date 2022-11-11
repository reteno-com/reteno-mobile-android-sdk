package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.repository.EventsRepository
import com.reteno.core.domain.model.event.Event
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
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
}