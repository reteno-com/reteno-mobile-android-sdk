package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toRemote
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


class ContactRepositoryTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN_NEW = "FCM_Token"

        private val EXPECTED_API_CONTRACT_URL = ApiContract.MobileApi.Device.url
        private const val EXPECTED_URL = "https://mobile-api.reteno.com/api/v1/device"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var restConfig: RestConfig
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: ContactRepositoryImpl


    override fun before() {
        super.before()
        SUT = ContactRepositoryImpl(apiClient, restConfig)
    }

    @Test
    fun sendDeviceProperties() {
        // Given
        val device = Device(
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
        val expectedDeviceJson =
            "{\"deviceId\":\"device_ID\",\"externalUserId\":\"External_device_ID\",\"pushToken\":\"FCM_Token\",\"category\":\"MOBILE\",\"osType\":\"ANDROID\"}"

        val apiContractCaptured = slot<ApiContract>()
        val jsonBodyCaptured = slot<String>()
        every {
            apiClient.post(
                url = capture(apiContractCaptured),
                jsonBody = capture(jsonBodyCaptured),
                responseHandler = any()
            )
        } just runs

        // When
        SUT.sendDeviceProperties(device, object : ResponseCallback {
            override fun onSuccess(response: String) {}
            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {}
        })

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }

        Assert.assertEquals(EXPECTED_API_CONTRACT_URL, EXPECTED_URL)
        Assert.assertEquals(EXPECTED_API_CONTRACT_URL, apiContractCaptured.captured.url)
        Assert.assertEquals(expectedDeviceJson, jsonBodyCaptured.captured)
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
        every { restConfig.deviceId } returns deviceId

        SUT.sendUserData(user, object : ResponseCallback {
            override fun onSuccess(response: String) {}
            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {}
        })

        verify { user.toRemote(deviceId) }

        unmockkStatic(User::toRemote)
    }

    @Test
    fun sendUserData() {
        // Given
        val user = User(
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

        val expectedUserJson = "{\"deviceId\":\"device_ID\",\"externalUserId\":\"External_device_ID\",\"userAttributes\":{\"phone\":\"380999360360\",\"email\":\"email@gmail.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"languageCode\":\"ua\",\"timeZone\":\"Kyiv\",\"address\":{\"region\":\"UA\",\"town\":\"Dnipro\",\"address\":\"Street, 7\",\"postcode\":\"45000\"},\"fields\":[{\"key\":\"key1\",\"value\":\"value\"},{\"key\":\"key2\",\"value\":\"true\"}]},\"subscriptionKeys\":[\"key1, key2\"],\"groupNamesInclude\":[\"add1\"],\"groupNamesExclude\":[\"remove1\"]}"

        every { restConfig.deviceId } returns DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID)

        val apiContractCaptured = slot<ApiContract>()
        val jsonBodyCaptured = slot<String>()
        justRun {
            apiClient.post(
                url = capture(apiContractCaptured),
                jsonBody = capture(jsonBodyCaptured),
                responseHandler = any()
            )
        }

        // When
        SUT.sendUserData(user, object : ResponseCallback {
            override fun onSuccess(response: String) {}
            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {}
        })

        // Then
        verify(exactly = 1) { apiClient.post(any(), any(), any()) }

        Assert.assertEquals(ApiContract.MobileApi.User.url, apiContractCaptured.captured.url)
        Assert.assertEquals(expectedUserJson, jsonBodyCaptured.captured)
    }
}