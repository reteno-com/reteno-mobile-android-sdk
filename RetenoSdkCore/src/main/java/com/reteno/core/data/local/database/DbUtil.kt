package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.listFromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.model.device.Device
import com.reteno.core.util.allElementsNotNull
import com.reteno.core.util.allElementsNull
import net.sqlcipher.Cursor

// --------------------- Device ----------------------------------------------------------------
fun ContentValues.putDevice(device: DeviceDb) {
    put(DbSchema.DeviceSchema.COLUMN_DEVICE_ID, device.deviceId)
    put(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID, device.externalUserId)
    put(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN, device.pushToken)
    put(DbSchema.DeviceSchema.COLUMN_PUSH_SUBSCRIBED, device.pushSubscribed?.toString())
    put(DbSchema.DeviceSchema.COLUMN_CATEGORY, device.category.toString())
    put(DbSchema.DeviceSchema.COLUMN_OS_TYPE, device.osType.toString())
    put(DbSchema.DeviceSchema.COLUMN_OS_VERSION, device.osVersion)
    put(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL, device.deviceModel)
    put(DbSchema.DeviceSchema.COLUMN_APP_VERSION, device.appVersion)
    put(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE, device.languageCode)
    put(DbSchema.DeviceSchema.COLUMN_TIMEZONE, device.timeZone)
    put(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID, device.advertisingId)
}

fun Cursor.getDevice(): DeviceDb? {
    val deviceId = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_ID))
    val externalUserId = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_EXTERNAL_USER_ID))
    val pushToken = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_PUSH_TOKEN))
    val pushSubscribedString = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_PUSH_SUBSCRIBED))
    val pushSubscribed: BooleanDb? = BooleanDb.fromString(pushSubscribedString)
    val category = DeviceCategoryDb.fromString(getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_CATEGORY)))
    val osType = DeviceOsDb.fromString(getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_TYPE)))
    val osVersion = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_OS_VERSION)) ?: Device.fetchOsVersion()
    val deviceModel = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_DEVICE_MODEL)) ?: Device.fetchDeviceModel()
    val appVersion = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_APP_VERSION)) ?: Device.fetchAppVersion()
    val languageCode = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_LANGUAGE_CODE)) ?: Device.fetchLanguageCode()
    val timeZone = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_TIMEZONE)) ?: Device.fetchTimeZone()
    val advertisingId = getStringOrNull(getColumnIndex(DbSchema.DeviceSchema.COLUMN_ADVERTISING_ID))

    return if (deviceId == null) {
        null
    } else {
        DeviceDb(
            deviceId = deviceId,
            externalUserId = externalUserId,
            pushToken = pushToken,
            pushSubscribed = pushSubscribed,
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
fun ContentValues.putUser(user: UserDb) {
    put(DbSchema.UserSchema.COLUMN_DEVICE_ID, user.deviceId)
    put(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID, user.externalUserId)
    user.subscriptionKeys?.toJson()?.let { subscriptionKeys ->
        put(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS, subscriptionKeys)
    }
    user.groupNamesInclude?.toJson()?.let { groupNamesInclude ->
        put(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE, groupNamesInclude)
    }
    user.groupNamesExclude?.toJson()?.let { groupNamesExclude ->
        put(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE, groupNamesExclude)
    }
}

fun ContentValues.putUserAttributes(parentRowId: Long, userAttributes: UserAttributesDb) {
    put(DbSchema.UserSchema.COLUMN_USER_ROW_ID, parentRowId)

    put(DbSchema.UserAttributesSchema.COLUMN_PHONE, userAttributes.phone)
    put(DbSchema.UserAttributesSchema.COLUMN_EMAIL, userAttributes.email)
    put(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME, userAttributes.firstName)
    put(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME, userAttributes.lastName)
    put(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE, userAttributes.languageCode)
    put(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE, userAttributes.timeZone)

    userAttributes.fields?.toJson()?.let { userAttrs ->
        put(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS, userAttrs)
    }
}

fun ContentValues.putUserAddress(parentRowId: Long, userAddress: AddressDb) {
    put(DbSchema.UserSchema.COLUMN_USER_ROW_ID, parentRowId)

    put(DbSchema.UserAddressSchema.COLUMN_REGION, userAddress.region)
    put(DbSchema.UserAddressSchema.COLUMN_TOWN, userAddress.town)
    put(DbSchema.UserAddressSchema.COLUMN_ADDRESS, userAddress.address)
    put(DbSchema.UserAddressSchema.COLUMN_POSTCODE, userAddress.postcode)
}

fun Cursor.getUser(): UserDb? {
    val region = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_REGION))
    val town = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_TOWN))
    val address = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_ADDRESS))
    val postCode = getStringOrNull(getColumnIndex(DbSchema.UserAddressSchema.COLUMN_POSTCODE))

    val userAddress = if (allElementsNull(region, town, address, postCode)) {
        null
    } else {
        AddressDb(region = region, town = town, address = address, postcode = postCode)
    }

    val phone = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_PHONE))
    val email = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_EMAIL))
    val firstName = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
    val lastName = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME))
    val languageCode = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
    val timeZone = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
    val customFields = getStringOrNull(getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))?.listFromJson<UserCustomFieldDb>()

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

    val deviceId = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_DEVICE_ID))
    val externalUserId = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID))
    val subscriptionKeys = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS))?.fromJson<List<String>>()
    val groupNamesInclude = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE))?.fromJson<List<String>>()
    val groupNamesExclude = getStringOrNull(getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))?.fromJson<List<String>>()

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

// --------------------- Push Status -----------------------------------------------------------
fun ContentValues.putInteraction(interaction: InteractionDb) {
    put(DbSchema.InteractionSchema.COLUMN_INTERACTION_ID, interaction.interactionId)
    put(DbSchema.InteractionSchema.COLUMN_INTERACTION_TIME, interaction.time)
    put(DbSchema.InteractionSchema.COLUMN_INTERACTION_STATUS, interaction.status.toString())
    put(DbSchema.InteractionSchema.COLUMN_INTERACTION_TOKEN, interaction.token)
}

fun Cursor.getInteraction(): InteractionDb? {
    val interactionId = getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_ID))
    val status = getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_STATUS))
    val time = getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_TIME))
    val token = getStringOrNull(getColumnIndex(DbSchema.InteractionSchema.COLUMN_INTERACTION_TOKEN))

    return if (allElementsNotNull(interactionId, status, time, token)) {
        InteractionDb(
            interactionId = interactionId!!,
            status = InteractionStatusDb.fromString(status),
            time = time!!,
            token = token!!
        )
    } else {
        null
    }
}

// --------------------- Events ----------------------------------------------------------------
fun ContentValues.putEvents(events: EventsDb) {
    put(DbSchema.EventsSchema.COLUMN_EVENTS_DEVICE_ID, events.deviceId)
    put(DbSchema.EventsSchema.COLUMN_EVENTS_EXTERNAL_USER_ID, events.externalUserId)
}

fun List<EventDb>.toContentValuesList(parentRowId: Long): List<ContentValues> {
    val contentValues = mutableListOf<ContentValues>()

    for (event in this) {
        val singleContentValues = ContentValues().apply {
            put(DbSchema.EventsSchema.COLUMN_EVENTS_ID, parentRowId)
            put(DbSchema.EventSchema.COLUMN_EVENT_TYPE_KEY, event.eventTypeKey)
            put(DbSchema.EventSchema.COLUMN_EVENT_OCCURRED, event.occurred)
            event.params?.toJson()?.let { params ->
                put(DbSchema.EventSchema.COLUMN_EVENT_PARAMS, params)
            }
        }
        contentValues.add(singleContentValues)
    }

    return contentValues
}

fun Cursor.getEvent(): EventDb? {
    val eventTypeKey = getStringOrNull(getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_TYPE_KEY))
    val occurred = getStringOrNull(getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_OCCURRED))

    val paramsString = getStringOrNull(getColumnIndex(DbSchema.EventSchema.COLUMN_EVENT_PARAMS))
    val params: List<ParameterDb>? = paramsString?.listFromJson()

    val result: EventDb? = if (allElementsNotNull(eventTypeKey, occurred)) {
        EventDb(eventTypeKey!!, occurred!!, params)
    } else {
        null
    }

    return result
}

// --------------------- AppInbox ----------------------------------------------------------------
fun ContentValues.putAppInbox(inboxDb: AppInboxMessageDb) {
    put(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_ID, inboxDb.id)
    put(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID, inboxDb.deviceId)
    put(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_STATUS, inboxDb.status.toString())
    put(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_TIME, inboxDb.occurredDate)
}

fun Cursor.getAppInbox(): AppInboxMessageDb? {
    val id = getStringOrNull(getColumnIndex(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_ID))
    val deviceId = getStringOrNull(getColumnIndex(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_DEVICE_ID))
    val time = getStringOrNull(getColumnIndex(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_TIME))
    val status = getStringOrNull(getColumnIndex(DbSchema.AppInboxSchema.COLUMN_APP_INBOX_STATUS))

    return if (allElementsNotNull(id, deviceId, time, status)) {
        AppInboxMessageDb(
            id = id!!,
            deviceId = deviceId!!,
            occurredDate = time!!,
            status = AppInboxMessageStatusDb.fromString(status)
        )
    } else {
        null
    }
}