package com.reteno.core.data.local.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.schema.UserSchema
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test


class DbUtilUserTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val USER_PARENT_ROW_ID = 1L
        private const val DEVICE_ID = "valueDeviceId"
        private const val EXTERNAL_USER_ID = "valueExternalUserId"
        private val SUBSCRIPTION_KEYS = listOf("subscriptionKey1", "subscriptionKey2", "subscriptionKey3")
        private val GROUP_NAMES_INCLUDE = listOf("groupNamesInclude1", "groupNamesInclude2", "groupNamesInclude3")
        private val GROUP_NAMES_EXCLUDE = listOf("groupNamesExclude1", "groupNamesExclude2", "groupNamesExclude3")

        private const val PHONE = "phone1"
        private const val EMAIL = "email1"
        private const val FIRST_NAME = "firstName1"
        private const val LAST_NAME = "lastName1"
        private const val LANGUAGE_CODE = "languageCode1"
        private const val TIME_ZONE = "timeZone1"
        private const val FIELD_KEY1 = "key1"
        private const val FIELD_KEY2 = "key2"
        private const val FIELD_KEY3 = "key3"
        private const val FIELD_VALUE1 = "value1"
        private const val FIELD_VALUE2 = "value2"

        private const val REGION = "region1"
        private const val TOWN = "town1"
        private const val ADDRESS = "address1"
        private const val POSTCODE = "postcode1"

        private val addressFull = AddressDb(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        private val customFieldsFull = listOf(
            UserCustomFieldDb(FIELD_KEY1, FIELD_VALUE1),
            UserCustomFieldDb(FIELD_KEY2, FIELD_VALUE2),
            UserCustomFieldDb(FIELD_KEY3, null)
        )
        private val userAttributesFull = UserAttributesDb(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = addressFull,
            fields = customFieldsFull
        )
        private val userFull = UserDb(
            rowId = USER_PARENT_ROW_ID.toString(),
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = userAttributesFull,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )

        private const val COLUMN_INDEX_USER_ROW_ID = 1
        private const val COLUMN_INDEX_REGION = 2
        private const val COLUMN_INDEX_TOWN = 3
        private const val COLUMN_INDEX_ADDRESS = 4
        private const val COLUMN_INDEX_POSTCODE = 5
        private const val COLUMN_INDEX_PHONE = 6
        private const val COLUMN_INDEX_EMAIL = 7
        private const val COLUMN_INDEX_FIRST_NAME = 8
        private const val COLUMN_INDEX_LAST_NAME = 9
        private const val COLUMN_INDEX_LANGUAGE_CODE = 10
        private const val COLUMN_INDEX_TIME_ZONE = 11
        private const val COLUMN_INDEX_FIELDS = 12
        private const val COLUMN_INDEX_DEVICE_ID = 13
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 14
        private const val COLUMN_INDEX_SUBSCRIPTION_KEYS = 15
        private const val COLUMN_INDEX_GROUP_NAMES_INCLUDE = 16
        private const val COLUMN_INDEX_GROUP_NAMES_EXCLUDE = 17
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private val contentValues = ContentValues()

    @MockK
    private lateinit var cursor: Cursor
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        contentValues.clear()
        mockColumnIndexes()
    }

    override fun after() {
        super.after()
        contentValues.clear()
        clearMocks(cursor)
    }

    @Test
    fun givenUserProvided_whenPutUser_thenContentValuesUpdated() {
        // Given
        val user = userFull.copy(userAttributes = null)
        val keySet = arrayOf(
            UserSchema.COLUMN_DEVICE_ID,
            UserSchema.COLUMN_EXTERNAL_USER_ID,
            UserSchema.COLUMN_SUBSCRIPTION_KEYS,
            UserSchema.COLUMN_GROUP_NAMES_INCLUDE,
            UserSchema.COLUMN_GROUP_NAMES_EXCLUDE
        )

        // When
        contentValues.putUser(user)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(UserSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(UserSchema.COLUMN_EXTERNAL_USER_ID))
        assertEquals(getExpectedSubscriptionKeysFull(), contentValues.get(UserSchema.COLUMN_SUBSCRIPTION_KEYS))
        assertEquals(getGroupNamesIncludeFull(), contentValues.get(UserSchema.COLUMN_GROUP_NAMES_INCLUDE))
        assertEquals(getGroupNamesExcludeFull(), contentValues.get(UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))
    }

    @Test
    fun givenUserProvided_whenPutUserExternalUserIdOnly_thenContentValuesUpdated() {
        // Given
        val user = userFull.copy(
            userAttributes = null,
            subscriptionKeys = null,
            groupNamesInclude = null,
            groupNamesExclude = null
        )
        val keySet = arrayOf(
            UserSchema.COLUMN_DEVICE_ID,
            UserSchema.COLUMN_EXTERNAL_USER_ID
        )

        // When
        contentValues.putUser(user)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(UserSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(UserSchema.COLUMN_EXTERNAL_USER_ID))
        assertNull(contentValues.get(UserSchema.COLUMN_SUBSCRIPTION_KEYS))
        assertNull(contentValues.get(UserSchema.COLUMN_GROUP_NAMES_INCLUDE))
        assertNull(contentValues.get(UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))
    }

    @Test
    fun givenUserAttributesProvided_whenPutUserAttributesNoCustomFields_thenContentValuesUpdated() {
        // Given
        val userAttributesDb = userAttributesFull.copy(address = null, fields = null)
        val keySet = arrayOf(
            UserSchema.COLUMN_USER_ROW_ID,
            UserSchema.UserAttributesSchema.COLUMN_PHONE,
            UserSchema.UserAttributesSchema.COLUMN_EMAIL,
            UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME,
            UserSchema.UserAttributesSchema.COLUMN_LAST_NAME,
            UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE,
            UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE
        )

        // When
        contentValues.putUserAttributes(USER_PARENT_ROW_ID, userAttributesDb)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(PHONE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_PHONE))
        assertEquals(EMAIL, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_EMAIL))
        assertEquals(FIRST_NAME, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
        assertEquals(LAST_NAME, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_LAST_NAME))
        assertEquals(LANGUAGE_CODE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
        assertNull(contentValues.get(UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))
    }

    @Test
    fun givenUserAttributesProvided_whenPutUserAttributesWithCustomFields_thenContentValuesUpdated() {
        // Given
        val expectedCustomFieldsResult = getExpectedCustomFields()

        val userAttributesDb = userAttributesFull.copy(address = null)
        val keySet = arrayOf(
            UserSchema.COLUMN_USER_ROW_ID,
            UserSchema.UserAttributesSchema.COLUMN_PHONE,
            UserSchema.UserAttributesSchema.COLUMN_EMAIL,
            UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME,
            UserSchema.UserAttributesSchema.COLUMN_LAST_NAME,
            UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE,
            UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE,
            UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS
        )

        // When
        contentValues.putUserAttributes(USER_PARENT_ROW_ID, userAttributesDb)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(PHONE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_PHONE))
        assertEquals(EMAIL, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_EMAIL))
        assertEquals(FIRST_NAME, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
        assertEquals(LAST_NAME, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_LAST_NAME))
        assertEquals(LANGUAGE_CODE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
        assertEquals(expectedCustomFieldsResult, contentValues.get(UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))
    }

    @Test
    fun givenUserAddressProvided_whenPutUserAddress_thenContentValuesUpdated() {
        // Given
        val keySet = arrayOf(
            UserSchema.COLUMN_USER_ROW_ID,
            UserSchema.UserAddressSchema.COLUMN_REGION,
            UserSchema.UserAddressSchema.COLUMN_TOWN,
            UserSchema.UserAddressSchema.COLUMN_ADDRESS,
            UserSchema.UserAddressSchema.COLUMN_POSTCODE
        )

        // When
        contentValues.putUserAddress(USER_PARENT_ROW_ID, addressFull)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(REGION, contentValues.get(UserSchema.UserAddressSchema.COLUMN_REGION))
        assertEquals(TOWN, contentValues.get(UserSchema.UserAddressSchema.COLUMN_TOWN))
        assertEquals(ADDRESS, contentValues.get(UserSchema.UserAddressSchema.COLUMN_ADDRESS))
        assertEquals(POSTCODE, contentValues.get(UserSchema.UserAddressSchema.COLUMN_POSTCODE))
    }

    @Test
    fun givenCursorWithFullUserModel_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressFull()
        mockUserAttributesFull()

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(userFull, actualUser)
    }

    @Test
    fun givenCursorWithEmptyAddress_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressEmpty()
        mockUserAttributesFull()

        val expectedUserAttributes = userAttributesFull.copy(address = null)
        val expectedUser = userFull.copy(userAttributes = expectedUserAttributes)

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun givenCursorWithEmptyCustomFields_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressFull()
        mockUserAttributesWithoutCustomFields()

        val expectedUserAttributes = userAttributesFull.copy(fields = null)
        val expectedUser = userFull.copy(userAttributes = expectedUserAttributes)

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun givenCursorWithEmptyUserAttributes_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressEmpty()
        mockUserAttributesEmpty()

        val expectedUser = userFull.copy(userAttributes = null)

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun givenCursorWithUserEmpty_whenGetUser_thenUserReturned() {
        // Given
        mockUserDeviceIdExternalUserIdIsNonEmpty()
        mockUserAddressEmpty()
        mockUserAttributesEmpty()

        val expectedUser = userFull.copy(
            userAttributes = null,
            subscriptionKeys = null,
            groupNamesInclude = null,
            groupNamesExclude = null,
        )

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun givenCursorWithDeviceIdNull_whenGetUser_thenUserIsNull() {
        // Given
        mockUserDeviceIdIsNull()
        mockUserAddressEmpty()
        mockUserAttributesEmpty()

        // When
        val actualUser = cursor.getUser()

        // Then
        assertNull(actualUser)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getGroupNamesExcludeFull() =
        "[\"${GROUP_NAMES_EXCLUDE[0]}\",\"${GROUP_NAMES_EXCLUDE[1]}\",\"${GROUP_NAMES_EXCLUDE[2]}\"]"

    private fun getGroupNamesIncludeFull() =
        "[\"${GROUP_NAMES_INCLUDE[0]}\",\"${GROUP_NAMES_INCLUDE[1]}\",\"${GROUP_NAMES_INCLUDE[2]}\"]"

    private fun getExpectedSubscriptionKeysFull() =
        "[\"${SUBSCRIPTION_KEYS[0]}\",\"${SUBSCRIPTION_KEYS[1]}\",\"${SUBSCRIPTION_KEYS[2]}\"]"

    private fun getExpectedCustomFields(): String {
        val expectedCustomField1 = "{\"key\":\"$FIELD_KEY1\",\"value\":\"$FIELD_VALUE1\"}"
        val expectedCustomField2 = "{\"key\":\"$FIELD_KEY2\",\"value\":\"$FIELD_VALUE2\"}"
        val expectedCustomField3 = "{\"key\":\"$FIELD_KEY3\"}"
        return "[$expectedCustomField1,$expectedCustomField2,$expectedCustomField3]"
    }

    private fun mockUserDeviceIdIsNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_USER_ROW_ID) } returns USER_PARENT_ROW_ID.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns getExpectedSubscriptionKeysFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns getGroupNamesIncludeFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns getGroupNamesExcludeFull()
    }

    private fun mockUserExternalUserIdIsNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_USER_ROW_ID) } returns USER_PARENT_ROW_ID.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns getExpectedSubscriptionKeysFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns getGroupNamesIncludeFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns getGroupNamesExcludeFull()
    }

    private fun mockUserDeviceIdExternalUserIdIsNonEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_USER_ROW_ID) } returns USER_PARENT_ROW_ID.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns null
    }

    private fun mockUserFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_USER_ROW_ID) } returns USER_PARENT_ROW_ID.toString()
        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns getExpectedSubscriptionKeysFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns getGroupNamesIncludeFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns getGroupNamesExcludeFull()
    }

    private fun mockUserAddressFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_REGION) } returns REGION
        every { cursor.getStringOrNull(COLUMN_INDEX_TOWN) } returns TOWN
        every { cursor.getStringOrNull(COLUMN_INDEX_ADDRESS) } returns ADDRESS
        every { cursor.getStringOrNull(COLUMN_INDEX_POSTCODE) } returns POSTCODE
    }

    private fun mockUserAddressEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_REGION) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_TOWN) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_ADDRESS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_POSTCODE) } returns null
    }

    private fun mockUserAttributesFull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_PHONE) } returns PHONE
        every { cursor.getStringOrNull(COLUMN_INDEX_EMAIL) } returns EMAIL
        every { cursor.getStringOrNull(COLUMN_INDEX_FIRST_NAME) } returns FIRST_NAME
        every { cursor.getStringOrNull(COLUMN_INDEX_LAST_NAME) } returns LAST_NAME
        every { cursor.getStringOrNull(COLUMN_INDEX_LANGUAGE_CODE) } returns LANGUAGE_CODE
        every { cursor.getStringOrNull(COLUMN_INDEX_TIME_ZONE) } returns TIME_ZONE
        every { cursor.getStringOrNull(COLUMN_INDEX_FIELDS) } returns getExpectedCustomFields()
    }

    private fun mockUserAttributesWithoutCustomFields() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_PHONE) } returns PHONE
        every { cursor.getStringOrNull(COLUMN_INDEX_EMAIL) } returns EMAIL
        every { cursor.getStringOrNull(COLUMN_INDEX_FIRST_NAME) } returns FIRST_NAME
        every { cursor.getStringOrNull(COLUMN_INDEX_LAST_NAME) } returns LAST_NAME
        every { cursor.getStringOrNull(COLUMN_INDEX_LANGUAGE_CODE) } returns LANGUAGE_CODE
        every { cursor.getStringOrNull(COLUMN_INDEX_TIME_ZONE) } returns TIME_ZONE
        every { cursor.getStringOrNull(COLUMN_INDEX_FIELDS) } returns null
    }

    private fun mockUserAttributesEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_PHONE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EMAIL) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_FIRST_NAME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_LAST_NAME) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_LANGUAGE_CODE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_TIME_ZONE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_FIELDS) } returns null
    }


    private fun mockColumnIndexes() {
        every { cursor.getColumnIndex(UserSchema.UserAddressSchema.COLUMN_REGION) } returns COLUMN_INDEX_REGION
        every { cursor.getColumnIndex(UserSchema.UserAddressSchema.COLUMN_TOWN) } returns COLUMN_INDEX_TOWN
        every { cursor.getColumnIndex(UserSchema.UserAddressSchema.COLUMN_ADDRESS) } returns COLUMN_INDEX_ADDRESS
        every { cursor.getColumnIndex(UserSchema.UserAddressSchema.COLUMN_POSTCODE) } returns COLUMN_INDEX_POSTCODE

        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_PHONE) } returns COLUMN_INDEX_PHONE
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_EMAIL) } returns COLUMN_INDEX_EMAIL
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_FIRST_NAME) } returns COLUMN_INDEX_FIRST_NAME
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_LAST_NAME) } returns COLUMN_INDEX_LAST_NAME
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_TIME_ZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(UserSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS) } returns COLUMN_INDEX_FIELDS

        every { cursor.getColumnIndex(UserSchema.COLUMN_USER_ROW_ID) } returns COLUMN_INDEX_USER_ROW_ID
        every { cursor.getColumnIndex(UserSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(UserSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(UserSchema.COLUMN_SUBSCRIPTION_KEYS) } returns COLUMN_INDEX_SUBSCRIPTION_KEYS
        every { cursor.getColumnIndex(UserSchema.COLUMN_GROUP_NAMES_INCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_INCLUDE
        every { cursor.getColumnIndex(UserSchema.COLUMN_GROUP_NAMES_EXCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_EXCLUDE
    }
    // endregion helper methods --------------------------------------------------------------------
}