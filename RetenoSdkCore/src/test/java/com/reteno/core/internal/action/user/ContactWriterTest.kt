package com.reteno.core.internal.action.user

import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.user.User
import com.reteno.core.fixtures.stubDevice
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.action.sync.SyncScope
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ContactWriterTest {

    private class SutFixture(scope: TestScope, suffix: String? = null) {
        val env = UserActionFixture(scope, suffix)

        suspend fun write(suffix: String?, externalUserId: String, user: User?) =
            env.traced.runner("Test") {
                env.writer.write(this, suffix, externalUserId, user)
            }
    }

    @Test
    fun `should save changed external id user and device then sync contact immediately`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = stubUser()
        val deviceIdBeforeUser = fixture.env.deviceId.copy(externalId = "new-user")

        whenn()
        fixture.write(suffix = null, externalUserId = "new-user", user = user)

        then()
        assertEquals(listOf("external id new-user", "user saved"), fixture.env.log)
        verify(exactly = 1) { fixture.env.usersDb.insertUser(user.toDb(deviceIdBeforeUser)) }
        verify(exactly = 1) { fixture.env.configRepository.setUserEmail("john@example.com") }
        verify(exactly = 1) { fixture.env.configRepository.setUserPhone("+380501234567") }
        verify(exactly = 1) {
            fixture.env.devicesDb.insertDevice(stubDevice(fixture.env.deviceId, "token", true).toDb())
        }
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should save valid user without attributes and clear stored email and phone`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = User(subscriptionKeys = listOf("news"))

        whenn()
        fixture.write(suffix = null, externalUserId = "old-user", user = user)

        then()
        verify(exactly = 1) { fixture.env.usersDb.insertUser(any()) }
        verify(exactly = 1) { fixture.env.configRepository.setUserEmail(null) }
        verify(exactly = 1) { fixture.env.configRepository.setUserPhone(null) }
    }

    @Test
    fun `should keep unchanged external id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.write(suffix = null, externalUserId = "old-user", user = null)

        then()
        verify(exactly = 0) { fixture.env.configRepository.setExternalUserId(any()) }
        val lines = fixture.env.traced.sink.lines
        assertTrue(lines.any { it.contains("save external user id to local settings") && it.contains("unchanged") })
    }

    @Test
    fun `should not save user when none was given`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.write(suffix = null, externalUserId = "new-user", user = null)

        then()
        verify(exactly = 0) { fixture.env.usersDb.insertUser(any()) }
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
    }

    @Test
    fun `should report user rejected by validation and still sync device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val empty = User()

        whenn()
        fixture.write(suffix = null, externalUserId = "new-user", user = empty)

        then()
        verify(exactly = 0) { fixture.env.usersDb.insertUser(any()) }
        assertEquals(listOf("SetUserAttributes: user rejected by validation"), fixture.env.invalidReports)
        assertEquals(listOf(SyncScope.CONTACT), fixture.env.sync.immediateScopes)
    }

    @Test
    fun `should switch account when suffix differs`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = "old-account")
        fixture.env.session.markDeviceSent()

        whenn()
        fixture.write(suffix = "new-account", externalUserId = "new-user", user = null)

        then()
        assertEquals(listOf("stop on main", "start on main"), fixture.env.sdk.calls)
        assertEquals(listOf("clear session", "suffix new-account", "external id new-user"), fixture.env.log)
        assertEquals(true, fixture.env.sessionClearedOnIo)
        assertEquals("new-account", fixture.env.deviceId.idSuffix)
        assertTrue(fixture.env.session.isDeviceSent)
    }

    @Test
    fun `should not restart sdk when suffix is unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = null)

        whenn()
        fixture.write(suffix = null, externalUserId = "new-user", user = null)

        then()
        assertTrue(fixture.env.sdk.calls.isEmpty())
        assertTrue(fixture.env.log.none { it == "clear session" })
    }

    @Test
    fun `should treat device as not sent while switching account`() = runUnitTest {
        given()
        val fixture = SutFixture(this, suffix = "old-account")
        fixture.env.session.markDeviceSent()
        var sentWhenExternalIdSaved: Boolean? = null
        every { fixture.env.configRepository.setExternalUserId(any()) } answers {
            sentWhenExternalIdSaved = fixture.env.session.isDeviceSent
        }

        whenn()
        fixture.write(suffix = null, externalUserId = "new-user", user = null)

        then()
        assertEquals(false, sentWhenExternalIdSaved)
        val lines = fixture.env.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ stop sdk for account switch") })
        assertTrue(lines.any { it.contains("✓ clear current session") })
        assertTrue(lines.any { it.contains("✓ save device id suffix to local settings") })
        assertTrue(lines.any { it.contains("✓ start sdk after account switch") })
    }
}
