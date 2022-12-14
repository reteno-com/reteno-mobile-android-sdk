package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.user.User
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
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

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkObject(Device.Companion)
            mockDevice()
            mockkObject(Validator)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkObject(Device.Companion)
            unmockkObject(Validator)
        }

        private fun mockDevice() {
            every {
                Device.createDevice(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } answers {
                createDevice(
                    deviceId = firstArg(),
                    externalUserId = secondArg(),
                    pushToken = thirdArg(),
                    pushSubscribed = args[3] as Boolean?
                )
            }
        }

        private fun createDevice(
            deviceId: String,
            externalUserId: String?,
            pushToken: String?,
            pushSubscribed: Boolean?
        ) = Device(
            deviceId = deviceId,
            externalUserId = externalUserId,
            pushToken = pushToken,
            pushSubscribed = pushSubscribed,
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
        SUT = ContactController(contactRepository, configRepository)
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
    fun whenOnNewFcmToken_thenTokenSavedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID_ANDROID, null)

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID_ANDROID, null, FCM_TOKEN_NEW)
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice) }
    }

    @Test
    fun givenUserFull_whenSetUserData_thenInteractWithContactRepository() {
        // Given
        val user = User(
            userAttributes = null,
            subscriptionKeys = USER_SUBSCRIPTION_KEYS,
            groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
            groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
        )
        every { Validator.validateUser(user) } returns user

        // When
        SUT.setUserData(user)

        // Then
        verify(exactly = 1) { contactRepository.saveUserData(eq(user)) }
    }

    @Test
    fun givenUserNull_whenSetUserData_thenNoInteracttionWithContactRepository() {
        // When
        SUT.setUserData(null)

        // Then
        verify(exactly = 0) { contactRepository.saveUserData(any()) }
    }

    @Test
    fun givenUserValidationReturnsNull_whenSetUserData_thenNoInteracttionWithContactRepository() {
        // Given
        val user = User(
            userAttributes = null,
            subscriptionKeys = USER_SUBSCRIPTION_KEYS,
            groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
            groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
        )
        every { Validator.validateUser(any()) } returns null

        // When
        SUT.setUserData(user)

        // Then
        verify(exactly = 0) { contactRepository.saveUserData(any()) }
    }

    @Test
    fun whenPushDeviceData_thenInteractWithContactRepository() {
        // When
        SUT.pushDeviceData()

        // Then
        verify { contactRepository.pushDeviceData() }
    }

    @Test
    fun whenPushUserData_thenInteractWithContactRepository() {
        // When
        SUT.pushUserData()

        // Then
        verify { contactRepository.pushUserData() }
    }

    @Test
    fun givenNotificationsDisabled_whenEnableNotifications_thenDeviceSaved() {
        // Given
        val pushSubscribed = true
        every { configRepository.getNotificationsEnabled() } returns false
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(
            DEVICE_ID_ANDROID,
            EXTERNAL_DEVICE_ID
        )

        val expectedDevice =
            createDevice(DEVICE_ID_ANDROID, EXTERNAL_DEVICE_ID, FCM_TOKEN_NEW, pushSubscribed)

        // When
        SUT.notificationsEnabled(pushSubscribed)

        // Then
        verify(exactly = 1) { configRepository.saveNotificationsEnabled(pushSubscribed) }
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice) }
    }

    @Test
    fun givenNotificationsDisabled_whenDisableNotifications_thenNothingChanged() {
        // Given
        val pushSubscribed = false
        every { configRepository.getNotificationsEnabled() } returns false

        // When
        SUT.notificationsEnabled(pushSubscribed)

        // Then
        verify(exactly = 0) { configRepository.saveNotificationsEnabled(any()) }
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenNotificationsEnabled_whenEnableNotifications_thenNothingChanged() {
        // Given
        val pushSubscribed = true
        every { configRepository.getNotificationsEnabled() } returns true

        // When
        SUT.notificationsEnabled(pushSubscribed)

        // Then
        verify(exactly = 0) { configRepository.saveNotificationsEnabled(any()) }
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenNotificationsEnabled_whenDisableNotifications_thenDeviceSaved() {
        // Given
        val pushSubscribed = false
        every { configRepository.getNotificationsEnabled() } returns true
        every { configRepository.getFcmToken() } returns FCM_TOKEN_NEW
        every { configRepository.getDeviceId() } returns DeviceId(
            DEVICE_ID_ANDROID,
            EXTERNAL_DEVICE_ID
        )

        val expectedDevice =
            createDevice(DEVICE_ID_ANDROID, EXTERNAL_DEVICE_ID, FCM_TOKEN_NEW, pushSubscribed)

        // When
        SUT.notificationsEnabled(pushSubscribed)

        // Then
        verify(exactly = 1) { configRepository.saveNotificationsEnabled(pushSubscribed) }
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice) }
    }
}