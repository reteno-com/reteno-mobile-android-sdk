package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.user.AddressDTO
import com.reteno.core.data.remote.model.user.UserAttributesDTO
import com.reteno.core.data.remote.model.user.UserCustomFieldDTO
import com.reteno.core.data.remote.model.user.UserDTO
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertNull
import net.sqlcipher.Cursor
import org.junit.Assert.assertEquals
import org.junit.Test


class DbUtilUserTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
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

        private const val USER_PARENT_ROW_ID = 1L

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
        val user = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = null,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )
        val keySet = arrayOf(
            DbSchema.UserSchema.COLUMN_DEVICE_ID,
            DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID,
            DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS,
            DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE,
            DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE
        )

        // When
        contentValues.putUser(user)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(DbSchema.UserSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID))
        assertEquals(getExpectedSubscriptionKeysFull(), contentValues.get(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS))
        assertEquals(getGroupNamesIncludeFull(), contentValues.get(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE))
        assertEquals(getGroupNamesExcludeFull(), contentValues.get(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))
    }

    @Test
    fun givenUserProvided_whenPutUserExternalUserIdOnly_thenContentValuesUpdated() {
        // Given
        val user = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = null,
            subscriptionKeys = null,
            groupNamesInclude = null,
            groupNamesExclude = null,
        )
        val keySet = arrayOf(
            DbSchema.UserSchema.COLUMN_DEVICE_ID,
            DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID,
            DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS,
            DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE,
            DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE
        )

        // When
        contentValues.putUser(user)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(DEVICE_ID, contentValues.get(DbSchema.UserSchema.COLUMN_DEVICE_ID))
        assertEquals(EXTERNAL_USER_ID, contentValues.get(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID))
        assertNull(contentValues.get(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS))
        assertNull(contentValues.get(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE))
        assertNull(contentValues.get(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE))
    }

    @Test
    fun givenUserAttributesProvided_whenPutUserAttributesNoCustomFields_thenContentValuesUpdated() {
        // Given
        val userAttributesDTO = UserAttributesDTO(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = null,
            fields = null
        )
        val keySet = arrayOf(
            DbSchema.UserSchema.COLUMN_USER_ROW_ID,
            DbSchema.UserAttributesSchema.COLUMN_PHONE,
            DbSchema.UserAttributesSchema.COLUMN_EMAIL,
            DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME,
            DbSchema.UserAttributesSchema.COLUMN_LAST_NAME,
            DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE,
            DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE,
            DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS
        )

        // When
        contentValues.putUserAttributes(USER_PARENT_ROW_ID, userAttributesDTO)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(DbSchema.UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(PHONE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_PHONE))
        assertEquals(EMAIL, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_EMAIL))
        assertEquals(FIRST_NAME, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
        assertEquals(LAST_NAME, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME))
        assertEquals(LANGUAGE_CODE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
        assertNull(contentValues.get(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))
    }

    @Test
    fun givenUserAttributesProvided_whenPutUserAttributesWithCustomFields_thenContentValuesUpdated() {
        // Given
        val expectedCustomFieldsResult = getExpectedCustomFields()
        val customFields = listOf<UserCustomFieldDTO>(
            UserCustomFieldDTO(FIELD_KEY1, FIELD_VALUE1),
            UserCustomFieldDTO(FIELD_KEY2, FIELD_VALUE2),
            UserCustomFieldDTO(FIELD_KEY3, null)
        )

        val userAttributesDTO = UserAttributesDTO(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = null,
            fields = customFields
        )
        val keySet = arrayOf(
            DbSchema.UserSchema.COLUMN_USER_ROW_ID,
            DbSchema.UserAttributesSchema.COLUMN_PHONE,
            DbSchema.UserAttributesSchema.COLUMN_EMAIL,
            DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME,
            DbSchema.UserAttributesSchema.COLUMN_LAST_NAME,
            DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE,
            DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE,
            DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS
        )

        // When
        contentValues.putUserAttributes(USER_PARENT_ROW_ID, userAttributesDTO)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(DbSchema.UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(PHONE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_PHONE))
        assertEquals(EMAIL, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_EMAIL))
        assertEquals(FIRST_NAME, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME))
        assertEquals(LAST_NAME, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME))
        assertEquals(LANGUAGE_CODE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE))
        assertEquals(TIME_ZONE, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE))
        assertEquals(expectedCustomFieldsResult, contentValues.get(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS))
    }

    @Test
    fun givenUserAddressProvided_whenPutUserAddress_thenContentValuesUpdated() {
        // Given
        val userAddressDTO = AddressDTO(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )
        val keySet = arrayOf(
            DbSchema.UserSchema.COLUMN_USER_ROW_ID,
            DbSchema.UserAddressSchema.COLUMN_REGION,
            DbSchema.UserAddressSchema.COLUMN_TOWN,
            DbSchema.UserAddressSchema.COLUMN_ADDRESS,
            DbSchema.UserAddressSchema.COLUMN_POSTCODE
        )

        // When
        contentValues.putUserAddress(USER_PARENT_ROW_ID, userAddressDTO)

        // Then
        assertEquals(keySet.toSet(), contentValues.keySet())

        assertEquals(USER_PARENT_ROW_ID, contentValues.get(DbSchema.UserSchema.COLUMN_USER_ROW_ID))
        assertEquals(REGION, contentValues.get(DbSchema.UserAddressSchema.COLUMN_REGION))
        assertEquals(TOWN, contentValues.get(DbSchema.UserAddressSchema.COLUMN_TOWN))
        assertEquals(ADDRESS, contentValues.get(DbSchema.UserAddressSchema.COLUMN_ADDRESS))
        assertEquals(POSTCODE, contentValues.get(DbSchema.UserAddressSchema.COLUMN_POSTCODE))
    }

    @Test
    fun givenCursorWithFullUserModel_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressFull()
        mockUserAttributesFull()

        val expectedAddress = AddressDTO(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        val expectedCustomFields = listOf<UserCustomFieldDTO>(
            UserCustomFieldDTO(FIELD_KEY1, FIELD_VALUE1),
            UserCustomFieldDTO(FIELD_KEY2, FIELD_VALUE2),
            UserCustomFieldDTO(FIELD_KEY3, null)
        )
        val expectedUserAttributes = UserAttributesDTO(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = expectedAddress,
            fields = expectedCustomFields
        )

        val expectedUser = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = expectedUserAttributes,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )

        // When
        val actualUser = cursor.getUser()

        // Then
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun givenCursorWithEmptyAddress_whenGetUser_thenUserReturned() {
        // Given
        mockUserFull()
        mockUserAddressEmpty()
        mockUserAttributesFull()

        val expectedAddress = null

        val expectedCustomFields = listOf<UserCustomFieldDTO>(
            UserCustomFieldDTO(FIELD_KEY1, FIELD_VALUE1),
            UserCustomFieldDTO(FIELD_KEY2, FIELD_VALUE2),
            UserCustomFieldDTO(FIELD_KEY3, null)
        )
        val expectedUserAttributes = UserAttributesDTO(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = expectedAddress,
            fields = expectedCustomFields
        )

        val expectedUser = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = expectedUserAttributes,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )

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

        val expectedAddress = AddressDTO(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        val expectedUserAttributes = UserAttributesDTO(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = expectedAddress,
            fields = null
        )

        val expectedUser = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = expectedUserAttributes,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )

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

        val expectedUser = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = null,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )

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

        val expectedUser = UserDTO(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
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

    @Test
    fun givenCursorWithExternalUserIdNull_whenGetUser_thenUserIsNull() {
        // Given
        mockUserExternalUserIdIsNull()
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

        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns getExpectedSubscriptionKeysFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns getGroupNamesIncludeFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns getGroupNamesExcludeFull()
    }

    private fun mockUserExternalUserIdIsNull() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns getExpectedSubscriptionKeysFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns getGroupNamesIncludeFull()
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns getGroupNamesExcludeFull()
    }

    private fun mockUserDeviceIdExternalUserIdIsNonEmpty() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getStringOrNull(COLUMN_INDEX_DEVICE_ID) } returns DEVICE_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_EXTERNAL_USER_ID) } returns EXTERNAL_USER_ID
        every { cursor.getStringOrNull(COLUMN_INDEX_SUBSCRIPTION_KEYS) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_INCLUDE) } returns null
        every { cursor.getStringOrNull(COLUMN_INDEX_GROUP_NAMES_EXCLUDE) } returns null
    }

    private fun mockUserFull() {
        every { cursor.isNull(any()) } returns false

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
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_REGION) } returns COLUMN_INDEX_REGION
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_TOWN) } returns COLUMN_INDEX_TOWN
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_ADDRESS) } returns COLUMN_INDEX_ADDRESS
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_POSTCODE) } returns COLUMN_INDEX_POSTCODE

        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_PHONE) } returns COLUMN_INDEX_PHONE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_EMAIL) } returns COLUMN_INDEX_EMAIL
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME) } returns COLUMN_INDEX_FIRST_NAME
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME) } returns COLUMN_INDEX_LAST_NAME
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS) } returns COLUMN_INDEX_FIELDS

        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_USER_ROW_ID) } returns COLUMN_INDEX_USER_ROW_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS) } returns COLUMN_INDEX_SUBSCRIPTION_KEYS
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_INCLUDE
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_EXCLUDE
    }
    // endregion helper methods --------------------------------------------------------------------
}