package com.reteno.core.internal.action.deeplink

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
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

internal class TrackDeeplinkClickActionTest {

    private class SutFixture(scope: TestScope) {
        val wrappedLinksDb = mockk<RetenoDatabaseManagerWrappedLink>()
        val sync = FakeSyncTrigger()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var insertedOnIo = false
        val sut = TrackDeeplinkClickAction(wrappedLinksDb, sync, dispatchers, traced.runner)

        init {
            every { wrappedLinksDb.insertWrappedLink(any()) } answers { insertedOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should save wrapped link click and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(WRAPPED)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.wrappedLinksDb.insertWrappedLink(WRAPPED) }
        assertTrue(fixture.insertedOnIo)
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should ignore blank wrapped link`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(" ")

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.wrappedLinksDb.insertWrappedLink(any()) }
        assertEquals(0, fixture.sync.requests)
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ no wrapped link, nothing to track") })
    }

    @Test
    fun `should never write the link to trace`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(WRAPPED)

        then()
        assertFalse(fixture.traced.sink.lines.any { it.contains("http") || it.contains("track.example") })
    }

    private companion object {
        const val WRAPPED = "https://track.example/c/abc"
    }
}
