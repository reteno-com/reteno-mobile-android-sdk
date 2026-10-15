package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.fixtures.stubEventDb
import com.reteno.core.fixtures.stubEventsDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class EventMapperTest {

    @Test
    fun `should send events of a device with their parameters`() = runUnitTest {
        given()
        val event = stubEventDb().copy(params = listOf(ParameterDb(name = "price", value = "10")))

        whenn()
        val remote = stubEventsDb(listOf(event)).toRemote()

        then()
        val sent = remote.eventList.single()
        val parameter = requireNotNull(sent.params).single()
        assertEquals(
            listOf("device-body", "external-user", "purchase", "2026-01-02T03:04:05.000Z", "price", "10"),
            listOf(
                remote.deviceId,
                remote.externalUserId,
                sent.eventTypeKey,
                sent.occurred,
                parameter.name,
                parameter.value
            )
        )
    }

    @Test
    fun `should send event without parameters`() = runUnitTest {
        given()
        val event = stubEventDb()

        whenn()
        val remote = event.toRemote()

        then()
        assertNull(remote.params)
    }
}
