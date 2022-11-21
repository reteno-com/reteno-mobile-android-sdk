package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.user.User
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Test

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


        private val USER_SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")
        private val USER_GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactRepository: ContactRepository

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    private lateinit var SUT: ContactController
    // endregion helper fields ---------------------------------------------------------------------

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
        val user = User(
            userAttributes = null,
            subscriptionKeys = USER_SUBSCRIPTION_KEYS,
            groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
            groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
        )

        SUT.setUserData(user)

        verify { contactRepository.saveUserData(user) }
    }

    @Test
    fun whenPushDeviceData_thenInteractWithContactRepository() {
        SUT.pushDeviceData()
        verify { contactRepository.pushDeviceData() }
    }

    @Test
    fun whenPushUserData_thenInteractWithContactRepository() {
        SUT.pushUserData()
        verify { contactRepository.pushUserData() }
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