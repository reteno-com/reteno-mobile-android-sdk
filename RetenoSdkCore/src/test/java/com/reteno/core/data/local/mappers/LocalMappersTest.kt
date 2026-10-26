package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionRequest
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.recommendation.post.RecomEvent
import com.reteno.core.domain.model.recommendation.post.RecomEventType
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal class LocalMappersTest {

    @Test
    fun `should store booleans as stored flags and send them as numbers`() = runUnitTest {
        given()
        val values = listOf(true, false)

        whenn()
        val stored = values.map { it.toDb() } to values.map { it.toIntValue() }

        then()
        assertEquals(listOf(BooleanDb.TRUE, BooleanDb.FALSE), stored.first)
        assertEquals(listOf(1, 0), stored.second)
    }

    @Test
    fun `should store every device field`() = runUnitTest {
        given()
        val device = stubDevice().copy(pushSubscribed = true, advertisingId = "ad-1", email = "a@b.c", phone = "+100")

        whenn()
        val stored = device.toDb()

        then()
        assertEquals(
            listOf(
                device.deviceId, device.externalUserId, device.pushToken, BooleanDb.TRUE, DeviceCategoryDb.MOBILE,
                DeviceOsDb.ANDROID, device.osVersion, device.deviceModel, device.appVersion, device.languageCode,
                device.timeZone, "ad-1", "a@b.c", "+100"
            ),
            listOf(
                stored.deviceId, stored.externalUserId, stored.pushToken, stored.pushSubscribed, stored.category,
                stored.osType, stored.osVersion, stored.deviceModel, stored.appVersion, stored.languageCode,
                stored.timeZone, stored.advertisingId, stored.email, stored.phone
            )
        )
    }

    @Test
    fun `should store unknown push subscription as absent`() = runUnitTest {
        given()
        val device = stubDevice().copy(pushSubscribed = null)

        whenn()
        val stored = device.toDb()

        then()
        assertNull(stored.pushSubscribed)
    }

    @Test
    fun `should store every os and device category`() = runUnitTest {
        given()
        val systems = DeviceOS.values().toList()
        val categories = DeviceCategory.values().toList()

        whenn()
        val stored = systems.map { it.toDb() } to categories.map { it.toDb() }

        then()
        assertEquals(listOf(DeviceOsDb.ANDROID, DeviceOsDb.IOS), stored.first)
        assertEquals(listOf(DeviceCategoryDb.MOBILE, DeviceCategoryDb.TABLET), stored.second)
    }

    @Test
    fun `should store events with occurrence in utc millis and parameters`() = runUnitTest {
        given()
        val event = Event.Custom("purchase", OCCURRED, listOf(Parameter("price", "10")))

        whenn()
        val stored = Events(deviceId = "device-1", externalUserId = "user-1", eventList = listOf(event)).toDb()

        then()
        val storedEvent = stored.eventList.single()
        val parameter = requireNotNull(storedEvent.params).single()
        assertEquals(
            listOf("device-1", "user-1", "purchase", "2026-01-02T01:04:05.678Z", "price", "10"),
            listOf(
                stored.deviceId,
                stored.externalUserId,
                storedEvent.eventTypeKey,
                storedEvent.occurred,
                parameter.name,
                parameter.value
            )
        )
    }

    @Test
    fun `should store event without parameters`() = runUnitTest {
        given()
        val event = Event.Custom("purchase", OCCURRED)

        whenn()
        val stored = event.toDb()

        then()
        assertNull(stored.params)
    }

    @Test
    fun `should store interaction with its action as json`() = runUnitTest {
        given()
        val interaction = Interaction(
            status = InteractionStatus.CLICKED,
            time = "t",
            token = "token",
            action = InteractionAction("CLICK", "btn", "u")
        )

        whenn()
        val stored = interaction.toDb("interaction-1")

        then()
        assertEquals(
            listOf(
                "interaction-1",
                InteractionStatusDb.CLICKED,
                "t",
                "token",
                """{"type":"CLICK","targetComponentId":"btn","url":"u"}"""
            ),
            listOf(stored.interactionId, stored.status, stored.time, stored.token, stored.action)
        )
    }

    @Test
    fun `should store interaction without action`() = runUnitTest {
        given()
        val interaction = Interaction(status = InteractionStatus.OPENED, time = "t")

        whenn()
        val stored = interaction.toDb("interaction-1")

        then()
        assertNull(stored.action)
    }

    @Test
    fun `should store and read back every interaction status`() = runUnitTest {
        given()
        val statuses = InteractionStatus.values().toList()

        whenn()
        val stored = statuses.map { it.toDb() }

        then()
        assertEquals(
            listOf(InteractionStatusDb.DELIVERED, InteractionStatusDb.CLICKED, InteractionStatusDb.OPENED),
            stored
        )
        assertEquals(statuses, stored.map { it.fromDb() })
    }

    @Test
    fun `should store interaction request`() = runUnitTest {
        given()
        val request = InteractionRequest(id = "interaction-1", status = InteractionStatus.DELIVERED)

        whenn()
        val stored = request.toDb()

        then()
        assertEquals(listOf("interaction-1", InteractionStatusDb.DELIVERED), listOf(stored.id, stored.status))
    }

    @Test
    fun `should store in-app interaction with time in utc`() = runUnitTest {
        given()
        val interaction = InAppInteraction("in-app-1", OCCURRED, 7, InAppInteractionStatus.FAILED, "script error")

        whenn()
        val stored = interaction.toDb()

        then()
        assertEquals(
            listOf("in-app-1", "2026-01-02T01:04:05Z", 7L, "FAILED", "script error"),
            listOf(stored.interactionId, stored.time, stored.messageInstanceId, stored.status, stored.statusDescription)
        )
    }

    @Test
    fun `should store every log event field and level`() = runUnitTest {
        given()
        val event = stubRetenoLogEvent()

        whenn()
        val stored = event.toDb()
        val levels = LogLevel.values().map { it.toDb() }

        then()
        assertEquals(
            listOf(
                DeviceOsDb.ANDROID,
                "14",
                "1.0 (1)",
                event.device,
                event.sdkVersion,
                event.deviceId,
                event.bundleId,
                LogLevelDb.ERROR,
                event.errorMessage
            ),
            listOf(
                stored.platformName, stored.osVersion, stored.version, stored.device, stored.sdkVersion,
                stored.deviceId, stored.bundleId, stored.logLevel, stored.errorMessage
            )
        )
        assertEquals(LogLevelDb.values().toList(), levels)
    }

    @Test
    fun `should store recommendation events with occurrence in utc`() = runUnitTest {
        given()
        val events = RecomEvents(
            recomVariantId = "variant-1",
            recomEvents = listOf(
                RecomEvent(RecomEventType.CLICKS, OCCURRED, "sku-1"),
                RecomEvent(RecomEventType.IMPRESSIONS, OCCURRED, "sku-2")
            )
        )

        whenn()
        val stored = events.toDb()

        then()
        val storedEvents = requireNotNull(stored.recomEvents)
        assertEquals("variant-1", stored.recomVariantId)
        assertEquals(
            listOf(
                listOf(RecomEventTypeDb.CLICKS, "2026-01-02T01:04:05Z", "sku-1"),
                listOf(RecomEventTypeDb.IMPRESSIONS, "2026-01-02T01:04:05Z", "sku-2")
            ),
            storedEvents.map { listOf(it.recomEventType, it.occurred, it.productId) }
        )
    }

    @Test
    fun `should store recommendation variant without events`() = runUnitTest {
        given()
        val events = RecomEvents(recomVariantId = "variant-1", recomEvents = null)

        whenn()
        val stored = events.toDb()

        then()
        assertNull(stored.recomEvents)
    }

    private companion object {
        val OCCURRED: ZonedDateTime = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 678_000_000, ZoneOffset.ofHours(2))
    }
}
