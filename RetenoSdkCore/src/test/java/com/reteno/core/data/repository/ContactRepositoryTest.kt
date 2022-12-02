package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.RetenoDatabaseManager
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
import org.junit.Test

class ContactRepositoryTest : BaseUnitTest() {

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
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository
    @RelaxedMockK
    private lateinit var retenoDatabaseManager: RetenoDatabaseManager

    private lateinit var SUT: ContactRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockOperationQueue()
        mockkObject(PushOperationQueue)
        SUT = ContactRepositoryImpl(apiClient, configRepository, retenoDatabaseManager)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
        unMockOperationQueue()
    }

    @Test
    fun whenSaveDeviceData_thenInsertDbAndPush() {
        // Given
        val device = getDevice()
        val deviceDb = getDeviceDb()
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(
            listOf(deviceDb),
            emptyList()
        )

        // When
        SUT.saveDeviceData(device)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify { retenoDatabaseManager.insertDevice(deviceDb) }
    }

    @Test
    fun whenDbHasSavedDeviceData_thenSendToApi() {
        // Given
        val device = getDeviceDb()
        val expectedDeviceJson =
            "{\"deviceId\":\"${DEVICE_ID}\",\"externalUserId\":\"${EXTERNAL_DEVICE_ID}\",\"pushToken\":\"${FCM_TOKEN_NEW}\",\"category\":\"${CATEGORY}\",\"osType\":\"${OS_TYPE}\"}"

        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(
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
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(listOf(deviceDataDb), listOf(deviceDataDb), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        SUT.pushDeviceData()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { retenoDatabaseManager.deleteDevices(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenDevicePushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returns listOf(deviceDataDb)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        SUT.pushDeviceData()

        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun whenDevicePushFailedAndErrorIsNonRepeatable_thenTryPushNextDevice() {
        val deviceDataDb = mockk<DeviceDb>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(listOf(deviceDataDb), listOf(deviceDataDb), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        SUT.pushDeviceData()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { retenoDatabaseManager.getDevices(1) }
        verify(exactly = 2) { retenoDatabaseManager.deleteDevices(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoDeviceInDb_whenDevicePush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { retenoDatabaseManager.getDevices(any()) } returns emptyList()

        // When
        SUT.pushDeviceData()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }

    }

    @Test
    fun whenSendUsedData_thenCallUserToDbMapper() {
        val user = getUser()

        mockkStatic(User::toDb)
        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { configRepository.getDeviceId() } returns deviceId

        SUT.saveUserData(user)

        verify { user.toDb(deviceId) }

        unmockkStatic(User::toDb)
    }

    @Test
    fun whenSaveUserAttributes_thenInsertDbAndPush() {
        // Given
        val user = getUser()
        every { configRepository.getDeviceId() } returns mockk(relaxed = true)
        val userDb = user.toDb(mockk(relaxed = true))
        every { retenoDatabaseManager.getUser(any()) } returnsMany listOf(
            listOf(userDb),
            emptyList()
        )

        // When
        SUT.saveUserData(user)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify { retenoDatabaseManager.insertUser(userDb) }
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
        every { retenoDatabaseManager.getUser(any()) } returnsMany listOf(
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
        val userData = mockk<UserDb>(relaxed = true)
        every { retenoDatabaseManager.getUser(any()) } returnsMany listOf(listOf(userData), listOf(userData), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        SUT.pushUserData()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { retenoDatabaseManager.deleteUsers(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun whenUserPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val userData = mockk<UserDb>(relaxed = true)
        every { retenoDatabaseManager.getUser(any()) } returns listOf(userData)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(500, null, null)
        }

        SUT.pushUserData()

        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun whenUserPushFailedAndErrorIsNonRepeatable_thenTryPushNextUser() {
        val userData = mockk<UserDb>(relaxed = true)
        every { retenoDatabaseManager.getUser(any()) } returnsMany listOf(listOf(userData), listOf(userData), emptyList())
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(400, null, null)
        }

        SUT.pushUserData()

        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { retenoDatabaseManager.getUser(1) }
        verify(exactly = 2) { retenoDatabaseManager.deleteUsers(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoUserInDb_whenUserPush_thenApiClientPutsDoesNotCalled() {
        // Given
        every { retenoDatabaseManager.getUser(any()) } returns emptyList()

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