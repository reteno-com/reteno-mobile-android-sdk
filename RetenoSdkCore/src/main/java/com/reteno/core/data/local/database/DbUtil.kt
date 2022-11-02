package com.reteno.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.model.InteractionModelDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.listFromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.user.AddressDTO
import com.reteno.core.data.remote.model.user.UserAttributesDTO
import com.reteno.core.data.remote.model.user.UserCustomFieldDTO
import com.reteno.core.data.remote.model.user.UserDTO
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.core.util.allElementsNotNull
import com.reteno.core.util.allElementsNull


internal object DbUtil {

    // --------------------- Device ----------------------------------------------------------------
    fun ContentValues.putDevice(device: Device) {
        put(DbSchema.DeviceSchema.COLUMN_DEVICE_ID, device.deviceId)
        put(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID, device.externalUserId)
        put(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN, device.pushToken)
        put(DbSchema.DeviceSchema.COLUMN_CATEGORY, device.category.toString())
        put(DbSchema.DeviceSchema.COLUMN_OS_TYPE, device.osType.toString())
        put(DbSchema.DeviceSchema.COLUMN_OS_VERSION, device.osVersion)
        put(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL, device.deviceModel)
        put(DbSchema.DeviceSchema.COLUMN_APP_VERSION, device.appVersion)
        put(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE, device.languageCode)
        put(DbSchema.DeviceSchema.COLUMN_TIMEZONE, device.timeZone)
        put(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID, device.advertisingId)
    }

    fun Cursor.getDevice(): Device? {
        val deviceId = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_ID))
        val externalUserId =
            getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID))
        val pushToken = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN))
        val category =
            DeviceCategory.fromString(getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_CATEGORY)))
        val osType =
            DeviceOS.fromString(getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_TYPE)))
        val osVersion = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_VERSION))
        val deviceModel = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL))
        val appVersion = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_APP_VERSION))
        val languageCode =
            getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE))
        val timeZone = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_TIMEZONE))
        val advertisingId =
            getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID))

        return if (deviceId == null) {
            null
        } else {
            Device(
                deviceId = deviceId,
                externalUserId = externalUserId,
                pushToken = pushToken,
                category = category,
                osType = osType,
                osVersion = osVersion,
                deviceModel = deviceModel,
                appVersion = appVersion,
                languageCode = languageCode,
                timeZone = timeZone,
                advertisingId = advertisingId
            )
        }
    }


    // --------------------- User ------------------------------------------------------------------
    fun ContentValues.putUser(user: UserDTO) {
        put(DbSchema.UserSchema.COLUMN_DEVICE_ID, user.deviceId)
        put(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID, user.externalUserId)
        put(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS, user.subscriptionKeys?.toJson())
        put(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE, user.groupNamesInclude?.toJson())
        put(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE, user.groupNamesExclude?.toJson())
    }

    fun ContentValues.putUserAttributes(rowId: Long, userAttributes: UserAttributesDTO) {
        put(DbSchema.UserSchema.COLUMN_USER_ROW_ID, rowId)

        put(DbSchema.UserAttributesSchema.COLUMN_PHONE, userAttributes.phone)
        put(DbSchema.UserAttributesSchema.COLUMN_EMAIL, userAttributes.email)
        put(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME, userAttributes.firstName)
        put(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME, userAttributes.lastName)
        put(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE, userAttributes.languageCode)
        put(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE, userAttributes.timeZone)

        put(
            DbSchema.UserAttributesSchema.COLUMN_TIME_CUSTOM_FIELDS,
            userAttributes.fields?.toJson()
        )
    }

    fun ContentValues.putUserAddress(rowId: Long, userAddress: AddressDTO) {
        put(DbSchema.UserSchema.COLUMN_USER_ROW_ID, rowId)

        put(DbSchema.UserAddressSchema.COLUMN_REGION, userAddress.region)
        put(DbSchema.UserAddressSchema.COLUMN_TOWN, userAddress.town)
        put(DbSchema.UserAddressSchema.COLUMN_ADDRESS, userAddress.address)
        put(DbSchema.UserAddressSchema.COLUMN_POSTCODE, userAddress.postcode)
    }

    fun Cursor.getUser(): UserDTO? {
        val region = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_REGION))
        val town = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_TOWN))
        val address = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_ADDRESS))
        val postCode = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_POSTCODE))

        val userAddress = if (allElementsNull(region, town, address, postCode)) {
            null
        } else {
            AddressDTO(region, town, address, postCode)
        }

        val phone = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_PHONE))
        val email = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_EMAIL))
        val firstName =
            getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
        val lastName =
            getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME))
        val languageCode =
            getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
        val timeZone =
            getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
        val customFields =
            getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_TIME_CUSTOM_FIELDS))?.listFromJson<UserCustomFieldDTO>()

        val userAttributes = if (allElementsNull(
                phone,
                email,
                firstName,
                lastName,
                languageCode,
                timeZone,
                customFields
            )
        ) {
            null
        } else {
            UserAttributesDTO(
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

        val deviceId = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_DEVICE_ID))
        val externalUserId =
            getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID))
        val subscriptionKeys =
            getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS))?.fromJson<List<String>>()
        val groupNamesInclude =
            getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE))?.fromJson<List<String>>()
        val groupNamesExclude =
            getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))?.fromJson<List<String>>()

        return if (deviceId == null || externalUserId == null) {
            null
        } else {
            UserDTO(
                deviceId,
                externalUserId,
                userAttributes,
                subscriptionKeys,
                groupNamesInclude,
                groupNamesExclude
            )
        }
    }

    // --------------------- Push Status -----------------------------------------------------------
    fun ContentValues.putInteraction(interaction: InteractionModelDb) {
        put(DbSchema.InteractionSchema.COLUMN_INTERACTION_ID, interaction.interactionId)
        put(DbSchema.InteractionSchema.COLUMN_INTERACTION_TIME, interaction.time)
        put(DbSchema.InteractionSchema.COLUMN_INTERACTION_STATUS, interaction.status.toString())
        put(DbSchema.InteractionSchema.COLUMN_INTERACTION_TOKEN, interaction.token)
    }

    fun Cursor.getInteraction(): InteractionModelDb? {
        val interactionId =
            getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_ID))
        val status =
            getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_STATUS))
        val time =
            getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_TIME))
        val token =
            getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_TOKEN))

        return if (allElementsNotNull(interactionId, status, time, token)) {
            InteractionModelDb(
                interactionId = interactionId!!,
                status = InteractionStatus.fromString(status),
                time = time!!,
                token = token!!
            )
        } else {
            null
        }
    }
}