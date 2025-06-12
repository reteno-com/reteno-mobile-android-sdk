package com.reteno.core.data.repository

import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessaging
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class ConfigRepositoryImplTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN = "FCM_Token"
        private const val DEFAULT_NOTIFICATION_CHANNEL = "default_notification_channel_ID"

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic(TextUtils::class)
            mockkStatic(FirebaseMessaging::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(TextUtils::class)
            unmockkStatic(FirebaseMessaging::class)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @MockK
    private lateinit var sharedPrefsManager: SharedPrefsManager

    @RelaxedMockK
    private lateinit var restConfig: RestConfig

    private lateinit var SUT: ConfigRepository
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        coEvery { sharedPrefsManager.isNotificationsEnabled() } returns false
        coEvery { sharedPrefsManager.isDeviceRegistered() } returns false
        coEvery { sharedPrefsManager.setFirstLaunch(any()) } returns Unit
        SUT = ConfigRepositoryImpl(mockk(), sharedPrefsManager, restConfig)
    }

    @Test
    fun given_whenSetExternalDeviceId_thenDelegatedToRestConfig() {
        // Given
        every { restConfig["setExternalUserId"](anyString()) } returns Unit

        // When
        SUT.setExternalUserId(EXTERNAL_DEVICE_ID)

        // Then
        verify(exactly = 1) { restConfig["setExternalUserId"](EXTERNAL_DEVICE_ID) }
    }

    @Test
    fun given_whenGetDeviceId_thenDelegatedToRestConfig() {
        // Given
        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID, DeviceIdMode.RANDOM_UUID)
        every { restConfig.deviceId } returns deviceId

        // When
        val result = SUT.getDeviceId()

        // Then
        verify(exactly = 1) { restConfig.deviceId }
        assertEquals(deviceId, result)
    }

    @Test
    fun given_whenSaveFcmToken_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.saveFcmToken(any()) } just runs

        // When
        SUT.saveFcmToken(FCM_TOKEN)

        // Then
        verify(exactly = 1) { sharedPrefsManager.saveFcmToken(FCM_TOKEN) }
    }

    @Test
    fun given_whenGetFcmToken_thenDelegatedToSharedPrefsManager() = runTest {
        // Given
        every { TextUtils.isEmpty(any()) } returns false
        every { sharedPrefsManager.getFcmToken() } returns FCM_TOKEN

        // When
        val token = SUT.getFcmToken()
        assertEquals(FCM_TOKEN, token)

        // Then
        verify(exactly = 1) { sharedPrefsManager.getFcmToken() }
    }

    @Test
    fun given_whenSaveDeviceRegistered_thenDelegatedToSharedPrefsManager() {
        // Given
        justRun { sharedPrefsManager.saveDeviceRegistered(any()) }
        val isDeviceRegistered = false

        // When
        SUT.saveDeviceRegistered(isDeviceRegistered)

        // Then
        verify(exactly = 1) { sharedPrefsManager.saveDeviceRegistered(isDeviceRegistered) }
    }

    @Test
    fun given_whenIsDeviceRegistered_thenDelegatedToSharedPrefsManager() {
        // Given
        val isDeviceRegistered = false
        every { sharedPrefsManager.isDeviceRegistered() } returns isDeviceRegistered

        // When
        val actualResult = SUT.isDeviceRegistered()

        // Then
        verify(exactly = 1) { sharedPrefsManager.isDeviceRegistered() }
        assertEquals(isDeviceRegistered, actualResult)
    }

    @Test
    fun givenFcmTokenAbsent_whenGetFcmToken_thenGetAndSaveFreshFcmTokenCalled() = runTest {
        // Given
        every { TextUtils.isEmpty(any()) } returns true
        val firebaseMockk = mockk<FirebaseMessaging>(relaxed = true)
        every { firebaseMockk.isAutoInitEnabled } returns false
        every { FirebaseMessaging.getInstance() } returns firebaseMockk

        every { sharedPrefsManager.getFcmToken() } returns ""
        every { sharedPrefsManager.saveFcmToken(anyString()) } just runs

        // When
        val configRepositorySpy = spyk(SUT, recordPrivateCalls = true)
        configRepositorySpy.getFcmToken()

        // Then
        verify(exactly = 1) { sharedPrefsManager.getFcmToken() }
        verify(exactly = 1) { configRepositorySpy["getAndSaveFreshFcmToken"]() }
    }

    @Test
    fun givenFcmTokenPresent_whenGetFcmToken_thenGetAndSaveFreshFcmTokenCalled() = runTest {
        // Given
        every { TextUtils.isEmpty(any()) } returns true
        val firebaseMockk = mockk<FirebaseMessaging>(relaxed = true)
        every { firebaseMockk.isAutoInitEnabled } returns false
        every { FirebaseMessaging.getInstance() } returns firebaseMockk

        every { sharedPrefsManager.getFcmToken() } returns FCM_TOKEN
        every { sharedPrefsManager.saveFcmToken(anyString()) } just runs

        // When
        val configRepositorySpy = spyk(SUT, recordPrivateCalls = true)
        configRepositorySpy.getFcmToken()

        // Then
        verify(exactly = 1) { sharedPrefsManager.getFcmToken() }
        verify(exactly = 0) { configRepositorySpy["getAndSaveFreshFcmToken"]() }
    }

    @Test
    fun given_whenSaveDefaultNotificationChannel_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.saveDefaultNotificationChannel(any()) } just runs

        // When
        SUT.saveDefaultNotificationChannel(DEFAULT_NOTIFICATION_CHANNEL)

        // Then
        verify(exactly = 1) {
            sharedPrefsManager.saveDefaultNotificationChannel(
                DEFAULT_NOTIFICATION_CHANNEL
            )
        }
    }

    @Test
    fun given_whenGetDefaultNotificationChannel_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.getDefaultNotificationChannel() } returns DEFAULT_NOTIFICATION_CHANNEL

        // When
        val result = SUT.getDefaultNotificationChannel()

        // Then
        verify(exactly = 1) { sharedPrefsManager.getDefaultNotificationChannel() }
        assertEquals(DEFAULT_NOTIFICATION_CHANNEL, result)
    }
}