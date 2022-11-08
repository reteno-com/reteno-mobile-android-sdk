package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.model.device.Device
import com.reteno.core.model.device.DeviceCategory
import com.reteno.core.model.device.DeviceOS
import com.reteno.core.model.user.Address
import com.reteno.core.model.user.User
import com.reteno.core.model.user.UserAttributes
import com.reteno.core.model.user.UserCustomField
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Test

// TODO review later (B.S.)
class ContactControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID_ANDROID = "device_ID_ANDROID"
        private const val DEVICE_ID_APP_SET = "device_ID_APP_SET"
        private const val DEVICE_ID_UUID = "device_ID_UUID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val EXTERNAL_DEVICE_ID_NEW = "External_device_ID_NEW"
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
        unmockkObject(Device.Companion)
    }

    @Test
    fun givenPushTokenNotAvailable_whenSetExternalDeviceId_thenContactNotSent() {
        // Given
        every { configRepository.getFcmToken() } returns ""

        // When
        SUT.setExternalUserId(EXTERNAL_DEVICE_ID)

        // Then
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenPushTokenAvailable_whenSetExternalDeviceId_thenContactSent() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(
            DEVICE_ID_ANDROID,
            EXTERNAL_DEVICE_ID
        )

        // When
        SUT.setExternalUserId(EXTERNAL_DEVICE_ID_NEW)

        // Then
        val expectedDevice =
            Device.createDevice(DEVICE_ID_ANDROID, EXTERNAL_DEVICE_ID, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(eq(expectedDevice)) }
    }

    @Test
    fun givenPushTokenAvailableAndExternalIdWasSet_whenSetEqualsExternalDeviceId_thenContactDoesNotSent() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(
            DEVICE_ID_ANDROID,
            EXTERNAL_DEVICE_ID
        )

        // When
        SUT.setExternalUserId(EXTERNAL_DEVICE_ID)

        // Then
        val expectedDevice =
            Device.createDevice(DEVICE_ID_ANDROID, EXTERNAL_DEVICE_ID, FCM_TOKEN_NEW)
        verify(exactly = 0) { contactRepository.saveDeviceData(eq(expectedDevice)) }
    }

    @Test
    fun givenPushTokenNotAvailable_whenChangeDeviceIdMode_thenContactNotSent() {
        // Given
        every { configRepository.getFcmToken() } returns ""
        every { configRepository.setDeviceIdMode(any(), any()) } answers {
            val callback: (DeviceId) -> Unit = secondArg()
            callback.invoke(DeviceId(DEVICE_ID_UUID, null, DeviceIdMode.RANDOM_UUID))
        }

        // When
        SUT.setDeviceIdMode(DeviceIdMode.RANDOM_UUID) {}

        // Then
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenPushTokenAvailable_whenChangeDeviceIdMode_thenContactSent() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_ANDROID, null, DeviceIdMode.ANDROID_ID)
        val newDeviceId = DeviceId(DEVICE_ID_UUID, null, DeviceIdMode.RANDOM_UUID)

        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns oldDeviceId
        every { configRepository.setDeviceIdMode(any(), any()) } answers {
            every { configRepository.getDeviceId() } returns newDeviceId
            val callback: (DeviceId) -> Unit = secondArg()
            callback.invoke(newDeviceId)
        }

        // When
        SUT.setDeviceIdMode(DeviceIdMode.RANDOM_UUID) {}

        // Then
        val expectedDevice = Device.createDevice(DEVICE_ID_UUID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(eq(expectedDevice)) }
    }

    @Test
    fun givenPushTokenAvailable_whenNotChangeDeviceIdMode_thenContactSentOneTime() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_ANDROID, null, DeviceIdMode.ANDROID_ID)
        val newDeviceId = DeviceId(DEVICE_ID_UUID, null, DeviceIdMode.ANDROID_ID)

        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns oldDeviceId
        every { configRepository.setDeviceIdMode(any(), any()) } answers {
            every { configRepository.getDeviceId() } returns newDeviceId
            val callback: (DeviceId) -> Unit = secondArg()
            callback.invoke(newDeviceId)
        }

        // When
        SUT.setDeviceIdMode(DeviceIdMode.ANDROID_ID) {}

        // Then
        val expectedDevice = Device.createDevice(DEVICE_ID_UUID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(eq(expectedDevice)) }
    }

    @Test
    fun givenTokenAlreadySaved_whenOnNewFcmToken_thenNothingHappens() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 0) { configRepository.saveFcmToken(any()) }
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenTokenAbsent_whenOnNewFcmToken_thenTokenSavedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken() } returns "" andThen FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID_ANDROID, null)

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID_ANDROID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice) }
    }

    @Test
    fun givenTokenAvailable_whenOnNewFcmToken_thenTokenUpdatedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_OLD andThen FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID_ANDROID, null)
        every { contactRepository.saveDeviceData(any()) } just runs

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID_ANDROID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice) }
    }

    @Test
    fun whenSetUserData_thenInteractWithContactRepository() {
        val user = mockk<User>()

        SUT.setUserData(user)

        verify { contactRepository.saveUserData(user) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockDevice() {
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
}