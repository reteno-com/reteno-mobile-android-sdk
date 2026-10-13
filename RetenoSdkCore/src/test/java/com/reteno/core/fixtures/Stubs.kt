package com.reteno.core.fixtures

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserAttributesAnonymous
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

internal fun stubDevice(
    deviceId: DeviceId = stubDeviceId(),
    pushToken: String = "token",
    pushSubscribed: Boolean = true
) = Device(
    deviceId = deviceId.id,
    deviceIdSuffix = deviceId.idSuffix,
    externalUserId = deviceId.externalId,
    pushToken = pushToken,
    pushSubscribed = pushSubscribed,
    category = DeviceCategory.MOBILE,
    osVersion = "14",
    deviceModel = "Pixel",
    appVersion = "1.0",
    languageCode = "en",
    timeZone = "UTC",
    advertisingId = null,
    email = deviceId.email,
    phone = deviceId.phone
)

internal fun stubUser(
    firstName: String? = "John",
    email: String? = "john@example.com",
    phone: String? = "+380501234567",
    marketId: String? = null
) = User(userAttributes = UserAttributes(firstName = firstName, email = email, phone = phone, marketId = marketId))

internal fun stubAnonymousAttributes(
    firstName: String? = "John",
    marketId: String? = null
) = UserAttributesAnonymous(firstName = firstName, marketId = marketId)
