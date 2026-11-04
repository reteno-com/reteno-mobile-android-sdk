package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.fixtures.TestDatabase
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubWorkflowMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class WorkflowStorageTest {

    @Test
    fun `should preserve separate workflow executions and ordinary rows sharing a message`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppMessagesImpl(database)
            val first = stubWorkflowMessage().toDB()
            val second = stubWorkflowMessage(iid = SECOND_IID).toDB()
            val ordinary = stubInAppMessage().toDB()
            store.insertInAppMessages(listOf(first, second, ordinary))

            whenn()
            store.deleteInAppMessages(listOf(first))
            val retained = store.getInAppMessages()

            then()
            assertEquals(2, retained.size)
            assertEquals(setOf(null, SECOND_IID), retained.map { it.iid }.toSet())
            assertEquals(second.workflowContext, retained.first { it.iid != null }.workflowContext)
            assertEquals(second.expiresAt, retained.first { it.iid != null }.expiresAt)
        }
    }

    @Test
    fun `should retain an acknowledged opened marker without keeping it pending`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubWorkflowMessage()
            store.insertInteraction(InAppInteraction.createOpened(requireNotNull(message.iid), message.messageInstanceId)
                .copy(workflowContext = message.workflowContext, workflowExpiresAt = message.expiresAt).toDb())
            val inserted = store.getInteractions().single()

            whenn()
            store.deleteInteraction(inserted)
            val reopened = RetenoDatabaseManagerInAppInteractionImpl(database)

            then()
            assertTrue(reopened.isWorkflowShown(requireNotNull(message.workflowContext), requireNotNull(message.iid)))
            assertTrue(reopened.getPendingInteractions(requireNotNull(message.workflowContext)).isEmpty())
            assertEquals(0L, reopened.getInAppInteractionsCount())
        }
    }

    @Test
    fun `should persist complete clicked payload and bound retry attempts`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubWorkflowMessage()
            val click = InAppInteraction(
                interactionId = requireNotNull(message.iid),
                time = ZonedDateTime.now().withNano(0),
                messageInstanceId = message.messageInstanceId,
                status = InAppInteractionStatus.CLICKED,
                workflowContext = message.workflowContext,
                workflowExpiresAt = message.expiresAt,
                action = InteractionAction("OPEN_URL", "primary-cta", "https://example.com/orders/123")
            ).toDb()
            store.insertInteraction(click)
            val inserted = store.getInteractions().single()

            whenn()
            store.recordAttempt(inserted)
            val restored = RetenoDatabaseManagerInAppInteractionImpl(database).getInteractions().single()

            then()
            assertEquals(1, restored.attempts)
            assertEquals(click.action, restored.action)
            assertEquals(click.time, restored.time)
            assertEquals(click.interactionId, restored.interactionId)
            assertTrue(store.getPendingInteractions(requireNotNull(message.workflowContext)).isEmpty())
        }
    }

    @Test
    fun `should keep failed observations from consuming a workflow opportunity`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubWorkflowMessage()
            store.insertInteraction(InAppInteraction.createFailed(requireNotNull(message.iid), 8001, "render failure")
                .copy(workflowContext = message.workflowContext, workflowExpiresAt = message.expiresAt).toDb())

            whenn()
            val shown = store.isWorkflowShown(requireNotNull(message.workflowContext), requireNotNull(message.iid))

            then()
            assertFalse(shown)
        }
    }

    private companion object {
        const val SECOND_IID = "22222222-2222-4222-8222-222222222222"
    }
}
