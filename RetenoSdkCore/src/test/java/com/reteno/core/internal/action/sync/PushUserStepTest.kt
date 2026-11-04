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
import com.reteno.core.fixtures.FakeUserDb
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

    private class SutFixture(scope: TestScope, storedUsers: FakeUserDb? = null) {
        val usersDb: RetenoDatabaseManagerUser = storedUsers ?: mockk<RetenoDatabaseManagerUser>().also {
            justRun { it.deleteUsers(any()) }
            justRun { it.insertUser(any()) }
        }
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushUserStep(usersDb, rest.api(dispatchers), dispatchers, traced.runner)

        val sentBodies: List<String?>
            get() = rest.requests.map { it.body }
    }

    @Test
    fun `should report nothing to send without request when there are no users`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should report nothing to send without request when every user is synced`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.usersDb.getUsers(any()) } returns listOf(stubUserDb(synced = BooleanDb.TRUE))

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
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
    fun `should keep previously synced user and drop pending user when server rejects it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val pending = emailOnlyPendingUser()
        every { fixture.usersDb.getUsers(any()) } returns listOf(richSyncedUser(), pending)
        fixture.rest.failWith(422)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.usersDb.deleteUsers(listOf(pending)) }
        verify(exactly = 0) { fixture.usersDb.insertUser(any()) }
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
    fun `should drop rejected user without marking it synced and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val user = stubUserDb()
        every { fixture.usersDb.getUsers(any()) } returns listOf(user)
        fixture.rest.failWith(422)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.usersDb.deleteUsers(listOf(user)) }
        verify(exactly = 0) { fixture.usersDb.insertUser(any()) }
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ drop user rejected by server from local database") })
    }

    @Test
    fun `should send full user when synced user belongs to another external user`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubUserDb(rowId = "1", synced = BooleanDb.TRUE).copy(externalUserId = "previous-user")
        val switched = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000)
        every { fixture.usersDb.getUsers(any()) } returns listOf(synced, switched)

        whenn()
        fixture.sut()

        then()
        assertEquals(switched.toRemote().toJson(), fixture.rest.requests.single().body)
        val syncedSwitched = switched.toRemote().toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE)
        verify(exactly = 1) { fixture.usersDb.insertUser(syncedSwitched) }
    }

    @Test
    fun `should send full user when synced user belongs to another device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val synced = stubUserDb(rowId = "1", synced = BooleanDb.TRUE).copy(deviceId = "previous-device")
        val moved = stubUserDb(rowId = "2", createdAt = STUB_CREATED_AT + 60_000)
        every { fixture.usersDb.getUsers(any()) } returns listOf(synced, moved)

        whenn()
        fixture.sut()

        then()
        assertEquals(moved.toRemote().toJson(), fixture.rest.requests.single().body)
    }

    @Test
    fun `should send users switched while offline in separate requests in the order they were set`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        val fixture = SutFixture(this, FakeUserDb(listOf(bob, ann)))

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(listOf(ann.toRemote().toJson(), bob.toRemote().toJson()), fixture.sentBodies)
    }

    @Test
    fun `should keep the order users were stored in when they were set in the same second`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 0)
        val fixture = SutFixture(this, FakeUserDb(listOf(ann, bob)))

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(ann.toRemote().toJson(), bob.toRemote().toJson()), fixture.sentBodies)
    }

    @Test
    fun `should merge consecutive updates of the same user into one request`() = runUnitTest {
        given()
        val annName = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val annEmail = pendingUser(rowId = "2", externalUserId = "ann", firstName = null, secondsLater = 1)
            .copy(userAttributes = UserAttributesDb(email = "ann@example.com"))
        val bob = pendingUser(rowId = "3", externalUserId = "bob", firstName = "Bob", secondsLater = 2)
        val fixture = SutFixture(this, FakeUserDb(listOf(annName, annEmail, bob)))

        whenn()
        fixture.sut()

        then()
        val expected = listOf(
            annEmail.toRemote().createAccModel(annName.toRemote()).toJson(),
            bob.toRemote().toJson()
        )
        assertEquals(expected, fixture.sentBodies)
    }

    @Test
    fun `should send a user again when another user was set in between`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        val annAgain = pendingUser(rowId = "3", externalUserId = "ann", firstName = "Anna", secondsLater = 2)
        val fixture = SutFixture(this, FakeUserDb(listOf(ann, bob, annAgain)))

        whenn()
        fixture.sut()

        then()
        val expected = listOf(ann, bob, annAgain).map { it.toRemote().toJson() }
        assertEquals(expected, fixture.sentBodies)
    }

    @Test
    fun `should send only changes for the first user when it matches the synced one`() = runUnitTest {
        given()
        val synced = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
            .copy(isSynchronizedWithBackend = BooleanDb.TRUE)
        val annEmail = pendingUser(rowId = "2", externalUserId = "ann", firstName = "Ann", secondsLater = 60)
            .copy(userAttributes = UserAttributesDb(firstName = "Ann", email = "ann@example.com"))
        val bob = pendingUser(rowId = "3", externalUserId = "bob", firstName = "Bob", secondsLater = 61)
        val fixture = SutFixture(this, FakeUserDb(listOf(synced, annEmail, bob)))

        whenn()
        fixture.sut()

        then()
        val annDiff = requireNotNull(annEmail.toRemote().createDiffModel(synced.toRemote()))
        assertEquals(listOf(annDiff.toJson(), bob.toRemote().toJson()), fixture.sentBodies)
    }

    @Test
    fun `should keep only the last sent user as synced`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        val stored = FakeUserDb(listOf(ann, bob))
        val fixture = SutFixture(this, stored)

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf("bob" to BooleanDb.TRUE), storedContacts(stored))
    }

    @Test
    fun `should stop and keep later users when sending the first one fails with repeatable error`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        val stored = FakeUserDb(listOf(ann, bob))
        val fixture = SutFixture(this, stored)
        fixture.rest.failWith(500)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        assertEquals(listOf(ann.toRemote().toJson()), fixture.sentBodies)
        assertEquals(listOf(ann, bob), stored.rows)
    }

    @Test
    fun `should drop rejected user and send the next one`() = runUnitTest {
        given()
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        val stored = FakeUserDb(listOf(ann, bob))
        val fixture = SutFixture(this, stored)
        fixture.rest.failWith(422)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(listOf(ann.toRemote().toJson(), bob.toRemote().toJson()), fixture.sentBodies)
        assertEquals(listOf("bob" to BooleanDb.TRUE), storedContacts(stored))
    }

    @Test
    fun `should continue after sending user when a later round finds nothing to send`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val ann = pendingUser(rowId = "1", externalUserId = "ann", firstName = "Ann", secondsLater = 0)
        val bob = pendingUser(rowId = "2", externalUserId = "bob", firstName = "Bob", secondsLater = 1)
        every { fixture.usersDb.getUsers(any()) } returnsMany listOf(listOf(ann, bob), emptyList())

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(1, fixture.rest.requests.size)
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should send at most five users in one sync and leave the rest pending`() = runUnitTest {
        given()
        val switches = (0 until 6).map { index ->
            val externalUserId = if (index % 2 == 0) "ann" else "bob"
            pendingUser(
                rowId = "$index",
                externalUserId = externalUserId,
                firstName = "User $index",
                secondsLater = index
            )
        }
        val stored = FakeUserDb(switches)
        val fixture = SutFixture(this, stored)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(5, fixture.rest.requests.size)
        assertEquals(1L, stored.getUnSyncedUserCount())
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

    private fun pendingUser(rowId: String, externalUserId: String, firstName: String?, secondsLater: Int) =
        stubUserDb(rowId = rowId, createdAt = STUB_CREATED_AT + secondsLater * 1_000L, firstName = firstName)
            .copy(externalUserId = externalUserId)

    private fun storedContacts(stored: FakeUserDb) =
        stored.rows.map { it.externalUserId to it.isSynchronizedWithBackend }

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
