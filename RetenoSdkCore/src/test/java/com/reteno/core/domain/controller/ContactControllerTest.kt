package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.domain.Validator
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.device.DeviceCategory
import com.reteno.core.domain.model.device.DeviceOS
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.util.Logger
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class ContactControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID_ANDROID = "device_ID_ANDROID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val EXTERNAL_DEVICE_ID_NEW = "External_device_ID_NEW"
        private const val FCM_TOKEN_OLD = "FCM_Token_OLD"
        private const val FCM_TOKEN_NEW = "FCM_Token"


        private val USER_SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")
        private val USER_GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")

        private const val FIRST_NAME = "firstName1"
        private const val LAST_NAME = "lastName1"
        private const val LANGUAGE_CODE = "languageCode1"
        private const val TIME_ZONE = "timeZone1"
        private const val FIELD_KEY1 = "key1"
        private const val FIELD_KEY2 = "key2"
        private const val FIELD_KEY3 = "key3"
        private const val FIELD_VALUE1 = "value1"
        private const val FIELD_VALUE2 = "value2"

        private const val REGION = "region1"
        private const val TOWN = "town1"
        private const val ADDRESS = "address1"
        private const val POSTCODE = "postcode1"

        private val addressFull = Address(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        private val customFieldsFull = listOf(
            UserCustomField(FIELD_KEY1, FIELD_VALUE1),
            UserCustomField(FIELD_KEY2, FIELD_VALUE2),
            UserCustomField(FIELD_KEY3, null)
        )

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
                    pushSubscribed = args[3] as Boolean?,
                    email = args[5] as? String,
                    phone = args[6] as? String
                )
            }
        }

        private fun createDevice(
            deviceId: String,
            externalUserId: String?,
            pushToken: String?,
            pushSubscribed: Boolean? = null,
            email: String? = null,
            phone: String? = null
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
            advertisingId = null,
            email = email,
            phone = phone
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
    fun givenPushTokenAvailable_whenSetExternalDeviceId_thenContactSent() {
        // Given
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW)
        }
        every { configRepository.getDeviceId() } returns DeviceId(
            DEVICE_ID_ANDROID,
            EXTERNAL_DEVICE_ID
        )

        // When
        SUT.setExternalUserId(EXTERNAL_DEVICE_ID_NEW)

        // Then
        val expectedDevice =
            Device.createDevice(
                DEVICE_ID_ANDROID,
                EXTERNAL_DEVICE_ID,
                FCM_TOKEN_NEW,
                configRepository.isNotificationsEnabled()
            )
        verify(exactly = 1) { contactRepository.saveDeviceData(eq(expectedDevice), eq(false)) }
    }

    @Test
    fun givenPushTokenAvailableAndExternalIdWasSet_whenSetEqualsExternalDeviceId_thenContactDoesNotSent() {
        // Given
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW)
        }
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
    fun whenOnNewFcmToken_andTokenSame_thenTokenNotSavedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW)
        }
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID_ANDROID, null)

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 0) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(DEVICE_ID_ANDROID, null, FCM_TOKEN_NEW)
        verify(exactly = 0) { contactRepository.saveDeviceData(expectedDevice) }
    }

    @Test
    fun whenOnNewFcmToken_andTokenDifferent_thenTokenSavedDeviceUpdated() {
        // Given
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW + "different")
        }
        every { configRepository.getDeviceId() } returns DeviceId(DEVICE_ID_ANDROID, null)

        // When
        SUT.onNewFcmToken(FCM_TOKEN_NEW)

        // Then
        verify(exactly = 1) { configRepository.saveFcmToken(FCM_TOKEN_NEW) }
        val expectedDevice = Device.createDevice(
            DEVICE_ID_ANDROID,
            null,
            FCM_TOKEN_NEW,
            configRepository.isNotificationsEnabled()
        )
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice, false) }
    }

    @Test
    fun givenUserFull_whenSetUserData_thenContactRepositoryCalled() {
        // Given
        val user = getUser()
        every { Validator.validateUser(user) } returns user

        // When
        SUT.setUserData(user)

        // Then
        verify(exactly = 1) { contactRepository.saveUserData(eq(user), false) }
    }

    @Test
    fun givenUserNull_whenSetUserData_thenContactRepositoryNotCalled() {
        // Given
        val user = null

        // When
        SUT.setUserData(user)

        // Then
        verify(exactly = 0) { contactRepository.saveUserData(any()) }
    }

    @Test
    fun givenInvalidUser_whenSetUserData_thenContactRepositoryNotCalled() {
        val user = getUser()
        every { Validator.validateUser(user) } returns null

        // When
        SUT.setUserData(user)

        // Then
        verify(exactly = 0) { contactRepository.saveUserData(any()) }
        verify(exactly = 1) { Logger.captureMessage(eq("ContactController.setUserData(): user = [$user]")) }
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
        every { configRepository.isNotificationsEnabled() } returns false
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW)
        }
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
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice, any()) }
    }

    @Test
    fun givenNotificationsDisabled_whenDisableNotifications_thenEventNotResend_andDeviceIsNotUpdated() {
        // Given
        val pushSubscribed = false
        every { configRepository.isNotificationsEnabled() } returns false

        // When
        SUT.notificationsEnabled(pushSubscribed)

        // Then
        verify(exactly = 0) { configRepository.saveNotificationsEnabled(any()) }
        verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
    }

    @Test
    fun givenNotificationsEnabled_whenEnableNotifications_thenEventNotResend_andDeviceIsNotUpdated() {
        // Given
        val pushSubscribed = true
        every { configRepository.isNotificationsEnabled() } returns true

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
        every { configRepository.isNotificationsEnabled() } returns true
        every { configRepository.getFcmToken(any()) } answers {
            val callback = arg<((String) -> Unit)>(0)
            callback.invoke(FCM_TOKEN_NEW)
        }
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
        verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice, any()) }
    }

    @Test
    fun givenIsDeviceRegisteredFalse_whenCheckIfDeviceRegistered_thenSaveDeviceDataCalled() =
        runTest {
            // Given
            every { configRepository.isDeviceRegistered() } returns false
            every { configRepository.getFcmToken(any()) } answers {
                val callback = arg<((String) -> Unit)>(0)
                callback.invoke(FCM_TOKEN_NEW)
            }
            every { configRepository.getDeviceId() } returns DeviceId(
                DEVICE_ID_ANDROID,
                EXTERNAL_DEVICE_ID
            )
            val expectedDevice =
                createDevice(
                    DEVICE_ID_ANDROID,
                    EXTERNAL_DEVICE_ID,
                    FCM_TOKEN_NEW,
                    configRepository.isNotificationsEnabled()
                )

            // When
            SUT.checkIfDeviceRegistered()

            // Then
            verify(exactly = 1) { contactRepository.saveDeviceData(expectedDevice, false) }
        }

    @Test
    fun givenIsDeviceRegisteredTrue_whenCheckIfDeviceRegistered_thenSaveDeviceDataNotCalled() =
        runTest {
            // Given
            every { configRepository.isDeviceRegistered() } returns true

            // When
            SUT.checkIfDeviceRegistered()

            // Then
            verify(exactly = 0) { contactRepository.saveDeviceData(any()) }
        }

    @Test
    fun givenValidAnonymousUserAttributes_whenSetAnonymousUserAttributes_thenRepositorySaveAnonymousUserData() {
        // Given
        val anonymousUserAttributes = getUserAttributesAnonymous()
        every { Validator.validateAnonymousUserAttributes(anonymousUserAttributes) } returns anonymousUserAttributes
        val expectedUserData = User(anonymousUserAttributes.toUserAttributes())

        // When
        SUT.setAnonymousUserAttributes(anonymousUserAttributes)

        // Then
        verify(exactly = 1) { contactRepository.saveUserData(eq(expectedUserData), any()) }
    }

    @Test
    fun givenInvalidAnonymousUserAttributes_whenSetAnonymousUserAttributes_thenRepositorySaveAnonymousUserDataNotCalled() {
        // Given
        val anonymousUserAttributes = getUserAttributesAnonymous()
        every { Validator.validateAnonymousUserAttributes(anonymousUserAttributes) } returns null

        // When
        SUT.setAnonymousUserAttributes(anonymousUserAttributes)

        // Then
        verify(exactly = 0) { contactRepository.saveUserData(any()) }
        verify(exactly = 1) { Logger.captureMessage(eq("setAnonymousUserAttributes(): attributes = [$anonymousUserAttributes]")) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getUser() = User(
        userAttributes = null,
        subscriptionKeys = USER_SUBSCRIPTION_KEYS,
        groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
        groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
    )

    private fun getUserAttributesAnonymous() = UserAttributesAnonymous(
        firstName = FIRST_NAME,
        lastName = LAST_NAME,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        address = addressFull,
        fields = customFieldsFull
    )
    // endregion helper methods --------------------------------------------------------------------
}