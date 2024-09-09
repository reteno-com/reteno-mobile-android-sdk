package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.user.AddressRemote
import com.reteno.core.data.remote.model.user.UserAttributesRemote
import com.reteno.core.data.remote.model.user.UserCustomFieldRemote
import com.reteno.core.data.remote.model.user.UserRemote

internal fun UserDb.toRemote() = UserRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
    userAttributes = userAttributes?.toRemote(),
    subscriptionKeys = subscriptionKeys,
    groupNamesInclude = groupNamesInclude,
    groupNamesExclude = groupNamesExclude
)

internal fun UserAttributesDb.toRemote() = UserAttributesRemote(
    phone = phone,
    email = email,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
    timeZone = timeZone,
    address = address?.toRemote(),
    fields = fields?.map { it.toRemote() }
)

internal fun UserRemote.toDb() = UserDb(
    deviceId = deviceId,
    externalUserId = externalUserId,
    userAttributes = userAttributes?.toDb(),
    subscriptionKeys = subscriptionKeys,
    groupNamesInclude = groupNamesInclude,
    groupNamesExclude = groupNamesExclude
)

internal fun UserAttributesRemote.toDb() = UserAttributesDb(
    phone = phone,
    email = email,
    firstName = firstName,
    lastName = lastName,
    languageCode = languageCode,
    timeZone = timeZone,
    address = address?.toDb(),
    fields = fields?.map { it.toDb() }
)

internal fun UserCustomFieldRemote.toDb() = UserCustomFieldDb(
    key = key,
    value = value
)

internal fun AddressRemote.toDb() = AddressDb(
    region = region,
    town = town,
    address = address,
    postcode = postcode
)

internal fun UserCustomFieldDb.toRemote() = UserCustomFieldRemote(
    key = key,
    value = value
)

internal fun AddressDb.toRemote() = AddressRemote(
    region = region,
    town = town,
    address = address,
    postcode = postcode
)