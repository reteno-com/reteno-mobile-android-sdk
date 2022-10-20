package com.reteno.robolectric.core.data.local.config

import android.util.Log
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.util.SharedPrefsManager
import com.reteno.robolectric.BaseRobolectricTest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.robolectric.annotation.Config


@Config(sdk = [26])
class DeviceIdHelperTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID_UUID = "device_ID_UUID"
        const val DEVICE_ID_ANDROID = "device_ID_ANDROID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"


    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var sharedPrefsManager: SharedPrefsManager
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: DeviceIdHelperProxy


    override fun before() {
        super.before()
        MockKAnnotations.init(this)

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        SUT = DeviceIdHelperProxy(sharedPrefsManager)
    }

    override fun after() {
        super.after()
        unmockkStatic(Log::class)
    }

    @Test
    fun givenEmptyDeviceId_withDeviceIdMode_thenDeviceIdModeChanged() {
        // Given
        val oldDeviceIdMode = DeviceIdMode.RANDOM_UUID
        val newDeviceIdMode = DeviceIdMode.ANDROID_ID
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, null, oldDeviceIdMode)
        val expectedDeviceId = DeviceId(DEVICE_ID_ANDROID, null, newDeviceIdMode)

        every { sharedPrefsManager.getDeviceIdUuid() } returns DEVICE_ID_UUID

        // When
        lateinit var newDeviceId: DeviceId
        SUT.withDeviceIdMode(oldDeviceId, newDeviceIdMode) {
            newDeviceId = it
        }

        // Then
        assertEquals(expectedDeviceId, newDeviceId)
    }

    @Test
    fun givenEmptyDeviceId_withDeviceIdMode_thenDeviceIdChanged() {
        // Given
        val oldDeviceIdMode = DeviceIdMode.RANDOM_UUID
        val expectedDeviceIdMode = DeviceIdMode.ANDROID_ID
        val externalId = null
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, externalId, oldDeviceIdMode)
        every { sharedPrefsManager.getDeviceIdUuid() } returns DEVICE_ID_UUID

        // When
        lateinit var newDeviceId: DeviceId
        SUT.withDeviceIdMode(oldDeviceId, expectedDeviceIdMode) {
            newDeviceId = it
        }

        // Then
        val newId = DeviceIdHelperProxy.getId(newDeviceId)
        val newMode = DeviceIdHelperProxy.getMode(newDeviceId)
        val newExternalId = DeviceIdHelperProxy.getExternalId(newDeviceId)

        assertNotEquals(oldDeviceIdMode, expectedDeviceIdMode)
        assertNotEquals(DEVICE_ID_UUID, newId)
        assertEquals(expectedDeviceIdMode, newMode)
        assertEquals(externalId, newExternalId)
    }

    @Test
    fun givenExternalDeviceIdSaved_whenWithNullExternalDeviceId_thenExternalDeviceIdErased() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)

        // When
        val newDeviceId = SUT.withExternalDeviceId(oldDeviceId, null)

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, null)
        assertEquals(expectedDeviceId, newDeviceId)
    }

    @Test
    fun givenExternalDeviceIdSaved_whenWithEmptyExternalDeviceId_thenExternalDeviceIdErased() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)

        // When
        val newDeviceId = SUT.withExternalDeviceId(oldDeviceId, "")

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, null)
        assertEquals(expectedDeviceId, newDeviceId)
    }

    @Test
    fun givenNoExternalDeviceId_whenWithExternalDeviceId_thenExternalDeviceIdSaved() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, null)

        // When
        val newDeviceId = SUT.withExternalDeviceId(oldDeviceId, EXTERNAL_DEVICE_ID)

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)
        assertEquals(expectedDeviceId, newDeviceId)
    }

    // region helper methods -----------------------------------------------------------------------

    // endregion helper methods --------------------------------------------------------------------


    // region helper classes -----------------------------------------------------------------------

    // endregion helper classes --------------------------------------------------------------------
}