package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class ContactRepositoryImplTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN_NEW = "FCM_Token"
        private val CATEGORY = DeviceCategory.MOBILE
        private val OS_TYPE = DeviceOS.ANDROID

        private const val USER_ATTRS_PHONE = "+380990009900"
        private const val USER_ATTRS_EMAIL = "email@gmail.com"
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

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectOperationQueue()
            mockObjectPushOperationQueue()
            mockkStatic(User::toDb)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectOperationQueue()
            unMockObjectPushOperationQueue()
            unmockkStatic(User::toDb)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
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
        SUT = ContactRepositoryImpl(apiClient, configRepository, databaseManagerDevice, databaseManagerUser)
    }

    @Test
    fun whenSaveDeviceData_thenInsertDbAndPush() {
        // Given
        val device = getDevice()
        val deviceDb = getDeviceDb()
        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(
            listOf(deviceDb),
            emptyList()
        )

        // When
        SUT.saveDeviceData(device)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify { databaseManagerDevice.insertDevice(deviceDb) }
    }

    @Test
    fun whenDbHasSavedDeviceData_thenSendToApi() {
        // Given
        val device = getDeviceDb()
        val expectedDeviceJson =
            "{\"deviceId\":\"${DEVICE_ID}\",\"externalUserId\":\"${EXTERNAL_DEVICE_ID}\",\"pushToken\":\"${FCM_TOKEN_NEW}\",\"category\":\"${CATEGORY}\",\"osType\":\"${OS_TYPE}\"}"

        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(
            listOf(device),
            emptyList()
        )

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 1) { apiClient.post(eq(ApiContract.MobileApi.Device), eq(expectedDeviceJson), any()) }
    }

    @Test
    fun whenDevicePushSuccessful_thenTryPushNextDevice() {
        // Given
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(listOf(deviceDataDb), listOf(deviceDataDb), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerDevice.deleteDevices(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenDevicePushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { databaseManagerDevice.getDevices(any()) } returns listOf(deviceDataDb)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun whenDevicePushFailedAndErrorIsNonRepeatable_thenTryPushNextDevice() {
        // Given
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { databaseManagerDevice.getDevices(any()) } returnsMany listOf(listOf(deviceDataDb), listOf(deviceDataDb), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerDevice.getDevices(1) }
        verify(exactly = 2) { databaseManagerDevice.deleteDevices(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoDeviceInDb_whenDevicePush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { databaseManagerDevice.getDevices(any()) } returns emptyList()

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

    }

    @Test
    fun whenSendUsedData_thenCallUserToDbMapper() {
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
    fun whenSaveUserAttributes_thenInsertDbAndPush() {
        // Given
        val user = getUser()
        every { configRepository.getDeviceId() } returns mockk(relaxed = true)
        val userDb = user.toDb(mockk(relaxed = true))
        every { databaseManagerUser.getUser(any()) } returnsMany listOf(
            listOf(userDb),
            emptyList()
        )

        // When
        SUT.saveUserData(user)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify { databaseManagerUser.insertUser(userDb) }
    }

    @Test
    fun whenDbHasSavedUser_thenSendToApi() {
        // Given
        val expectedUserJson =
            "{\"deviceId\":\"${DEVICE_ID}\",\"externalUserId\":\"${EXTERNAL_DEVICE_ID}\",\"userAttributes\":{\"phone\":\"${USER_ATTRS_PHONE}\",\"email\":\"${USER_ATTRS_EMAIL}\",\"firstName\":\"${USER_ATTRS_FIRST_NAME}\",\"lastName\":\"${USER_ATTRS_LAST_NAME}\",\"languageCode\":\"${USER_ATTRS_LANGUAGE_CODE}\",\"timeZone\":\"${USER_ATTRS_TIMEZONE}\",\"address\":{\"region\":\"${USER_ATTRS_ADDRESS_REGION}\",\"town\":\"${USER_ATTRS_ADDRESS_TOWN}\",\"address\":\"${USER_ATTRS_ADDRESS_ADDRESS}\",\"postcode\":\"${USER_ATTRS_ADDRESS_POSTCODE}\"},\"fields\":[{\"key\":\"${USER_ATTRS_FIELD_KEY_1}\",\"value\":\"${USER_ATTRS_FIELD_VALUE_1}\"},{\"key\":\"${USER_ATTRS_FIELD_KEY_2}\",\"value\":\"${USER_ATTRS_FIELD_VALUE_2}\"}]},\"subscriptionKeys\":${USER_SUBSCRIPTION_KEYS.toJson()},\"groupNamesInclude\":${USER_GROUP_NAMES_INCLUDE.toJson()},\"groupNamesExclude\":${USER_GROUP_NAMES_EXCLUDE.toJson()}}"
        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { configRepository.getDeviceId() } returns deviceId
        val userDb = getUser().toDb(deviceId)
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { databaseManagerUser.getUser(any()) } returnsMany listOf(
            listOf(userDb),
            emptyList()
        )

        // When
        SUT.pushUserData()

        // Then
        verify(exactly = 1) { apiClient.post(eq(ApiContract.MobileApi.User), eq(expectedUserJson), any()) }
    }

    @Test
    fun whenUserPushSuccessful_thenTryPushNextUser() {
        // Given
        val userData = mockk<UserDb>(relaxed = true)
        every { databaseManagerUser.getUser(any()) } returnsMany listOf(listOf(userData), listOf(userData), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushUserData()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { databaseManagerUser.deleteUsers(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenUserPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        // Given
        val userData = mockk<UserDb>(relaxed = true)
        every { databaseManagerUser.getUser(any()) } returns listOf(userData)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        // When
        SUT.pushUserData()

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun whenUserPushFailedAndErrorIsNonRepeatable_thenTryPushNextUser() {
        // Given
        val userData = mockk<UserDb>(relaxed = true)
        every { databaseManagerUser.getUser(any()) } returnsMany listOf(listOf(userData), listOf(userData), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        // When
        SUT.pushUserData()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { databaseManagerUser.getUser(1) }
        verify(exactly = 2) { databaseManagerUser.deleteUsers(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoUserInDb_whenUserPush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { databaseManagerUser.getUser(any()) } returns emptyList()

        // When
        SUT.pushUserData()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

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
        advertisingId = null
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
        advertisingId = null
    )
    // endregion helper methods --------------------------------------------------------------------
}