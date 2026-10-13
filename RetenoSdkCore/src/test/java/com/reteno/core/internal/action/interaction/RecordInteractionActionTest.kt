package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.FailureReport
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class RecordInteractionActionTest {

    private class SutFixture(scope: TestScope, token: String = TOKEN) {
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>()
        val configRepository = mockk<ConfigRepository>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var tokenReadOnIo = false
        var insertedOnIo = false
        val sut = RecordInteractionAction(interactionsDb, configRepository, dispatchers, { TIME }, traced.runner)

        init {
            coEvery { configRepository.getFcmToken() } answers {
                tokenReadOnIo = dispatchers.markedIo.isRunningHere
                token
            }
            every { interactionsDb.insertInteraction(any()) } answers { insertedOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should save interaction status with push token and time`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut("interaction-1", InteractionStatus.DELIVERED)

        then()
        assertEquals(true, result.getOrNull())
        verify(exactly = 1) {
            fixture.interactionsDb.insertInteraction(Interaction(InteractionStatus.DELIVERED, TIME, token = TOKEN).toDb("interaction-1"))
        }
        assertTrue(fixture.insertedOnIo)
    }

    @Test
    fun `should read push token on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", InteractionStatus.DELIVERED)

        then()
        assertTrue(fixture.tokenReadOnIo)
    }

    @Test
    fun `should report interaction as not recorded when there is no push token`() = runUnitTest {
        given()
        val fixture = SutFixture(this, token = " ")

        whenn()
        val result = fixture.sut("interaction-1", InteractionStatus.DELIVERED)

        then()
        assertEquals(false, result.getOrNull())
        verify(exactly = 0) { fixture.interactionsDb.insertInteraction(any()) }
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ no push token yet, interaction not recorded") })
    }

    @Test
    fun `should fail when interaction cannot be saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.interactionsDb.insertInteraction(any()) } throws IOException("disk full")

        whenn()
        val result = fixture.sut("interaction-1", InteractionStatus.DELIVERED)

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(
            listOf(FailureReport("RecordInteraction", "save interaction to local database", IOException::class.java, "disk full")),
            fixture.traced.reports
        )
    }

    @Test
    fun `should trace status and redacted push token`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", InteractionStatus.DELIVERED)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("RecordInteraction  id=interaction-1  status=DELIVERED"))
        assertTrue(lines.any { it.contains("✓ read push token") && it.contains("…9876") })
        assertFalse(lines.any { it.contains(TOKEN) })
    }

    private companion object {
        const val TOKEN = "secret-push-token-9876"
        const val TIME = "2026-01-02T03:04:05Z"
    }
}
