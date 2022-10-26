package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.remote.model.user.AddressDTO
import com.reteno.core.data.remote.model.user.UserAttributesDTO
import com.reteno.core.data.remote.model.user.UserCustomFieldDTO
import com.reteno.core.data.remote.model.user.UserDTO
import com.reteno.core.model.user.Address
import com.reteno.core.model.user.User
import com.reteno.core.model.user.UserAttributes
import com.reteno.core.model.user.UserCustomField
import java.lang.IllegalStateException

fun User.toRemote(deviceId: DeviceId): UserDTO {
    return UserDTO(
        deviceId = deviceId.id,
        externalUserId = deviceId.externalId ?: throw IllegalStateException("External ID is null, but required non null"),
        userAttributes = userAttributes?.toRemote(),
        subscriptionKeys = subscriptionKeys,
        groupNamesInclude = groupNamesInclude,
        groupNamesExclude = groupNamesExclude
    )
}

fun UserAttributes.toRemote(): UserAttributesDTO {
    return UserAttributesDTO(
        phone = phone,
        email = email,
        firstName = firstName,
        lastName = lastName,
        languageCode = languageCode,
        timeZone = timeZone,
        address = address?.toRemote(),
        fields = fields?.map { it.toRemote() }
    )
}

fun UserCustomField.toRemote(): UserCustomFieldDTO {
    return UserCustomFieldDTO(
        key = key,
        value = value
    )
}

fun Address.toRemote(): AddressDTO {
    return AddressDTO(
        region = region,
        town = town,
        address = address,
        postcode = postcode
    )
}