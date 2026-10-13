package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class ContactRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN_NEW = "FCM_Token"
        private val CATEGORY = DeviceCategory.MOBILE
        private val OS_TYPE = DeviceOS.ANDROID

        private const val USER_ATTRS_PHONE = "+380990009900"
        private const val USER_ATTRS_EMAIL = "email@gmail.com"
        private const val USER_ATTRS_EMAIL_NEW = "new_email@gmail.com"
        private const val USER_ATTRS_FIRST_NAME = "John"
        private const val USER_ATTRS_LAST_NAME = "Doe"
        private const val USER_ATTRS_LANGUAGE_CODE = "ua"
        private const val USER_ATTRS_TIMEZONE = "Kyiv"
        private const val USER_ATTRS_FIELD_KEY_1 = "key1"
        private const val USER_ATTRS_FIELD_VALUE_1 = "value1"
        private const val USER_ATTRS_FIELD_KEY_2 = "key2"
        private const val USER_ATTRS_FIELD_VALUE_2 = "value2"
        private const val USER_ATTRS_ADDRESS_REGION = "UA"
        private const val USER_ATTRS_ADDRESS_TOWN = "Dnipro"
        private const val USER_ATTRS_ADDRESS_ADDRESS = "Street, 7"
        private const val USER_ATTRS_ADDRESS_POSTCODE = "45000"
        private val USER_SUBSCRIPTION_KEYS = listOf("key1", "key2")
        private val USER_GROUP_NAMES_INCLUDE = listOf("add1")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("remove1")

        private const val SERVER_ERROR_NON_REPEATABLE = 500
        private const val SERVER_ERROR_REPEATABLE = 400

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            BaseRobolectricTest.beforeClass()
            mockkStatic(User::toDb)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(User::toDb)
            BaseRobolectricTest.afterClass()
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var syncRequests = 0
    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository
    @RelaxedMockK
    private lateinit var databaseManagerDevice: RetenoDatabaseManagerDevice
    @RelaxedMockK
    private lateinit var databaseManagerUser: RetenoDatabaseManagerUser

    private lateinit var SUT: ContactRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        syncRequests = 0
        SUT = ContactRepositoryImpl(configRepository, databaseManagerDevice, databaseManagerUser) { syncRequests++ }
    }

    @Test
    fun givenDeviceIsNotYetPresentInDb_whenSaveDeviceData_thenInsertDbAndPush() {
        // Given
        val device = getDevice()
        val deviceDb = getDeviceDb()
        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(
            emptyList(),
            listOf(deviceDb),
            emptyList()
        )

        // When
        SUT.saveDeviceData(device, toParallelWork = false)

        // Then
        verify(exactly = 1) { databaseManagerDevice.insertDevice(deviceDb) }
        assertEquals(1, syncRequests)
    }

    @Test
    fun givenDeviceAlreadyPresentInDb_whenSaveDeviceData_thenNotInsertDbAndPush() {
        // Given
        val device = getDevice()
        val deviceDb = getDeviceDb()
        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(
            listOf(deviceDb),
            listOf(deviceDb),
            emptyList()
        )

        // When
        SUT.saveDeviceData(device, toParallelWork = false)

        // Then
        verify(exactly = 0) { databaseManagerDevice.insertDevice(deviceDb) }
        assertEquals(1, syncRequests)
    }

    @Test
    fun whenSendUsedData_thenCallUserToDbMapper() = runTest {
        // Given
        val user = getUser()

        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { configRepository.getDeviceId() } returns deviceId

        // When
        SUT.saveUserData(user)

        // Then
        verify { user.toDb(deviceId) }
    }

    @Test
    fun whenSaveUserAttributes_thenInsertDb() = runTest {
        // Given
        val user = getUser()
        every { configRepository.getDeviceId() } returns mockk(relaxed = true)
        val userDb = user.toDb(mockk(relaxed = true))
        every { databaseManagerUser.getUsers(any()) } returnsMany listOf(
            listOf(userDb),
            emptyList()
        )

        // When
        SUT.saveUserData(user)

        // Then
        verify { databaseManagerUser.insertUser(userDb) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getUser() = User(
        userAttributes = UserAttributes(
            phone = USER_ATTRS_PHONE,
            email = USER_ATTRS_EMAIL,
            firstName = USER_ATTRS_FIRST_NAME,
            lastName = USER_ATTRS_LAST_NAME,
            languageCode = USER_ATTRS_LANGUAGE_CODE,
            timeZone = USER_ATTRS_TIMEZONE,
            address = Address(
                region = USER_ATTRS_ADDRESS_REGION,
                town = USER_ATTRS_ADDRESS_TOWN,
                address = USER_ATTRS_ADDRESS_ADDRESS,
                postcode = USER_ATTRS_ADDRESS_POSTCODE
            ),
            fields = listOf(
                UserCustomField(USER_ATTRS_FIELD_KEY_1, USER_ATTRS_FIELD_VALUE_1),
                UserCustomField(USER_ATTRS_FIELD_KEY_2, USER_ATTRS_FIELD_VALUE_2)
            )
        ),
        subscriptionKeys = USER_SUBSCRIPTION_KEYS,
        groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
        groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
    )

    private fun getDevice() = Device(
        deviceId = DEVICE_ID,
        deviceIdSuffix = null,
        externalUserId = EXTERNAL_DEVICE_ID,
        pushToken = FCM_TOKEN_NEW,
        pushSubscribed = null,
        category = DeviceCategory.MOBILE,
        osType = DeviceOS.ANDROID,
        osVersion = null,
        deviceModel = null,
        appVersion = null,
        languageCode = null,
        timeZone = null,
        advertisingId = null,
        email = null,
        phone = null
    )

    private fun getDeviceDb() = DeviceDb(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_DEVICE_ID,
        pushToken = FCM_TOKEN_NEW,
        pushSubscribed = null,
        category = DeviceCategoryDb.MOBILE,
        osType = DeviceOsDb.ANDROID,
        osVersion = null,
        deviceModel = null,
        appVersion = null,
        languageCode = null,
        timeZone = null,
        advertisingId = null,
        email = null,
        phone = null
    )
    // endregion helper methods --------------------------------------------------------------------
}