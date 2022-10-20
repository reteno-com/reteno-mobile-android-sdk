package com.reteno.core.data.repository

import android.util.Log
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Matchers.anyString
import org.robolectric.annotation.Config

@Config(sdk = [26])
class ConfigRepositoryTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "device_ID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
        private const val FCM_TOKEN = "FCM_Token"
        private const val DEFAULT_NOTIFICATION_CHANNEL = "default_notification_channel_ID"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @MockK
    private lateinit var sharedPrefsManager: SharedPrefsManager

    private lateinit var restConfig: RestConfig
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: ConfigRepositoryImplProxy


    override fun before() {
        super.before()
        MockKAnnotations.init(this)

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        restConfig = spyk(RestConfig(DeviceIdHelper(sharedPrefsManager)), recordPrivateCalls = true)
        SUT = ConfigRepositoryImplProxy(sharedPrefsManager, restConfig)
    }

    override fun after() {
        super.after()
        unmockkStatic(Log::class)
    }

    @Test
    fun given_whenSetExternalDeviceId_thenDelegatedToRestConfig() {
        // Given
        every { restConfig["setExternalDeviceId"](anyString()) } returns Unit

        // When
        SUT.setExternalDeviceId(EXTERNAL_DEVICE_ID)

        // Then
        verify(exactly = 1) { restConfig["setExternalDeviceId"](EXTERNAL_DEVICE_ID) }
    }

    @Test
    fun given_whnSetDeviceIdMode_thenDelegatedToRestConfig() {
        // Given
        val deviceIdMode = DeviceIdMode.RANDOM_UUID
        every { restConfig["setDeviceIdMode"](ofType(DeviceIdMode::class), ofType(Function1::class)) } returns Unit

        // When
        SUT.setDeviceIdMode(deviceIdMode) {}

        // Then
        verify(exactly = 1) { restConfig["setDeviceIdMode"](DeviceIdMode.RANDOM_UUID, ofType(Function1::class)) }
    }

    @Test
    fun given_whenGetDeviceId_thenDelegatedToRestConfig() {
        // Given
        val deviceId = DeviceId(DEVICE_ID, EXTERNAL_DEVICE_ID, DeviceIdMode.RANDOM_UUID)
        every { restConfig.deviceId } returns deviceId

        // When
        val result = SUT.deviceId

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
    fun given_whenGetFcmToken_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.getFcmToken() } returns FCM_TOKEN

        // When
        val result = SUT.fcmToken

        // Then
        verify(exactly = 1) { sharedPrefsManager.getFcmToken() }
        assertEquals(FCM_TOKEN, result)
    }

    @Test
    fun given_whenSaveDefaultNotificationChannel_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.saveDefaultNotificationChannel(any()) } just runs

        // When
        SUT.saveDefaultNotificationChannel(DEFAULT_NOTIFICATION_CHANNEL)

        // Then
        verify(exactly = 1) { sharedPrefsManager.saveDefaultNotificationChannel(DEFAULT_NOTIFICATION_CHANNEL) }
    }

    @Test
    fun given_whenGetDefaultNotificationChannel_thenDelegatedToSharedPrefsManager() {
        // Given
        every { sharedPrefsManager.getDefaultNotificationChannel() } returns DEFAULT_NOTIFICATION_CHANNEL

        // When
        val result = SUT.defaultNotificationChannel

        // Then
        verify(exactly = 1) { sharedPrefsManager.getDefaultNotificationChannel() }
        assertEquals(DEFAULT_NOTIFICATION_CHANNEL, result)
    }
}