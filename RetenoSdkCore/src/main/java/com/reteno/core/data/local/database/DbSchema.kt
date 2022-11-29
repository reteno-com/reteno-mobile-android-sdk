package com.reteno.core.data.local.database


internal object DbSchema {
    internal const val DATABASE_NAME = "reteno.db"
    internal const val DATABASE_VERSION = 1

    internal const val COLUMN_TIMESTAMP = "timeStamp"

    // --------------------- Device ----------------------------------------------------------------
    internal object DeviceSchema {
        internal const val TABLE_NAME_DEVICE = "Device"

        internal const val COLUMN_DEVICE_ROW_ID = "row_id"
        internal const val COLUMN_DEVICE_ID = "deviceId"
        internal const val COLUMN_EXTERNAL_USER_ID = "externalUserId"
        internal const val COLUMN_PUSH_TOKEN = "pushToken"
        internal const val COLUMN_PUSH_SUBSCRIBED = "pushSubscribed"
        internal const val COLUMN_CATEGORY = "category"
        internal const val COLUMN_OS_TYPE = "osType"
        internal const val COLUMN_OS_VERSION = "osVersion"
        internal const val COLUMN_DEVICE_MODEL = "deviceModel"
        internal const val COLUMN_APP_VERSION = "appVersion"
        internal const val COLUMN_LANGUAGE_CODE = "languageCode"
        internal const val COLUMN_TIMEZONE = "timeZone"
        internal const val COLUMN_ADVERTISING_ID = "advertisingId"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_DEVICE" +
                    "(" +
                    "$COLUMN_DEVICE_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "$COLUMN_DEVICE_ID TEXT NOT NULL, " +
                    "$COLUMN_EXTERNAL_USER_ID TEXT, " +
                    "$COLUMN_PUSH_TOKEN TEXT, " +
                    "$COLUMN_PUSH_SUBSCRIBED TEXT, " +
                    "$COLUMN_CATEGORY TEXT NOT NULL, " +
                    "$COLUMN_OS_TYPE TEXT NOT NULL, " +
                    "$COLUMN_OS_VERSION TEXT, " +
                    "$COLUMN_DEVICE_MODEL TEXT, " +
                    "$COLUMN_APP_VERSION TEXT, " +
                    "$COLUMN_LANGUAGE_CODE TEXT, " +
                    "$COLUMN_TIMEZONE TEXT, " +
                    "$COLUMN_ADVERTISING_ID TEXT" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_DEVICE_ROW_ID,
            COLUMN_TIMESTAMP,
            COLUMN_DEVICE_ID,
            COLUMN_EXTERNAL_USER_ID,
            COLUMN_PUSH_TOKEN,
            COLUMN_PUSH_SUBSCRIBED,
            COLUMN_CATEGORY,
            COLUMN_OS_TYPE,
            COLUMN_OS_VERSION,
            COLUMN_DEVICE_MODEL,
            COLUMN_APP_VERSION,
            COLUMN_LANGUAGE_CODE,
            COLUMN_TIMEZONE,
            COLUMN_ADVERTISING_ID
        )
    }

    // --------------------- User ------------------------------------------------------------------
    internal object UserSchema {
        internal const val TABLE_NAME_USER = "User"

        internal const val COLUMN_USER_ROW_ID = "user_row_id"
        internal const val COLUMN_DEVICE_ID = "deviceId"
        internal const val COLUMN_EXTERNAL_USER_ID = "externalUserId"
        internal const val COLUMN_SUBSCRIPTION_KEYS = "subscriptionKeys"
        internal const val COLUMN_GROUP_NAMES_INCLUDE = "groupNamesInclude"
        internal const val COLUMN_GROUP_NAMES_EXCLUDE = "groupNamesExclude"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_USER" +
                    "(" +
                    "$COLUMN_USER_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "$COLUMN_DEVICE_ID TEXT, " +
                    "$COLUMN_EXTERNAL_USER_ID TEXT, " +
                    "$COLUMN_SUBSCRIPTION_KEYS TEXT, " +
                    "$COLUMN_GROUP_NAMES_INCLUDE TEXT, " +
                    "$COLUMN_GROUP_NAMES_EXCLUDE TEXT" +
                    ")"
    }

    internal object UserAttributesSchema {
        internal const val TABLE_NAME_USER_ATTRIBUTES = "UserAttributes"

        internal const val COLUMN_PHONE = "phone"
        internal const val COLUMN_EMAIL = "email"
        internal const val COLUMN_FIRST_NAME = "firstName"
        internal const val COLUMN_LAST_NAME = "lastName"
        internal const val COLUMN_LANGUAGE_CODE = "languageCode"
        internal const val COLUMN_TIME_ZONE = "timeZone"
        internal const val COLUMN_CUSTOM_FIELDS = "fields"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_USER_ATTRIBUTES" +
                    "(" +
                    "${UserSchema.COLUMN_USER_ROW_ID} INTEGER NOT NULL, " +
                    "$COLUMN_PHONE TEXT, " +
                    "$COLUMN_EMAIL TEXT, " +
                    "$COLUMN_FIRST_NAME TEXT, " +
                    "$COLUMN_LAST_NAME TEXT, " +
                    "$COLUMN_LANGUAGE_CODE TEXT, " +
                    "$COLUMN_TIME_ZONE TEXT, " +
                    "$COLUMN_CUSTOM_FIELDS TEXT, " +
                    "FOREIGN KEY (${UserSchema.COLUMN_USER_ROW_ID}) REFERENCES ${UserSchema.TABLE_NAME_USER} (${UserSchema.COLUMN_USER_ROW_ID}) ON DELETE CASCADE" +
                    ")"
    }

    internal object UserAddressSchema {
        internal const val TABLE_NAME_USER_ADDRESS = "UserAddress"

        internal const val COLUMN_REGION = "region"
        internal const val COLUMN_TOWN = "town"
        internal const val COLUMN_ADDRESS = "address"
        internal const val COLUMN_POSTCODE = "postcode"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_USER_ADDRESS" +
                    "(" +
                    "${UserSchema.COLUMN_USER_ROW_ID} INTEGER NOT NULL, " +
                    "$COLUMN_REGION TEXT, " +
                    "$COLUMN_TOWN TEXT, " +
                    "$COLUMN_ADDRESS TEXT, " +
                    "$COLUMN_POSTCODE TEXT, " +
                    "FOREIGN KEY (${UserSchema.COLUMN_USER_ROW_ID}) REFERENCES ${UserSchema.TABLE_NAME_USER} (${UserSchema.COLUMN_USER_ROW_ID}) ON DELETE CASCADE" +
                    ")"
    }

    // --------------------- Push status -----------------------------------------------------------
    internal object InteractionSchema {
        internal const val TABLE_NAME_INTERACTION = "Interaction"

        internal const val COLUMN_INTERACTION_ROW_ID = "row_id"

        internal const val COLUMN_INTERACTION_ID = "interactionId"
        internal const val COLUMN_INTERACTION_STATUS = "status"
        internal const val COLUMN_INTERACTION_TIME = "time"
        internal const val COLUMN_INTERACTION_TOKEN = "token"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_INTERACTION" +
                    "(" +
                    "$COLUMN_INTERACTION_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "$COLUMN_INTERACTION_ID TEXT, " +
                    "$COLUMN_INTERACTION_STATUS TEXT, " +
                    "$COLUMN_INTERACTION_TIME TEXT, " +
                    "$COLUMN_INTERACTION_TOKEN TEXT" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_INTERACTION_ROW_ID,
            COLUMN_TIMESTAMP,
            COLUMN_INTERACTION_ID,
            COLUMN_INTERACTION_STATUS,
            COLUMN_INTERACTION_TIME,
            COLUMN_INTERACTION_TOKEN
        )
    }

    // --------------------- Events ----------------------------------------------------------------
    internal object EventsSchema {
        internal const val TABLE_NAME_EVENTS = "Events"

        internal const val COLUMN_EVENTS_ID = "events_id"

        internal const val COLUMN_EVENTS_DEVICE_ID = "deviceId"
        internal const val COLUMN_EVENTS_EXTERNAL_USER_ID = "externalUserId"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_EVENTS" +
                    "(" +
                    "$COLUMN_EVENTS_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_EVENTS_DEVICE_ID TEXT, " +
                    "$COLUMN_EVENTS_EXTERNAL_USER_ID TEXT" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_EVENTS_ID,
            COLUMN_EVENTS_DEVICE_ID,
            COLUMN_EVENTS_EXTERNAL_USER_ID
        )
    }

    internal object EventSchema {
        internal const val TABLE_NAME_EVENT = "Event"

        internal const val COLUMN_EVENT_ROW_ID = "row_id"

        internal const val COLUMN_EVENT_TYPE_KEY = "eventTypeKey"
        internal const val COLUMN_EVENT_OCCURRED = "occurred"
        internal const val COLUMN_EVENT_PARAMS = "params"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_EVENT" +
                    "(" +
                    "${EventsSchema.COLUMN_EVENTS_ID} INTEGER NOT NULL, " +
                    "$COLUMN_EVENT_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_EVENT_TYPE_KEY TEXT, " +
                    "$COLUMN_EVENT_OCCURRED TIMESTAMP, " +
                    "$COLUMN_EVENT_PARAMS TEXT, " +
                    "FOREIGN KEY (${EventsSchema.COLUMN_EVENTS_ID}) REFERENCES ${EventsSchema.TABLE_NAME_EVENTS} (${EventsSchema.COLUMN_EVENTS_ID})" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            EventsSchema.COLUMN_EVENTS_ID,
            COLUMN_EVENT_ROW_ID,
            COLUMN_EVENT_TYPE_KEY,
            COLUMN_EVENT_OCCURRED,
            COLUMN_EVENT_PARAMS
        )
    }

    // --------------------- App Inbox -------------------------------------------------------------
    internal object AppInboxSchema {
        internal const val TABLE_NAME_APP_INBOX = "AppInbox"

        internal const val COLUMN_APP_INBOX_ID = "messageId"
        internal const val COLUMN_APP_INBOX_DEVICE_ID = "deviceId"
        internal const val COLUMN_APP_INBOX_STATUS = "status"
        internal const val COLUMN_APP_INBOX_TIME = "time"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_APP_INBOX" +
                    "(" +
                    "$COLUMN_APP_INBOX_ID TEXT PRIMARY KEY, " +
                    "$COLUMN_APP_INBOX_DEVICE_ID TEXT, " +
                    "$COLUMN_APP_INBOX_STATUS TEXT, " +
                    "$COLUMN_APP_INBOX_TIME TEXT" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_APP_INBOX_ID,
            COLUMN_APP_INBOX_DEVICE_ID,
            COLUMN_APP_INBOX_STATUS,
            COLUMN_APP_INBOX_TIME
        )
    }

    // --------------------- Recommendations -------------------------------------------------------
    internal object RecomEventsSchema {
        internal const val TABLE_NAME_RECOM_EVENTS = "RecomEvents"

        internal const val COLUMN_RECOM_VARIANT_ID = "recomVariantId"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_RECOM_EVENTS" +
                    "($COLUMN_RECOM_VARIANT_ID TEXT PRIMARY KEY NOT NULL)"

        fun getAllColumns(): Array<String> = arrayOf(
            COLUMN_RECOM_VARIANT_ID
        )
    }

    internal object RecomEventSchema {
        internal const val TABLE_NAME_RECOM_EVENT = "RecomEvent"

        internal const val COLUMN_RECOM_EVENT_ROW_ID = "rowId"
        internal const val COLUMN_RECOM_EVENT_PRODUCT_ID = "productId"
        internal const val COLUMN_RECOM_EVENT_OCCURRED = "occurred"
        internal const val COLUMN_RECOM_EVENT_TYPE = "eventType"

        internal const val SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_RECOM_EVENT" +
                    "(" +
                    "${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} TEXT NOT NULL, " +
                    "$COLUMN_RECOM_EVENT_ROW_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_RECOM_EVENT_PRODUCT_ID TEXT, " +
                    "$COLUMN_RECOM_EVENT_OCCURRED TIMESTAMP, " +
                    "$COLUMN_RECOM_EVENT_TYPE TEXT, " +
                    "FOREIGN KEY (${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID}) REFERENCES ${RecomEventsSchema.TABLE_NAME_RECOM_EVENTS} (${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID})" +
                    ")"

        fun getAllColumns(): Array<String> = arrayOf(
            RecomEventsSchema.COLUMN_RECOM_VARIANT_ID,
            COLUMN_RECOM_EVENT_ROW_ID,
            COLUMN_RECOM_EVENT_PRODUCT_ID,
            COLUMN_RECOM_EVENT_OCCURRED,
            COLUMN_RECOM_EVENT_TYPE
        )
    }
}