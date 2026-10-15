package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toDb
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.STUB_CREATED_AT
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
import com.reteno.core.fixtures.stubUserDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PushUserStepTest {

    private class SutFixture(scope: TestScope) {
        val usersDb = mockk<RetenoDatabaseManagerUser>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushUserStep(usersDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            justRun { usersDb.deleteUsers(any()) }
            justRun { usersDb.insertUser(any()) }
        }
    }

    @Test
    fun `should continue without request when there are no users`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should continue without request when every user is synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(stubUserDb(synced = BooleanDb.TRUE))

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 0) { fixture.usersDb.deleteUsers(any()) }
    }

    @Test
    fun `should send merged unsynced users when nothing was synced before`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = stubUserDb(rowId = "1", createdAt = STUB_CREATED_AT, firstName = "John", email = null)
        val second = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 1, firstName = null, email = "j@example.com")
        every { fixture.usersDb.getUsers(any()) } returns listOf(second, first)
        val merged = second.toRemote().createAccModel(first.toRemote())

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(
            listOf(SentRequest(HttpMethod.POST, ApiContract.MobileApi.User, merged.toJson())),
            fixture.rest.requests
        )
    }

    @Test
    fun `should store merged user as synced after successful send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = stubUserDb()
        every { fixture.usersDb.getUsers(any()) } returns listOf(user)

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.usersDb.deleteUsers(listOf(user)) }
        val syncedUser = user.toRemote().toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE)
        verify(exactly = 1) { fixture.usersDb.insertUser(syncedUser) }
    }

    @Test
    fun `should send only changed attributes compared to recently synced user`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubUserDb(rowId = "1", firstName = "John", email = null, synced = BooleanDb.TRUE)
        val changed = stubUserDb(
            rowId = "2",
            createdAt = STUB_CREATED_AT + 60_000,
            firstName = "John",
            email = "j@example.com"
        )
        every { fixture.usersDb.getUsers(any()) } returns listOf(synced, changed)
        val diff = requireNotNull(changed.toRemote().createDiffModel(synced.toRemote()))
        val full = changed.toRemote().createAccModel(synced.toRemote())

        whenn()
        fixture.sut()

        then()
        assertEquals(diff.toJson(), fixture.rest.requests.single().body)
        verify(exactly = 1) { fixture.usersDb.insertUser(full.toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE)) }
    }

    @Test
    fun `should send user attributes under their wire field names`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = stubUserDb(email = "j@example.com").copy(subscriptionKeys = listOf("news"))
        every { fixture.usersDb.getUsers(any()) } returns listOf(user)

        whenn()
        fixture.sut()

        then()
        assertEquals(USER_JSON, fixture.rest.requests.single().body)
    }

    @Test
    fun `should keep synced attributes missing from pending user when marking it synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(richSyncedUser(), emailOnlyPendingUser())

        whenn()
        fixture.sut()

        then()
        verify(exactly = 1) { fixture.usersDb.insertUser(MERGED_SYNCED_USER) }
    }

    @Test
    fun `should keep synced attributes missing from pending user when server rejects it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(richSyncedUser(), emailOnlyPendingUser())
        fixture.rest.failWith(422)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.usersDb.insertUser(MERGED_SYNCED_USER) }
    }

    @Test
    fun `should drop unsynced users without changes and not send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubUserDb(rowId = "1", synced = BooleanDb.TRUE)
        val unchanged = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000)
        every { fixture.usersDb.getUsers(any()) } returns listOf(synced, unchanged)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.usersDb.deleteUsers(listOf(unchanged)) }
    }

    @Test
    fun `should send full user when synced user is outdated`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubUserDb(rowId = "1", synced = BooleanDb.TRUE)
        val unchanged = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 10 * 60_000)
        every { fixture.usersDb.getUsers(any()) } returns listOf(synced, unchanged)

        whenn()
        fixture.sut()

        then()
        assertEquals(unchanged.toRemote().toJson(), fixture.rest.requests.single().body)
    }

    @Test
    fun `should abort and keep users when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(stubUserDb())
        fixture.rest.failWith(500)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.usersDb.deleteUsers(any()) }
        verify(exactly = 0) { fixture.usersDb.insertUser(any()) }
    }

    @Test
    fun `should store user as synced and continue when server rejects it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = stubUserDb()
        every { fixture.usersDb.getUsers(any()) } returns listOf(user)
        fixture.rest.failWith(422)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        val syncedUser = user.toRemote().toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE)
        verify(exactly = 1) { fixture.usersDb.insertUser(syncedUser) }
    }

    @Test
    fun `should access local database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.usersDb.getUsers(any()) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            emptyList()
        }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
    }

    @Test
    fun `should trace steps without user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(
            stubUserDb(firstName = "Secretname", email = "secret@example.com")
        )

        whenn()
        fixture.sut()

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ load users from local database") && it.contains("count=1") })
        assertTrue(lines.any { it.contains("✓ send user to server") })
        assertTrue(lines.any { it.contains("✓ mark user as synced in local database") })
        assertFalse(lines.any { it.contains("Secretname") || it.contains("secret@example.com") })
    }

    private fun richSyncedUser() = stubUserDb(rowId = "1", synced = BooleanDb.TRUE).copy(
        userAttributes = UserAttributesDb(phone = "+380501234567", firstName = "John", lastName = "Doe")
    )

    private fun emailOnlyPendingUser() = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000).copy(
        userAttributes = UserAttributesDb(email = "j@example.com")
    )

    companion object {
        const val USER_JSON = """{"deviceId":"device-body","externalUserId":"external-user",""" +
            """"userAttributes":{"email":"j@example.com","firstName":"John"},"subscriptionKeys":["news"]}"""

        val MERGED_SYNCED_USER = UserDb(
            deviceId = "device-body",
            externalUserId = "external-user",
            userAttributes = UserAttributesDb(
                phone = "+380501234567",
                email = "j@example.com",
                firstName = "John",
                lastName = "Doe"
            ),
            isSynchronizedWithBackend = BooleanDb.TRUE
        )
    }
}
