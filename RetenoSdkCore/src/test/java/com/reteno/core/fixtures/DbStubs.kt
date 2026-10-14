package com.reteno.core.fixtures

import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserDb

internal const val STUB_CREATED_AT = 1_767_000_000_000L

internal fun stubDeviceDb(
    rowId: String? = "1",
    createdAt: Long = STUB_CREATED_AT,
    pushToken: String? = "token",
    synced: BooleanDb? = null
) = DeviceDb(
    rowId = rowId,
    createdAt = createdAt,
    deviceId = "device-body",
    externalUserId = "external-user",
    pushToken = pushToken,
    pushSubscribed = BooleanDb.TRUE,
    category = DeviceCategoryDb.MOBILE,
    osVersion = "14",
    deviceModel = "Pixel",
    appVersion = "1.0",
    languageCode = "en",
    timeZone = "UTC",
    advertisingId = null,
    isSynchronizedWithBackend = synced,
    email = null,
    phone = null
)

internal fun stubUserDb(
    rowId: String? = "1",
    createdAt: Long = STUB_CREATED_AT,
    firstName: String? = "John",
    email: String? = null,
    synced: BooleanDb? = null
) = UserDb(
    rowId = rowId,
    createdAt = createdAt,
    deviceId = "device-body",
    externalUserId = "external-user",
    userAttributes = UserAttributesDb(firstName = firstName, email = email),
    isSynchronizedWithBackend = synced
)

internal fun stubInteractionDb(
    rowId: String? = "1",
    interactionId: String = "interaction-1",
    status: InteractionStatusDb = InteractionStatusDb.CLICKED
) = InteractionDb(
    rowId = rowId,
    interactionId = interactionId,
    status = status,
    time = "2026-01-02T03:04:05Z",
    token = "token"
)

internal fun stubInAppInteractionDb(
    rowId: String? = "1",
    status: String = "OPEN"
) = InAppInteractionDb(
    rowId = rowId,
    interactionId = "in-app-1",
    time = "2026-01-02T03:04:05Z",
    messageInstanceId = 7,
    status = status
)

internal fun stubEventDb(
    rowId: String? = "1",
    eventTypeKey: String = "purchase",
    occurred: String = "2026-01-02T03:04:05.000Z"
) = EventDb(rowId = rowId, eventTypeKey = eventTypeKey, occurred = occurred)

internal fun stubEventsDb(eventList: List<EventDb> = listOf(stubEventDb())) =
    EventsDb(deviceId = "device-body", externalUserId = "external-user", eventList = eventList)

internal fun stubAppInboxMessageDb(id: String = "message-1") =
    AppInboxMessageDb(id = id, deviceId = "device-body", occurredDate = "2026-01-02T03:04:05Z")

internal fun stubRecomEventsDb(
    recomVariantId: String = "variant-1",
    recomEvents: List<RecomEventDb>? = listOf(
        RecomEventDb(
            rowId = "1",
            recomEventType = RecomEventTypeDb.CLICKS,
            occurred = "2026-01-02T03:04:05Z",
            productId = "sku-1"
        )
    )
) = RecomEventsDb(recomVariantId = recomVariantId, recomEvents = recomEvents)

internal fun stubRetenoLogEventDb(
    rowId: String? = "1",
    errorMessage: String? = "Action Sync failed"
) = RetenoLogEventDb(
    rowId = rowId,
    osVersion = "14",
    version = "1.0 (1)",
    device = "Pixel",
    sdkVersion = "2.0.0",
    deviceId = "device-body",
    bundleId = "com.example.app",
    logLevel = LogLevelDb.ERROR,
    errorMessage = errorMessage
)
