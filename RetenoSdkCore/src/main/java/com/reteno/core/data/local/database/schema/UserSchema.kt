package com.reteno.core.data.local.database.schema

internal object UserSchema {
    internal const val TABLE_NAME_USER = "User"

    internal const val COLUMN_USER_ROW_ID = "user_row_id"
    internal const val COLUMN_DEVICE_ID = "deviceId"
    internal const val COLUMN_EXTERNAL_USER_ID = "externalUserId"
    internal const val COLUMN_SUBSCRIPTION_KEYS = "subscriptionKeys"
    internal const val COLUMN_GROUP_NAMES_INCLUDE = "groupNamesInclude"
    internal const val COLUMN_GROUP_NAMES_EXCLUDE = "groupNamesExclude"
    internal const val COLUMN_SYNCHRONIZED_WITH_BACKEND = "synchronizedWithBackend"

    internal const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME_USER" +
                "(" +
                "$COLUMN_USER_ROW_ID INTEGER PRIMARY KEY, " +
                "${DbSchema.COLUMN_TIMESTAMP} TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "$COLUMN_DEVICE_ID TEXT, " +
                "$COLUMN_EXTERNAL_USER_ID TEXT, " +
                "$COLUMN_SUBSCRIPTION_KEYS TEXT, " +
                "$COLUMN_GROUP_NAMES_INCLUDE TEXT, " +
                "$COLUMN_GROUP_NAMES_EXCLUDE TEXT," +
                "$COLUMN_SYNCHRONIZED_WITH_BACKEND TEXT" +
                ")"

    internal const val SQL_UPGRADE_TABLE_VERSION_6 =
        "ALTER TABLE $TABLE_NAME_USER ADD COLUMN $COLUMN_SYNCHRONIZED_WITH_BACKEND TEXT"

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
}