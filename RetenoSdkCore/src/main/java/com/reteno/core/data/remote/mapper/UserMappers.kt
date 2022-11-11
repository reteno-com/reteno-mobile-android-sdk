package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.user.AddressRemote
import com.reteno.core.data.remote.model.user.UserAttributesRemote
import com.reteno.core.data.remote.model.user.UserCustomFieldRemote
import com.reteno.core.data.remote.model.user.UserRemote
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField

fun User.toRemote(deviceId: DeviceId) = UserRemote(
    deviceId = deviceId.id,
    externalUserId = deviceId.externalId
        ?: throw IllegalStateException("External ID is null, but required non null"),
    userAttributes = userAttributes?.toRemote(),
    subscriptionKeys = subscriptionKeys,
    groupNamesInclude = groupNamesInclude,
    groupNamesExclude = groupNamesExclude
)

fun UserAttributes.toRemote() = UserAttributesRemote(
    phone = phone,
    email = email,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
    timeZone = timeZone,
    address = address?.toRemote(),
    fields = fields?.map { it.toRemote() }
)

fun UserCustomField.toRemote() = UserCustomFieldRemote(
    key = key,
    value = value
)

fun Address.toRemote() = AddressRemote(
    region = region,
    town = town,
    address = address,
    postcode = postcode
)

//==================================================================================================
fun UserDb.toRemote(deviceId: DeviceId) = UserRemote(
    deviceId = deviceId.id,
    externalUserId = deviceId.externalId
        ?: throw IllegalStateException("External ID is null, but required non null"),
    userAttributes = userAttributes?.toRemote(),
    subscriptionKeys = subscriptionKeys,
    groupNamesInclude = groupNamesInclude,
    groupNamesExclude = groupNamesExclude
)

fun UserAttributesDb.toRemote() = UserAttributesRemote(
    phone = phone,
    email = email,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
    timeZone = timeZone,
    address = address?.toRemote(),
    fields = fields?.map { it.toRemote() }
)

fun UserCustomFieldDb.toRemote() = UserCustomFieldRemote(
    key = key,
    value = value
)

fun AddressDb.toRemote() = AddressRemote(
    region = region,
    town = town,
    address = address,
    postcode = postcode
)