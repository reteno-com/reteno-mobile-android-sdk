package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.user.AddressRemote
import com.reteno.core.data.remote.model.user.UserAttributesRemote
import com.reteno.core.data.remote.model.user.UserCustomFieldRemote
import com.reteno.core.data.remote.model.user.UserRemote

fun UserDb.toRemote() = UserRemote(
    deviceId = deviceId,
    externalUserId = externalUserId,
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