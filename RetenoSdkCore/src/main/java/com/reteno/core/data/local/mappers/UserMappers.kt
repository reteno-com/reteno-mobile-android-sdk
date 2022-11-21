package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField

fun User.toDb(deviceId: DeviceId) = UserDb(
    deviceId = deviceId.id,
    externalUserId = deviceId.externalId
        ?: throw IllegalStateException("External ID is null, but required non null"),
    userAttributes = userAttributes?.toDb(),
    subscriptionKeys = subscriptionKeys,
    groupNamesInclude = groupNamesInclude,
    groupNamesExclude = groupNamesExclude
)

fun UserAttributes.toDb() = UserAttributesDb(
    phone = phone,
    email = email,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
    timeZone = timeZone,
    address = address?.toDb(),
    fields = fields?.map { it.toDb() }
)

fun UserCustomField.toDb() = UserCustomFieldDb(
    key = key,
    value = value
)

fun Address.toDb() = AddressDb(
    region = region,
    town = town,
    address = address,
    postcode = postcode
)