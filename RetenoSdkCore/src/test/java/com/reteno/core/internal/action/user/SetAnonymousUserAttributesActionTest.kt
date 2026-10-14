package com.reteno.core.internal.action.user

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SetAnonymousUserAttributesActionTest {

    private class SutFixture(scope: TestScope) {
        val configRepository = mockk<ConfigRepository>()
        val usersDb = mockk<RetenoDatabaseManagerUser>()
        val deviceId = stubDeviceId()
        val sync = FakeSyncTrigger()
        val remoteLog = FakeRemoteLog()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var insertedOnIo = false
        val sut = SetAnonymousUserAttributesAction(
            configRepository = configRepository,
            usersDb = usersDb,
            sync = sync,
            remoteLog = remoteLog,
            dispatchers = dispatchers,
            runner = traced.runner
        )

        init {
            every { configRepository.getDeviceId() } returns deviceId
            every { usersDb.insertUser(any()) } answers { insertedOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should save anonymous attributes as user and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val attributes = stubAnonymousAttributes()

        whenn()
        val result = fixture.sut(attributes)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.usersDb.insertUser(User(attributes.toUserAttributes()).toDb(fixture.deviceId)) }
        assertTrue(fixture.insertedOnIo)
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should report attributes rejected by validation without saving`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(UserAttributesAnonymous())

        then()
        verify(exactly = 0) { fixture.usersDb.insertUser(any()) }
        assertEquals(0, fixture.sync.requests)
        assertEquals(listOf("SetAnonymousUserAttributes: attributes rejected by validation"), fixture.remoteLog.infos)
    }

    @Test
    fun `should trace attribute names without values`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubAnonymousAttributes(firstName = "Secretname"))

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("SetAnonymousUserAttributes  attributes={firstName}"))
        assertFalse(lines.any { it.contains("Secretname") })
    }
}
