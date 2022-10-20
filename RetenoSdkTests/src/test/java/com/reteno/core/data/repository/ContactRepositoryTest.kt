package com.reteno.core.data.repository

import com.reteno.core.BaseUnitTest
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
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
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: ContactRepositoryImplProxy


    override fun before() {
        super.before()
        SUT = ContactRepositoryImplProxy(apiClient)
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
}