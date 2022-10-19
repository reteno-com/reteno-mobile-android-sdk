package com.reteno.core.domain.controller

import com.reteno.core.BaseUnitTest
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.ds.ConfigRepository
import com.reteno.core.data.remote.ds.ContactRepository
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Test


class ContactControllerTest : BaseUnitTest() {
    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN_OLD = "FCM_Token_OLD"
        private const val FCM_TOKEN_NEW = "FCM_Token"
    }

    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactRepository: ContactRepository

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: ContactController

    override fun before() {
        super.before()
        mockkObject(Device.Companion)
        mockDevice()
        SUT = ContactController(contactRepository, configRepository)
    }


    override fun after() {
        super.after()
        mockkObject(Device.Companion)
    }

    @Test
    fun givenPushTokenNotAvailable_whenSetExternalDeviceId_thenContactNotSent() {
        // Given
        every { configRepository.getFcmToken() } returns ""
        every { configRepository.changeDeviceIdMode(any(), any()) } answers {
            val callback: () -> Unit = secondArg()
            callback.invoke()
        }

        // When
        SUT.setExternalDeviceId(EXTERNAL_DEVICE_ID)

        // Then
        verify(exactly = 0) { contactRepository.sendDeviceProperties(any(), any()) }
    }

    @Test
    fun givenPushTokenAvailable_whenSetExternalDeviceId_thenContactSent() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getExternalId() } returns EXTERNAL_DEVICE_ID
        every { configRepository.changeDeviceIdMode(any(), any()) } answers {
            val callback: () -> Unit = secondArg()
            callback.invoke()
        }

        // When
        SUT.setExternalDeviceId(EXTERNAL_DEVICE_ID)

        // Then
        val expectedDevice = Device.createDevice(DEVICE_ID, EXTERNAL_DEVICE_ID, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.sendDeviceProperties(eq(expectedDevice), any()) }
    }

    @Test
    fun givenPushTokenNotAvailable_whenChangeDeviceIdMode_thenContactNotSent() {
        // Given
        every { configRepository.getFcmToken() } returns ""
        every { configRepository.changeDeviceIdMode(any(), any()) } answers {
            val callback: () -> Unit = secondArg()
            callback.invoke()
        }

        // When
        SUT.changeDeviceIdMode(DeviceIdMode.ANDROID_ID) {}

        // Then
        verify(exactly = 0) { contactRepository.sendDeviceProperties(any(), any()) }
    }

    @Test
    fun givenPushTokenAvailable_whenChangeDeviceIdMode_thenContactSent() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getExternalId() } returns null
        every { configRepository.changeDeviceIdMode(any(), any()) } answers {
            val callback: () -> Unit = secondArg()
            callback.invoke()
        }

        // When
        SUT.changeDeviceIdMode(DeviceIdMode.ANDROID_ID) {}

        // Then
        val expectedDevice = Device.createDevice(DEVICE_ID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.sendDeviceProperties(eq(expectedDevice), any()) }
    }

    @Test
    fun givenTokenAlreadySaved_whenOnNewFcmToken_thenNothingHappens() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 0) { configRepository.saveFcmToken(any()) }
        verify(exactly = 0) { contactRepository.sendDeviceProperties(any(), any()) }
    }

    @Test
    fun givenTokenAbsent_whenOnNewFcmToken_thenTokenSavedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken() } returns "" andThen FCM_TOKEN_NEW
        every { configRepository.getExternalId() } returns null

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.sendDeviceProperties(expectedDevice, any()) }
    }

    @Test
    fun givenTokenAvailable_whenOnNewFcmToken_thenTokenUpdatedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_OLD andThen FCM_TOKEN_NEW
        every { configRepository.getExternalId() } returns null
        every { contactRepository.sendDeviceProperties(any(), any()) } just runs

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.sendDeviceProperties(expectedDevice, any()) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockDevice() {
        every { configRepository.getDeviceId() } returns DEVICE_ID

        every {
            Device.createDevice(
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val deviceId = firstArg<String>()
            val externalUserId = secondArg<String?>()
            val pushToken = thirdArg<String?>()
            Device(
                deviceId = deviceId,
                externalUserId = externalUserId,
                pushToken = pushToken,
                category = DeviceCategory.MOBILE,
                osType = DeviceOS.ANDROID,
                osVersion = null,
                deviceModel = null,
                appVersion = null,
                languageCode = null,
                timeZone = null,
                advertisingId = null
            )
        }
    }
    // endregion helper methods --------------------------------------------------------------------


    // region helper classes -----------------------------------------------------------------------

    // endregion helper classes --------------------------------------------------------------------
}