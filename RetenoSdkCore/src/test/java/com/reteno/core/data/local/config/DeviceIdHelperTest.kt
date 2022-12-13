package com.reteno.core.data.local.config

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.base.robolectric.Constants.DEVICE_ID_ANDROID
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.robolectric.annotation.Config


@Config(sdk = [26])
class DeviceIdHelperTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID_UUID = "device_ID_UUID"
        private const val EXTERNAL_DEVICE_ID = "External_device_ID"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var sharedPrefsManager: SharedPrefsManager
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: DeviceIdHelper


    override fun before() {
        super.before()
        MockKAnnotations.init(this)

        SUT = DeviceIdHelper(sharedPrefsManager)
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
        assertNotEquals(oldDeviceIdMode, expectedDeviceIdMode)
        assertNotEquals(DEVICE_ID_UUID, newDeviceId.id)
        assertEquals(expectedDeviceIdMode, newDeviceId.mode)
        assertEquals(externalId, newDeviceId.externalId)
    }

    @Test
    fun givenExternalDeviceIdSaved_whenWithNullExternalDeviceId_thenExternalDeviceIdIsNull() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)

        // When
        val newDeviceId = SUT.withExternalUserId(oldDeviceId, null)

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, null)
        assertEquals(expectedDeviceId, newDeviceId)
    }

    @Test
    fun givenExternalDeviceIdSaved_whenWithEmptyExternalDeviceId_thenExternalDeviceIdIsEmpty() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)

        // When
        val newDeviceId = SUT.withExternalUserId(oldDeviceId, "")

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, "")
        assertEquals(expectedDeviceId, newDeviceId)
    }

    @Test
    fun givenNoExternalDeviceId_whenWithExternalDeviceId_thenExternalDeviceIdSaved() {
        // Given
        val oldDeviceId = DeviceId(DEVICE_ID_UUID, null)

        // When
        val newDeviceId = SUT.withExternalUserId(oldDeviceId, EXTERNAL_DEVICE_ID)

        // Then
        val expectedDeviceId = DeviceId(DEVICE_ID_UUID, EXTERNAL_DEVICE_ID)
        assertEquals(expectedDeviceId, newDeviceId)
    }
}