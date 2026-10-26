package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.data.local.model.logevent.LogLevelDb
import com.reteno.core.data.local.model.logevent.RetenoLogEventDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseManagerLogEventImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerLogEventImpl(db.database)
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read back every field of stored log events up to the limit`() = runUnitTest {
        withSut {
            given()
            sut.insertLogEvent(FULL_EVENT)
            sut.insertLogEvent(MINIMAL_EVENT)

            whenn()
            val events = listOf(sut.getLogEvents(), sut.getLogEvents(limit = 1))

            then()
            val stored = listOf(FULL_EVENT.copy(rowId = "1"), MINIMAL_EVENT.copy(rowId = "2"))
            assertEquals(listOf(stored, stored.take(1)), events)
            assertEquals(2L, sut.getLogEventsCount())
        }
    }

    @Test
    fun `should tell whether the last of the given log events was deleted`() = runUnitTest {
        withSut {
            given()
            sut.insertLogEvent(FULL_EVENT)
            sut.insertLogEvent(MINIMAL_EVENT)
            val (first, second) = sut.getLogEvents()

            whenn()
            val deleted = listOf(
                sut.deleteLogEvents(listOf(first, MINIMAL_EVENT)),
                sut.deleteLogEvents(listOf(first)),
                sut.deleteLogEvents(listOf(first, second))
            )

            then()
            assertEquals(listOf(true, false, true), deleted)
            assertEquals(0L, sut.getLogEventsCount())
        }
    }

    companion object {
        private val FULL_EVENT = RetenoLogEventDb(
            platformName = DeviceOsDb.ANDROID,
            osVersion = "14",
            version = "1.0 (1)",
            device = "Pixel",
            sdkVersion = "3.0.0",
            deviceId = "device-body",
            bundleId = "com.example.app",
            logLevel = LogLevelDb.ERROR,
            errorMessage = "Action Sync failed"
        )

        private val MINIMAL_EVENT = RetenoLogEventDb(
            osVersion = "13",
            version = null,
            device = "Galaxy",
            sdkVersion = "3.0.0",
            deviceId = null,
            bundleId = null,
            logLevel = LogLevelDb.INFO,
            errorMessage = null
        )
    }
}
