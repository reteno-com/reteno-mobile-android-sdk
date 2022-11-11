package com.reteno.core.data.local.database

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.util.Logger
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertTrue
import net.sqlcipher.Cursor
import org.junit.Assert.assertEquals
import org.junit.Test


class RetenoDatabaseManagerUserTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val ROW_ID_CORRUPTED = 101L

        private const val ROW_ID_INSERTED = 1L
        private const val TIMESTAMP = "TimeStampHere_Z"

        private const val USER_ROW_ID = "USER_ROW_ID"
        private const val DEVICE_ID = "DEVICE_ID"
        private const val EXTERNAL_USER_ID = "EXTERNAL_USER_ID"
        private val SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")
        private val GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")

        private const val PHONE = "PHONE"
        private const val EMAIL = "EMAIL"
        private const val FIRST_NAME = "FIRST_NAME"
        private const val LAST_NAME = "LAST_NAME"
        private const val LANGUAGE_CODE = "LANGUAGE_CODE"
        private const val TIME_ZONE = "TIME_ZONE"
        private const val CUSTOM_FIELD_KEY_1 = "CUSTOM_FIELD_KEY_1"
        private const val CUSTOM_FIELD_VALUE_1 = "CUSTOM_FIELD_VALUE_1"
        private const val CUSTOM_FIELD_KEY_2 = "CUSTOM_FIELD_KEY_2"
        private const val CUSTOM_FIELD_VALUE_2 = "CUSTOM_FIELD_VALUE_2"

        private const val REGION = "REGION"
        private const val TOWN = "TOWN"
        private const val ADDRESS = "ADDRESS"
        private const val POSTCODE = "POSTCODE"

        private val userAddressFull = AddressDb(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        private val customField1 =
            UserCustomFieldDb(key = CUSTOM_FIELD_KEY_1, value = CUSTOM_FIELD_VALUE_1)
        private val customField2 =
            UserCustomFieldDb(key = CUSTOM_FIELD_KEY_2, value = CUSTOM_FIELD_VALUE_2)

        private val userAttributesFull = UserAttributesDb(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = userAddressFull,
            fields = listOf(customField1, customField2)
        )
        private val userFull = UserDb(
            deviceId = DEVICE_ID,
            externalUserId = EXTERNAL_USER_ID,
            userAttributes = userAttributesFull,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE
        )

        private const val COLUMN_INDEX_TIMESTAMP = 1
        private const val COLUMN_INDEX_USER_ROW_ID = 2
        private const val COLUMN_INDEX_DEVICE_ID = 3
        private const val COLUMN_INDEX_EXTERNAL_USER_ID = 4
        private const val COLUMN_INDEX_SUBSCRIPTION_KEYS = 5
        private const val COLUMN_INDEX_GROUP_NAMES_INCLUDE = 6
        private const val COLUMN_INDEX_GROUP_NAMES_EXCLUDE = 7

        private const val COLUMN_INDEX_PHONE = 8
        private const val COLUMN_INDEX_EMAIL = 9
        private const val COLUMN_INDEX_FIRST_NAME = 10
        private const val COLUMN_INDEX_LAST_NAME = 11
        private const val COLUMN_INDEX_LANGUAGE_CODE = 12
        private const val COLUMN_INDEX_TIME_ZONE = 13
        private const val COLUMN_INDEX_CUSTOM_FIELDS = 14

        private const val COLUMN_INDEX_REGION = 15
        private const val COLUMN_INDEX_TOWN = 16
        private const val COLUMN_INDEX_ADDRESS = 17
        private const val COLUMN_INDEX_POSTCODE = 18
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    @MockK
    private lateinit var cursor: Cursor

    private var SUT: RetenoDatabaseManagerImpl? = null
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkStatic(Cursor::getUser)

        mockColumnIndexes()
        every { cursor.getStringOrNull(COLUMN_INDEX_TIMESTAMP) } returns TIMESTAMP
        justRun { cursor.close() }

        SUT = RetenoDatabaseManagerImpl(database)
    }

    override fun after() {
        super.after()
        clearMocks(cursor)
        unmockkStatic(Cursor::getUser)
    }

    @Test
    fun givenValidFullUserProvided_whenInsertUser_thenUserIsSavedToDb() {
        // Given
        val expectedContentValuesUser = ContentValues().apply {
            putUser(userFull)
        }
        val expectedContentValuesUserAttributes = ContentValues().apply {
            putUserAttributes(ROW_ID_INSERTED, userAttributesFull)
        }
        val expectedContentValuesUserAddress = ContentValues().apply {
            putUserAddress(ROW_ID_INSERTED, userAddressFull)
        }

        var actualContentValuesUser = ContentValues()
        var actualContentValuesUserAttributes = ContentValues()
        var actualContentValuesUserAddress = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValuesUser = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        } andThenAnswer  {
            actualContentValuesUserAttributes = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        } andThenAnswer {
            actualContentValuesUserAddress = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT?.insertUser(userFull)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DbSchema.UserSchema.TABLE_NAME_USER),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValuesUser, actualContentValuesUser)
        assertEquals(expectedContentValuesUserAttributes, actualContentValuesUserAttributes)
        assertEquals(expectedContentValuesUserAddress, actualContentValuesUserAddress)
    }

    @Test
    fun givenValidUserWithoutAddressProvided_whenInsertUser_thenUserIsSavedToDb() {
        // Given
        val userAttributes = userAttributesFull.copy(address = null)
        val user = userFull.copy(userAttributes = userAttributes)

        val expectedContentValuesUser = ContentValues().apply {
            putUser(user)
        }
        val expectedContentValuesUserAttributes = ContentValues().apply {
            putUserAttributes(ROW_ID_INSERTED, userAttributes)
        }
        val expectedContentValuesUserAddress = ContentValues()

        var actualContentValuesUser = ContentValues()
        var actualContentValuesUserAttributes = ContentValues()
        var actualContentValuesUserAddress = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValuesUser = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        } andThenAnswer  {
            actualContentValuesUserAttributes = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        } andThenAnswer {
            actualContentValuesUserAddress = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT?.insertUser(user)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DbSchema.UserSchema.TABLE_NAME_USER),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValuesUser, actualContentValuesUser)
        assertEquals(expectedContentValuesUserAttributes, actualContentValuesUserAttributes)
        assertEquals(expectedContentValuesUserAddress, actualContentValuesUserAddress)
    }

    @Test
    fun givenValidUserWithoutAttributesProvided_whenInsertUser_thenUserIsSavedToDb() {
        // Given
        val user = userFull.copy(userAttributes = null)
        val expectedContentValuesUser = ContentValues().apply {
            putUser(user)
        }
        val expectedContentValuesUserAttributes = ContentValues()

        var actualContentValuesUser = ContentValues()
        var actualContentValuesUserAttributes = ContentValues()
        every { database.insert(any(), null, any()) } answers {
            actualContentValuesUser = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        } andThenAnswer  {
            actualContentValuesUserAttributes = ContentValues(thirdArg<ContentValues>())
            ROW_ID_INSERTED
        }

        // When
        SUT?.insertUser(user)

        // Then
        verify(exactly = 1) {
            database.insert(
                table = eq(DbSchema.UserSchema.TABLE_NAME_USER),
                contentValues = any()
            )
        }
        assertEquals(expectedContentValuesUser, actualContentValuesUser)
        assertEquals(expectedContentValuesUserAttributes, actualContentValuesUserAttributes)
    }

    @Test
    fun givenUsersAvailableInDatabase_whenGetUser_thenUsersReturned() {
        // Given
        mockCursorRecordsNumber(2)
        mockDatabaseQuery()

        val user1 = userFull
        val user2 = userFull.copy(userAttributes = null)
        every { cursor.getUser() } returns user1 andThen user2

        // When
        val users = SUT?.getUser(null)

        // Then
        verify(exactly = 1) { database.rawQuery(any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertEquals(2, users?.size)
        assertEquals(user1, users?.get(0))
        assertEquals(user2, users?.get(1))
    }

    @Test
    fun givenUsersNotAvailableInDatabase_whenGetUser_thenEmptyListReturned() {
        // Given
        mockCursorRecordsNumber(0)
        mockDatabaseQuery()

        // When
        val users = SUT?.getUser(null)

        // Then
        verify(exactly = 1) { database.rawQuery(any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertEquals(0, users?.size)
    }

    @Test
    fun givenUserCorruptedInDatabaseAndRowIdDetected_whenGetUser_thenCorruptedRowRemoved() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()

        every { cursor.getUser() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_USER_ROW_ID) } returns ROW_ID_CORRUPTED

        // When
        val users = SUT?.getUser(null)

        // Then
        verify(exactly = 1) { database.rawQuery(any(), any()) }
        verify(exactly = 1) {
            database.delete(
                DbSchema.UserSchema.TABLE_NAME_USER,
                "${DbSchema.UserSchema.COLUMN_USER_ROW_ID}=?",
                arrayOf(ROW_ID_CORRUPTED.toString())
            )
        }
        verify(exactly = 1) { cursor.close() }

        assertTrue(users?.isEmpty() ?: false)
    }

    @Test
    fun givenUserCorruptedInDatabaseAndRowIdNotDetected_whenGetUser_thenExceptionIsLogged() {
        // Given
        mockCursorRecordsNumber(1)
        mockDatabaseQuery()
        every { cursor.getUser() } returns null
        every { cursor.getLongOrNull(COLUMN_INDEX_USER_ROW_ID) } returns null

        // When
        val users = SUT?.getUser(null)

        // Then
        verify(exactly = 1) { database.rawQuery(any(), any()) }
        verify(exactly = 1) { Logger.e(any(), any(), any()) }
        verify(exactly = 0) { database.delete(any(), any(), any()) }
        verify(exactly = 1) { cursor.close() }

        assertTrue(users?.isEmpty() ?: false)
    }

    @Test
    fun givenUserCountEmpty_whenGetUserCount_thenZeroReturned() {
        // Given
        val recordsCount = 0L
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns recordsCount

        // When
        val count = SUT?.getUserCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun givenUserCountNonEmpty_whenGetUserCount_thenCountReturned() {
        // Given
        val recordsCount = 5L
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns recordsCount

        // When
        val count = SUT?.getUserCount()

        // Then
        assertEquals(recordsCount, count)
    }

    @Test
    fun given_whenDeleteUserOldest_thenUserDeleted() {
        // Given
        val order = "ASC"
        val count = 2
        val whereClauseExpected = "${DbSchema.UserSchema.COLUMN_USER_ROW_ID} " +
                    "in (select ${DbSchema.UserSchema.COLUMN_USER_ROW_ID} " +
                    "from ${DbSchema.UserSchema.TABLE_NAME_USER} " +
                    "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                    "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteUsers(count, true)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.UserSchema.TABLE_NAME_USER, whereClauseExpected) }
    }

    @Test
    fun given_whenDeleteUserNewest_thenUserDeleted() {
        // Given
        val order = "DESC"
        val count = 6
        val whereClauseExpected = "${DbSchema.UserSchema.COLUMN_USER_ROW_ID} " +
                "in (select ${DbSchema.UserSchema.COLUMN_USER_ROW_ID} " +
                "from ${DbSchema.UserSchema.TABLE_NAME_USER} " +
                "ORDER BY ${DbSchema.COLUMN_TIMESTAMP} $order " +
                "LIMIT $count)"

        justRun { database.delete(any(), any(), any()) }

        // When
        SUT?.deleteUsers(count, false)

        // Then
        verify(exactly = 1) { database.delete(DbSchema.UserSchema.TABLE_NAME_USER, whereClauseExpected) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockColumnIndexes() {
        every { cursor.isNull(any()) } returns false

        every { cursor.getColumnIndex(DbSchema.COLUMN_TIMESTAMP) } returns COLUMN_INDEX_TIMESTAMP

        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_USER_ROW_ID) } returns COLUMN_INDEX_USER_ROW_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_DEVICE_ID) } returns COLUMN_INDEX_DEVICE_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_EXTERNAL_USER_ID) } returns COLUMN_INDEX_EXTERNAL_USER_ID
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_SUBSCRIPTION_KEYS) } returns COLUMN_INDEX_SUBSCRIPTION_KEYS
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_INCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_INCLUDE
        every { cursor.getColumnIndex(DbSchema.UserSchema.COLUMN_GROUP_NAMES_EXCLUDE) } returns COLUMN_INDEX_GROUP_NAMES_EXCLUDE

        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_PHONE) } returns COLUMN_INDEX_PHONE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_EMAIL) } returns COLUMN_INDEX_EMAIL
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_FIRST_NAME) } returns COLUMN_INDEX_FIRST_NAME
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LAST_NAME) } returns COLUMN_INDEX_LAST_NAME
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_LANGUAGE_CODE) } returns COLUMN_INDEX_LANGUAGE_CODE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_TIME_ZONE) } returns COLUMN_INDEX_TIME_ZONE
        every { cursor.getColumnIndex(DbSchema.UserAttributesSchema.COLUMN_CUSTOM_FIELDS) } returns COLUMN_INDEX_CUSTOM_FIELDS

        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_REGION) } returns COLUMN_INDEX_REGION
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_TOWN) } returns COLUMN_INDEX_TOWN
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_ADDRESS) } returns COLUMN_INDEX_ADDRESS
        every { cursor.getColumnIndex(DbSchema.UserAddressSchema.COLUMN_POSTCODE) } returns COLUMN_INDEX_POSTCODE
    }

    private fun mockCursorRecordsNumber(number: Int) {
        val responses = generateSequence(0) { it + 1 }
            .map { it < number }
            .take(number + 1)
            .toList()
        every { cursor.moveToNext() } returnsMany responses
    }

    private fun mockDatabaseQuery() {
        every { database.rawQuery(any(), any()) } returns cursor
    }
    // endregion helper methods --------------------------------------------------------------------
}