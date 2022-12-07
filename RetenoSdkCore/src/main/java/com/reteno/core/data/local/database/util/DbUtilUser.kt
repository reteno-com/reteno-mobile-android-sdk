package com.reteno.core.data.local.database.util

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.UserSchema
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.listFromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.util.allElementsNull
import net.sqlcipher.Cursor

fun ContentValues.putUser(user: UserDb) {
    put(UserSchema.COLUMN_DEVICE_ID, user.deviceId)
    put(UserSchema.COLUMN_EXTERNAL_USER_ID, user.externalUserId)
    user.subscriptionKeys?.toJson()?.let { subscriptionKeys ->
        put(UserSchema.COLUMN_SUBSCRIPTION_KEYS, subscriptionKeys)
    }
    user.groupNamesInclude?.toJson()?.let { groupNamesInclude ->
        put(UserSchema.COLUMN_GROUP_NAMES_INCLUDE, groupNamesInclude)
    }
    user.groupNamesExclude?.toJson()?.let { groupNamesExclude ->
        put(UserSchema.COLUMN_GROUP_NAMES_EXCLUDE, groupNamesExclude)
    }
}

fun ContentValues.putUserAttributes(parentRowId: Long, userAttributes: UserAttributesDb) {
    put(UserSchema.COLUMN_USER_ROW_ID, parentRowId)

    put(UserSchema.UserAttributesSchema.COLUMN_PHONE, userAttributes.phone)
    put(UserSchema.UserAttributesSchema.COLUMN_EMAIL, userAttributes.email)
    put(UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME, userAttributes.firstName)
    put(UserSchema.UserAttributesSchema.COLUMN_LAST_NAME, userAttributes.lastName)
    put(UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE, userAttributes.languageCode)
    put(UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE, userAttributes.timeZone)

    userAttributes.fields?.toJson()?.let { userAttrs ->
        put(UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS, userAttrs)
    }
}

fun ContentValues.putUserAddress(parentRowId: Long, userAddress: AddressDb) {
    put(UserSchema.COLUMN_USER_ROW_ID, parentRowId)

    put(UserSchema.UserAddressSchema.COLUMN_REGION, userAddress.region)
    put(UserSchema.UserAddressSchema.COLUMN_TOWN, userAddress.town)
    put(UserSchema.UserAddressSchema.COLUMN_ADDRESS, userAddress.address)
    put(UserSchema.UserAddressSchema.COLUMN_POSTCODE, userAddress.postcode)
}

fun Cursor.getUser(): UserDb? {
    val region = getStringOrNull(getColumnIndex(UserSchema.UserAddressSchema.COLUMN_REGION))
    val town = getStringOrNull(getColumnIndex(UserSchema.UserAddressSchema.COLUMN_TOWN))
    val address = getStringOrNull(getColumnIndex(UserSchema.UserAddressSchema.COLUMN_ADDRESS))
    val postCode = getStringOrNull(getColumnIndex(UserSchema.UserAddressSchema.COLUMN_POSTCODE))

    val userAddress = if (allElementsNull(region, town, address, postCode)) {
        null
    } else {
        AddressDb(region = region, town = town, address = address, postcode = postCode)
    }

    val phone = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_PHONE))
    val email = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_EMAIL))
    val firstName = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
    val lastName = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_LAST_NAME))
    val languageCode = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
    val timeZone = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
    val customFields = getStringOrNull(getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))?.listFromJson<UserCustomFieldDb>()

    val userAttributes = if (allElementsNull(
            phone,
            email,
            firstName,
            lastName,
            languageCode,
            timeZone,
            userAddress,
            customFields
        )
    ) {
        null
    } else {
        UserAttributesDb(
            phone = phone,
            email = email,
            firstName = firstName,
            lastName = lastName,
            languageCode = languageCode,
            timeZone = timeZone,
            address = userAddress,
            fields = customFields
        )
    }

    val deviceId = getStringOrNull(getColumnIndex(UserSchema.COLUMN_DEVICE_ID))
    val externalUserId = getStringOrNull(getColumnIndex(UserSchema.COLUMN_EXTERNAL_USER_ID))
    val subscriptionKeys = getStringOrNull(getColumnIndex(UserSchema.COLUMN_SUBSCRIPTION_KEYS))?.fromJson<List<String>>()
    val groupNamesInclude = getStringOrNull(getColumnIndex(UserSchema.COLUMN_GROUP_NAMES_INCLUDE))?.fromJson<List<String>>()
    val groupNamesExclude = getStringOrNull(getColumnIndex(UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))?.fromJson<List<String>>()

    return if (deviceId == null || externalUserId == null) {
        null
    } else {
        UserDb(
            deviceId = deviceId,
            externalUserId = externalUserId,
            userAttributes = userAttributes,
            subscriptionKeys = subscriptionKeys,
            groupNamesInclude = groupNamesInclude,
            groupNamesExclude = groupNamesExclude
        )
    }
}