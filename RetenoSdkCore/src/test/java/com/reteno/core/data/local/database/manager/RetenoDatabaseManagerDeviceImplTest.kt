package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.core.util.Util
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseManagerDeviceImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerDeviceImpl(db.database)

        fun store(vararg devices: DeviceDb) {
            devices.forEach { sut.insertDevice(it) }
            db.stampRows(DeviceSchema.TABLE_NAME)
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read back every field of a stored device`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE)

            whenn()
            val devices = sut.getDevices()

            then()
            assertEquals(listOf(FULL_DEVICE.copy(rowId = "1", createdAt = STORED_AT_MILLIS)), devices)
        }
    }

    @Test
    fun `should read back a device without optional fields`() = runUnitTest {
        withSut {
            given()
            store(MINIMAL_DEVICE)

            whenn()
            val devices = sut.getDevices()

            then()
            assertEquals(listOf(MINIMAL_DEVICE.copy(rowId = "1", createdAt = STORED_AT_MILLIS)), devices)
        }
    }

    @Test
    fun `should read oldest devices up to the limit`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE.copy(deviceId = "first"), FULL_DEVICE.copy(deviceId = "second"))
            db.execute("UPDATE Device SET timeStamp = '2026-01-01 00:00:00' WHERE deviceId = 'second'")

            whenn()
            val devices = sut.getDevices(limit = 1)

            then()
            assertEquals(listOf("second"), devices.map { it.deviceId })
        }
    }

    @Test
    fun `should remove and report a stored device that has no time`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE, FULL_DEVICE.copy(deviceId = "broken"))
            db.execute("UPDATE Device SET timeStamp = NULL WHERE deviceId = 'broken'")

            whenn()
            val devices = sut.getDevices()

            then()
            assertEquals(listOf(FULL_DEVICE.deviceId), devices.map { it.deviceId })
            assertEquals(listOf(FULL_DEVICE.deviceId), db.values("SELECT deviceId FROM Device"))
            assertEquals(
                listOf("readRows(): removed unreadable row from Device"),
                db.remoteLog.errors.map { it.first }
            )
        }
    }

    @Test
    fun `should read no devices when the table cannot be read`() = runUnitTest {
        withSut {
            given()
            db.dropTable(DeviceSchema.TABLE_NAME)

            whenn()
            val devices = sut.getDevices()

            then()
            assertEquals(emptyList<DeviceDb>(), devices)
            assertTrue("readRows(): unable to read Device" in db.remoteLog.errors.map { it.first })
        }
    }

    @Test
    fun `should count devices not yet synced with the server`() = runUnitTest {
        withSut {
            given()
            store(
                FULL_DEVICE.copy(isSynchronizedWithBackend = BooleanDb.TRUE),
                FULL_DEVICE.copy(isSynchronizedWithBackend = BooleanDb.FALSE),
                MINIMAL_DEVICE
            )

            whenn()
            val count = sut.getUnSyncedDeviceCount()

            then()
            assertEquals(1L, count)
        }
    }

    @Test
    fun `should tell whether a device was deleted`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE)
            val stored = sut.getDevices().single()

            whenn()
            val deleted = listOf(sut.deleteDevice(stored), sut.deleteDevice(stored))

            then()
            assertEquals(listOf(true, false), deleted)
        }
    }

    @Test
    fun `should delete nothing for a device without a row id`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE)
            val stored = sut.getDevices().single()

            whenn()
            val deleted = sut.deleteDevice(stored.copy(rowId = null))

            then()
            assertEquals(false, deleted)
            assertEquals(listOf(stored), sut.getDevices())
        }
    }

    @Test
    fun `should delete several stored devices`() = runUnitTest {
        withSut {
            given()
            store(FULL_DEVICE, FULL_DEVICE, FULL_DEVICE)
            val stored = sut.getDevices()

            whenn()
            sut.deleteDevices(stored.take(2) + MINIMAL_DEVICE)

            then()
            assertEquals(listOf(stored.last().rowId), db.values("SELECT row_id FROM Device"))
        }
    }

    companion object {
        private val STORED_AT_MILLIS = Util.formatSqlDateToTimestamp(TestDatabase.STORED_AT)

        private val FULL_DEVICE = DeviceDb(
            deviceId = "device-body",
            externalUserId = "external-user",
            pushToken = "push-token",
            pushSubscribed = BooleanDb.TRUE,
            category = DeviceCategoryDb.TABLET,
            osType = DeviceOsDb.ANDROID,
            osVersion = "14",
            deviceModel = "Pixel",
            appVersion = "1.0",
            languageCode = "uk",
            timeZone = "Europe/Kyiv",
            advertisingId = "ad-id",
            isSynchronizedWithBackend = BooleanDb.FALSE,
            email = "john@example.com",
            phone = "+380501234567"
        )

        private val MINIMAL_DEVICE = DeviceDb(
            deviceId = "minimal",
            externalUserId = null,
            pushToken = null,
            pushSubscribed = null,
            category = DeviceCategoryDb.MOBILE,
            osVersion = null,
            deviceModel = null,
            appVersion = null,
            languageCode = null,
            timeZone = null,
            advertisingId = null,
            email = null,
            phone = null
        )
    }
}
