package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
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
internal class RetenoDatabaseManagerInteractionImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerInteractionImpl(db.database)

        fun store(vararg interactions: InteractionDb) {
            interactions.forEach { sut.insertInteraction(it) }
            db.stampRows("Interaction")
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read back interactions with a token or an action up to the limit`() = runUnitTest {
        withSut {
            given()
            store(WITH_TOKEN, WITH_ACTION)

            whenn()
            val interactions = listOf(sut.getInteractions(), sut.getInteractions(limit = 1))

            then()
            val stored = listOf(WITH_TOKEN.copy(rowId = "1"), WITH_ACTION.copy(rowId = "2"))
            assertEquals(listOf(stored, stored.take(1)), interactions)
            assertEquals(2L, sut.getInteractionCount())
        }
    }

    @Test
    fun `should remove stored interactions missing an id, status, time or both token and action`() = runUnitTest {
        withSut {
            given()
            listOf(
                "INSERT INTO Interaction (status, time, token) VALUES ('CLICKED', 'now', 't')",
                "INSERT INTO Interaction (interactionId, time, token) VALUES ('i', 'now', 't')",
                "INSERT INTO Interaction (interactionId, status, token) VALUES ('i', 'CLICKED', 't')",
                "INSERT INTO Interaction (interactionId, status, time, token) VALUES ('i', 'CLICKED', 'now', '')",
                "INSERT INTO Interaction (interactionId, status, time, action) VALUES ('i', 'CLICKED', 'now', '')"
            ).forEach { db.execute(it) }

            whenn()
            val interactions = sut.getInteractions()

            then()
            assertEquals(emptyList<InteractionDb>(), interactions)
            assertEquals(0L, sut.getInteractionCount())
        }
    }

    @Test
    fun `should tell whether an interaction was deleted`() = runUnitTest {
        withSut {
            given()
            store(WITH_TOKEN)
            val stored = sut.getInteractions().single()

            whenn()
            val deleted = listOf(sut.deleteInteraction(stored), sut.deleteInteraction(stored))

            then()
            assertEquals(listOf(true, false), deleted)
        }
    }

    @Test
    fun `should delete nothing for an interaction without a row id`() = runUnitTest {
        withSut {
            given()
            store(WITH_TOKEN)
            val stored = sut.getInteractions().single()

            whenn()
            val deleted = sut.deleteInteraction(stored.copy(rowId = null))

            then()
            assertEquals(false, deleted)
            assertEquals(listOf(stored), sut.getInteractions())
        }
    }

    @Test
    fun `should delete and give back interactions made before a time`() = runUnitTest {
        withSut {
            given()
            store(WITH_TOKEN.copy(time = "2026-01-01T00:00:00Z"), WITH_ACTION.copy(time = "2026-02-01T00:00:00Z"))

            whenn()
            val deleted = sut.deleteInteractionByTime("2026-01-15T00:00:00Z")

            then()
            assertEquals(listOf(WITH_TOKEN.interactionId), deleted.map { it.interactionId })
            assertEquals(listOf(WITH_ACTION.interactionId), sut.getInteractions().map { it.interactionId })
        }
    }

    @Test
    fun `should read back interaction requests oldest first`() = runUnitTest {
        withSut {
            given()
            sut.insertInteractionRequest(InteractionRequestDb(id = "late", status = InteractionStatusDb.OPENED))
            sut.insertInteractionRequest(InteractionRequestDb(id = "early", status = InteractionStatusDb.CLICKED))
            db.execute("UPDATE InteractionRequest SET timeStamp = '2026-01-01 00:00:00' WHERE interactionId = 'early'")

            whenn()
            val requests = sut.getInteractionRequests()

            then()
            val expected = listOf(
                InteractionRequestDb(rowId = "2", id = "early", status = InteractionStatusDb.CLICKED),
                InteractionRequestDb(rowId = "1", id = "late", status = InteractionStatusDb.OPENED)
            )
            assertEquals(expected, requests)
        }
    }

    @Test
    fun `should remove stored interaction requests missing an id or status`() = runUnitTest {
        withSut {
            given()
            db.execute("INSERT INTO InteractionRequest (status) VALUES ('OPENED')")
            db.execute("INSERT INTO InteractionRequest (interactionId) VALUES ('i')")

            whenn()
            val requests = sut.getInteractionRequests()

            then()
            assertEquals(emptyList<InteractionRequestDb>(), requests)
            assertEquals(0L, db.count("InteractionRequest"))
        }
    }

    @Test
    fun `should tell whether an interaction request was deleted`() = runUnitTest {
        withSut {
            given()
            sut.insertInteractionRequest(InteractionRequestDb(id = "a", status = InteractionStatusDb.OPENED))
            val stored = sut.getInteractionRequests().single()

            whenn()
            val deleted = listOf(sut.deleteInteractionRequest(stored), sut.deleteInteractionRequest(stored))

            then()
            assertEquals(listOf(true, false), deleted)
        }
    }

    @Test
    fun `should delete nothing for an interaction request without a row id`() = runUnitTest {
        withSut {
            given()
            sut.insertInteractionRequest(InteractionRequestDb(id = "a", status = InteractionStatusDb.OPENED))
            val stored = sut.getInteractionRequests().single()

            whenn()
            val deleted = sut.deleteInteractionRequest(stored.copy(rowId = null))

            then()
            assertEquals(false, deleted)
            assertEquals(listOf(stored), sut.getInteractionRequests())
        }
    }

    companion object {
        private val WITH_TOKEN = InteractionDb(
            interactionId = "with-token",
            status = InteractionStatusDb.CLICKED,
            time = "2026-01-02T03:04:05Z",
            token = "push-token"
        )

        private val WITH_ACTION = InteractionDb(
            interactionId = "with-action",
            status = InteractionStatusDb.OPENED,
            time = "2026-01-02T03:04:06Z",
            action = "deeplink"
        )
    }
}
