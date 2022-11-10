package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.user.UserDTO
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import com.reteno.core.model.user.Address
import com.reteno.core.model.user.User
import com.reteno.core.model.user.UserAttributes
import com.reteno.core.model.user.UserCustomField
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert
import org.junit.Test

class ContactRepositoryTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN_NEW = "FCM_Token"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository
    @RelaxedMockK
    private lateinit var retenoDatabaseManager: RetenoDatabaseManager
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: ContactRepositoryImpl


    override fun before() {
        super.before()
        mockkObject(PushOperationQueue)
        SUT = ContactRepositoryImpl(apiClient, configRepository, retenoDatabaseManager)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
    }

    @Test
    fun whenSaveDeviceData_thenInsertDbAndPush() {
        // Given
        val device = getDevice()
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(
            listOf(device),
            emptyList()
        )

        // When
        SUT.saveDeviceData(device)

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify { retenoDatabaseManager.insertDevice(device) }
    }

    @Test
    fun whenDbHasSavedDeviceData_thenSendToApi() {
        // Given
        val device = getDevice()
        val expectedDeviceJson =
            "{\"deviceId\":\"device_ID\",\"externalUserId\":\"External_device_ID\",\"pushToken\":\"FCM_Token\",\"category\":\"MOBILE\",\"osType\":\"ANDROID\"}"

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
        val deviceData = mockk<Device>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(listOf(deviceData), listOf(deviceData), emptyList())
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
        val deviceData = mockk<Device>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returns listOf(deviceData)
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
        val deviceData = mockk<Device>(relaxed = true)
        every { retenoDatabaseManager.getDevices(any()) } returnsMany listOf(listOf(deviceData), listOf(deviceData), emptyList())
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
    fun whenSendUsedData_thenCallUserToRemoteMapper() {
        val user = User(
            userAttributes = UserAttributes(
                phone = "123",
                email = "email@gmail.com",
                firstName = null,
                lastName = null,
                languageCode = null,
                timeZone = null,
                address = null,
                fields = listOf()
            ),
            subscriptionKeys = listOf(),
            groupNamesInclude = listOf(),
            groupNamesExclude = listOf()
        )

        mockkStatic(User::toRemote)
        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { configRepository.getDeviceId() } returns deviceId

        SUT.saveUserData(user)

        verify { user.toRemote(deviceId) }

        unmockkStatic(User::toRemote)
    }

    @Test
    fun whenSaveUserAttributes_thenInsertDbAndPush() {
        // Given
        val user = getUser()
        every { configRepository.getDeviceId() } returns mockk(relaxed = true)
        val userDb = user.toRemote(mockk(relaxed = true))
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
            "{\"deviceId\":\"device_ID\",\"externalUserId\":\"External_device_ID\",\"userAttributes\":{\"phone\":\"380999360360\",\"email\":\"email@gmail.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"languageCode\":\"ua\",\"timeZone\":\"Kyiv\",\"address\":{\"region\":\"UA\",\"town\":\"Dnipro\",\"address\":\"Street, 7\",\"postcode\":\"45000\"},\"fields\":[{\"key\":\"key1\",\"value\":\"value\"},{\"key\":\"key2\",\"value\":\"true\"}]},\"subscriptionKeys\":[\"key1, key2\"],\"groupNamesInclude\":[\"add1\"],\"groupNamesExclude\":[\"remove1\"]}"

        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)
        every { configRepository.getDeviceId() } returns deviceId
        val userDb = getUser().toRemote(deviceId)
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
        val userData = mockk<UserDTO>(relaxed = true)
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
        val userData = mockk<UserDTO>(relaxed = true)
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
        val userData = mockk<UserDTO>(relaxed = true)
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

    private fun getUser(): User {
        return User(
            userAttributes = UserAttributes(
                phone = "380999360360",
                email = "email@gmail.com",
                firstName = "John",
                lastName = "Doe",
                languageCode = "ua",
                timeZone = "Kyiv",
                address = Address(
                    region = "UA",
                    town = "Dnipro",
                    address = "Street, 7",
                    postcode = "45000"
                ),
                fields = listOf(
                    UserCustomField("key1", "value"),
                    UserCustomField("key2", "true")
                )
            ),
            subscriptionKeys = listOf("key1, key2"),
            groupNamesInclude = listOf("add1"),
            groupNamesExclude = listOf("remove1")
        )
    }

    private fun getDevice() = Device(
        DEVICE_ID,
        EXTERNAL_DEVICE_ID,
        FCM_TOKEN_NEW,
        DeviceCategory.MOBILE,
        DeviceOS.ANDROID,
        null,
        null,
        null,
        null,
        null,
        null
    )
}