package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.fixtures.TestDatabase
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
internal class RetenoDatabaseManagerInAppInteractionImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerInAppInteractionImpl(db.database)

        fun store(vararg interactions: InAppInteractionDb) {
            interactions.forEach { sut.insertInteraction(it) }
            db.stampRows("InAppInteraction")
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read back every field of stored interactions up to the limit`() = runUnitTest {
        withSut {
            given()
            store(interaction("a", description = "closed by user"), interaction("b"))

            whenn()
            val interactions = listOf(sut.getInteractions(), sut.getInteractions(limit = 1))

            then()
            val stored = listOf(
                interaction("a", description = "closed by user").copy(rowId = "1"),
                interaction("b").copy(rowId = "2")
            )
            assertEquals(listOf(stored, stored.take(1)), interactions)
            assertEquals(2L, sut.getInAppInteractionsCount())
        }
    }

    @Test
    fun `should tell whether an interaction was deleted`() = runUnitTest {
        withSut {
            given()
            store(interaction("a"))
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
            store(interaction("a"))
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
            store(interaction("old", time = "2026-01-01T00:00:00Z"), interaction("new", time = "2026-02-01T00:00:00Z"))

            whenn()
            val deleted = sut.deleteInteractionsByTime("2026-01-15T00:00:00Z")

            then()
            assertEquals(listOf("old"), deleted.map { it.interactionId })
            assertTrue(sut.getInteractions().map { it.interactionId } == listOf("new"))
        }
    }

    private fun interaction(id: String, time: String = "2026-01-02T03:04:05Z", description: String? = null) =
        InAppInteractionDb(
            interactionId = id,
            time = time,
            messageInstanceId = 7,
            status = "OPEN",
            statusDescription = description
        )
}
