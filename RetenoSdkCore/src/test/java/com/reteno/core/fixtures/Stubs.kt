package com.reteno.core.fixtures

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal val STUB_TIME: ZonedDateTime = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)

internal fun stubDeviceId(
    idBody: String = "device-body",
    idSuffix: String? = null,
    externalId: String? = "external-user"
) = DeviceId(idBody = idBody, idSuffix = idSuffix, externalId = externalId)

internal fun stubCustomEvent(
    typeKey: String = "purchase",
    occurred: ZonedDateTime = STUB_TIME,
    parameters: List<Parameter>? = listOf(Parameter("sku", "A-1"))
) = Event.Custom(typeKey = typeKey, dateOccurred = occurred, parameters = parameters)

internal fun stubEcomEvent(
    externalOrderId: String = "order-1",
    occurred: ZonedDateTime = STUB_TIME
) = EcomEvent.OrderCancelled(externalOrderId, occurred)

internal fun stubRetenoLogEvent(
    logLevel: LogLevel = LogLevel.ERROR,
    errorMessage: String? = "Action Sync failed: IOException"
) = RetenoLogEvent(
    platformName = DeviceOS.ANDROID,
    osVersion = "14",
    version = "1.0 (1)",
    device = "Pixel",
    sdkVersion = "2.0.0",
    deviceId = "device-body",
    bundleId = "com.example.app",
    logLevel = logLevel,
    errorMessage = errorMessage
)
