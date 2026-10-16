package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.fixtures.TestDatabase
import com.reteno.core.util.Util
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
internal class RetenoDatabaseManagerUserImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerUserImpl(db.database)

        fun store(vararg users: UserDb) {
            users.forEach { sut.insertUser(it) }
            db.stampRows("User")
        }

        fun readBack(user: UserDb, rowId: String) = user.copy(rowId = rowId, createdAt = STORED_AT_MILLIS)
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read back every field of a stored user with attributes and address`() = runUnitTest {
        withSut {
            given()
            store(FULL_USER)

            whenn()
            val users = sut.getUsers()

            then()
            assertEquals(listOf(readBack(FULL_USER, rowId = "1")), users)
        }
    }

    @Test
    fun `should read back users with partial or no attributes up to the limit`() = runUnitTest {
        withSut {
            given()
            store(PARTIAL_ADDRESS_USER, NO_ADDRESS_USER, BARE_USER)

            whenn()
            val users = listOf(sut.getUsers(), sut.getUsers(limit = 1))

            then()
            val stored = listOf(
                readBack(PARTIAL_ADDRESS_USER, rowId = "1"),
                readBack(NO_ADDRESS_USER, rowId = "2"),
                readBack(BARE_USER, rowId = "3")
            )
            assertEquals(listOf(stored, stored.take(1)), users)
        }
    }

    @Test
    fun `should read a user whose attributes are all empty as a user without attributes`() = runUnitTest {
        withSut {
            given()
            store(BARE_USER.copy(userAttributes = UserAttributesDb(address = AddressDb())))

            whenn()
            val users = sut.getUsers()

            then()
            assertEquals(listOf(readBack(BARE_USER, rowId = "1")), users)
        }
    }

    @Test
    fun `should remove stored users missing a device or time`() = runUnitTest {
        withSut {
            given()
            store(BARE_USER, BARE_USER.copy(deviceId = "no time"))
            db.execute("UPDATE User SET timeStamp = NULL WHERE deviceId = 'no time'")
            db.execute("INSERT INTO User (externalUserId) VALUES ('no device')")

            whenn()
            val users = sut.getUsers()

            then()
            assertEquals(listOf(BARE_USER.deviceId), users.map { it.deviceId })
            assertEquals(listOf(BARE_USER.deviceId), db.values("SELECT deviceId FROM User"))
        }
    }

    @Test
    fun `should remove a stored user that cannot be parsed and read the users after it`() = runUnitTest {
        withSut {
            given()
            store(BARE_USER.copy(deviceId = "broken"), BARE_USER)
            db.execute("UPDATE User SET subscriptionKeys = '{' WHERE deviceId = 'broken'")

            whenn()
            val users = sut.getUsers()

            then()
            assertEquals(listOf(BARE_USER.deviceId), users.map { it.deviceId })
            assertEquals(listOf(BARE_USER.deviceId), db.values("SELECT deviceId FROM User"))
        }
    }

    @Test
    fun `should count users not yet synced with the server`() = runUnitTest {
        withSut {
            given()
            store(
                BARE_USER.copy(isSynchronizedWithBackend = BooleanDb.TRUE),
                BARE_USER.copy(isSynchronizedWithBackend = BooleanDb.FALSE)
            )

            whenn()
            val count = sut.getUnSyncedUserCount()

            then()
            assertEquals(1L, count)
        }
    }

    @Test
    fun `should tell whether a user was deleted with its attributes`() = runUnitTest {
        withSut {
            given()
            store(FULL_USER)
            val stored = sut.getUsers().single()

            whenn()
            val deleted = listOf(sut.deleteUser(stored), sut.deleteUser(stored))

            then()
            assertEquals(listOf(true, false), deleted)
            assertEquals(listOf(0L, 0L), listOf(db.count("UserAttributes"), db.count("UserAddress")))
        }
    }

    @Test
    fun `should delete nothing for a user without a row id`() = runUnitTest {
        withSut {
            given()
            store(FULL_USER)
            val stored = sut.getUsers().single()

            whenn()
            val deleted = sut.deleteUser(stored.copy(rowId = null))

            then()
            assertEquals(false, deleted)
            assertEquals(listOf(stored), sut.getUsers())
        }
    }

    @Test
    fun `should delete several stored users`() = runUnitTest {
        withSut {
            given()
            store(BARE_USER, BARE_USER, BARE_USER)
            val stored = sut.getUsers()

            whenn()
            sut.deleteUsers(stored.take(2) + BARE_USER)

            then()
            assertEquals(listOf(stored.last().rowId), db.values("SELECT user_row_id FROM User"))
        }
    }

    companion object {
        private val STORED_AT_MILLIS = Util.formatSqlDateToTimestamp(TestDatabase.STORED_AT)

        private val FULL_USER = UserDb(
            deviceId = "device-body",
            externalUserId = "external-user",
            userAttributes = UserAttributesDb(
                phone = "+380501234567",
                email = "ann@example.com",
                firstName = "Ann",
                lastName = "Lee",
                languageCode = "uk",
                timeZone = "Europe/Kyiv",
                address = AddressDb(region = "Kyiv", town = "Kyiv", address = "Main st 1", postcode = "01001"),
                fields = listOf(UserCustomFieldDb(key = "plan", value = "pro"), UserCustomFieldDb("note", null)),
                marketId = "m-1"
            ),
            subscriptionKeys = listOf("news"),
            groupNamesInclude = listOf("vip"),
            groupNamesExclude = listOf("churned"),
            isSynchronizedWithBackend = BooleanDb.FALSE
        )

        private val PARTIAL_ADDRESS_USER = UserDb(
            deviceId = "partial-address",
            userAttributes = UserAttributesDb(address = AddressDb(postcode = "01001"))
        )

        private val NO_ADDRESS_USER = UserDb(
            deviceId = "no-address",
            userAttributes = UserAttributesDb(firstName = "Bob")
        )

        private val BARE_USER = UserDb(deviceId = "bare")
    }
}
