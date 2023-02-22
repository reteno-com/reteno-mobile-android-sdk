package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.model.event.EventRemote
import com.reteno.core.data.remote.model.event.EventsRemote
import com.reteno.core.data.remote.model.event.ParameterRemote
import org.junit.Assert.assertEquals

import org.junit.Test

import java.time.ZonedDateTime


class EventMapperKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "deviceId1"
        private const val EXTERNAL_USER_ID = "externalUserId1"

        private const val EVENT_1_TYPE_KEY = "key1"
        private val EVENT_1_OCCURRED = ZonedDateTime.now().minusDays(1).toString()

        private const val FIELD_PARAM_NAME_1 = "name1"
        private const val FIELD_PARAM_VALUE_1 = "value1"
        private const val FIELD_PARAM_NAME_2 = "name2"
        private const val FIELD_PARAM_VALUE_2 = "value"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenParameterDb_whenToRemote_thenParameterRemoteReturned() {
        // Given
        val parameterDb = getParameterDb1()
        val expectedParameterRemote = getParameterRemote1()

        // When
        val actualParameterRemote = parameterDb.toRemote()

        // Then
        assertEquals(expectedParameterRemote, actualParameterRemote)
    }

    @Test
    fun givenEventDb_whenToRemote_thenEventRemoteReturned() {
        // Given
        val eventDb = getEventDb()
        val expectedEventRemote = getEventRemote()

        // When
        val actualEventRemote = eventDb.toRemote()

        // Then
        assertEquals(expectedEventRemote, actualEventRemote)
    }

    @Test
    fun givenEventsDb_whenToRemote_thenEventsRemoteReturned() {
        // Given
        val eventsDb = getEventsDb()
        val expectedEventsRemote = getEventsRemote()

        // When
        val actualEventsRemote = eventsDb.toRemote()

        // Then
        assertEquals(expectedEventsRemote, actualEventsRemote)
    }


    // region helper methods -----------------------------------------------------------------------
    private fun getParameterDb1() = ParameterDb(name = FIELD_PARAM_NAME_1, value = FIELD_PARAM_VALUE_1)
    private fun getParameterRemote1() = ParameterRemote(name = FIELD_PARAM_NAME_1, value = FIELD_PARAM_VALUE_1)
    private fun getParameterDb2() = ParameterDb(name = FIELD_PARAM_NAME_2, value = FIELD_PARAM_VALUE_2)
    private fun getParameterRemote2() = ParameterRemote(name = FIELD_PARAM_NAME_2, value = FIELD_PARAM_VALUE_2)
    private fun getParamsDb() = listOf(getParameterDb1(), getParameterDb2())
    private fun getParamsRemote() = listOf(getParameterRemote1(), getParameterRemote2())

    private fun getEventDb() = EventDb(
        eventTypeKey = EVENT_1_TYPE_KEY,
        occurred = EVENT_1_OCCURRED,
        params = getParamsDb()
    )
    private fun getEventRemote() = EventRemote(
        eventTypeKey = EVENT_1_TYPE_KEY,
        occurred = EVENT_1_OCCURRED,
        params = getParamsRemote()
    )

    private fun getEventsDb() = EventsDb(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        eventList = listOf(getEventDb())
    )
    private fun getEventsRemote() = EventsRemote(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        eventList = listOf(getEventRemote())
    )
    // endregion helper methods --------------------------------------------------------------------
}