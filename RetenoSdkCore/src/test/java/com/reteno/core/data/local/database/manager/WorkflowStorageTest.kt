package com.reteno.core.data.local.database.manager

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.RestClientImpl
import com.reteno.core.fixtures.FakeDeviceIdSources
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.fixtures.TestDatabase
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubDeliveryMessage
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class DeliveryStorageTest {

    @Test
    fun `should retain pending observations and suppression after profile update and reopening`() = runUnitTest {
        given()
        TestDatabase.open {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val prefs = SharedPrefsManager(application)
            val identity = RestConfig(prefs, DeviceIdHelper(FakeDeviceIdSources(), prefs) { stubConfig() })
            identity.initDeviceId(DeviceIdMode.ANDROID_ID)
            identity.setDeviceIdSuffix(null)
            identity.setExternalUserId("account-a")
            identity.setDeviceEmail("original@example.com")
            identity.setDevicePhone("+380501234567")
            val client = RestClientImpl(identity, { stubConfig() }, FakeLogSink())
            val owner = client.deliveryContext()
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val time = ZonedDateTime.now(ZoneOffset.UTC).withNano(0)
            val observations = listOf(
                InAppInteractionStatus.OPENED, InAppInteractionStatus.CLICKED, InAppInteractionStatus.FAILED
            ).map { status ->
                InAppInteraction(
                    interactionId = UUID.randomUUID().toString(),
                    time = time,
                    messageInstanceId = 8001,
                    status = status,
                    deliveryContext = owner,
                    deliveryExpiresAt = time.plusHours(1).toInstant().toString()
                ).toDb()
            }
            observations.forEach(store::insertInteraction)

            whenn()
            identity.setDeviceEmail(null)
            identity.setDevicePhone(null)
            database.close()
            val restoredPrefs = SharedPrefsManager(application)
            val restoredIdentity = RestConfig(
                restoredPrefs, DeviceIdHelper(FakeDeviceIdSources(), restoredPrefs) { stubConfig() }
            )
            restoredIdentity.initDeviceId(DeviceIdMode.ANDROID_ID)
            val restoredClient = RestClientImpl(restoredIdentity, { stubConfig() }, FakeLogSink())
            val restoredOwner = restoredClient.deliveryContext()
            val reopenedDatabase = RetenoDatabaseImpl(application) { }
            try {
                val reopened = RetenoDatabaseManagerInAppInteractionImpl(reopenedDatabase)
                val pending = reopened.getPendingInteractions(restoredOwner)

                then()
                assertEquals(owner, restoredOwner)
                assertEquals(observations.map { it.interactionId }.toSet(), pending.map { it.interactionId }.toSet())
                assertEquals(observations.map { it.status }.toSet(), pending.map { it.status }.toSet())
                assertTrue(pending.all { it.time == observations.first().time && it.deliveryContext == owner })
                assertTrue(observations.all { reopened.isDeliveryShown(restoredOwner, it.interactionId) })
            } finally {
                reopenedDatabase.close()
            }
        }
    }

    @Test
    fun `should preserve separate delivery executions and ordinary rows sharing a message`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppMessagesImpl(database)
            val first = stubDeliveryMessage().toDB()
            val second = stubDeliveryMessage(iid = SECOND_IID).toDB()
            val ordinary = stubInAppMessage().toDB()
            store.insertInAppMessages(listOf(first, second, ordinary))

            whenn()
            store.deleteInAppMessages(listOf(first))
            val retained = store.getInAppMessages()

            then()
            assertEquals(2, retained.size)
            assertEquals(setOf(null, SECOND_IID), retained.map { it.iid }.toSet())
            assertEquals(second.deliveryContext, retained.first { it.iid != null }.deliveryContext)
            assertEquals(second.expiresAt, retained.first { it.iid != null }.expiresAt)
        }
    }

    @Test
    fun `should retain an acknowledged opened marker without keeping it pending`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubDeliveryMessage()
            store.insertInteraction(InAppInteraction.createOpened(requireNotNull(message.iid), message.messageInstanceId)
                .copy(deliveryContext = message.deliveryContext, deliveryExpiresAt = message.expiresAt).toDb())
            val inserted = store.getInteractions().single()

            whenn()
            store.deleteInteraction(inserted)
            val reopened = RetenoDatabaseManagerInAppInteractionImpl(database)

            then()
            assertTrue(reopened.isDeliveryShown(requireNotNull(message.deliveryContext), requireNotNull(message.iid)))
            assertTrue(reopened.getPendingInteractions(requireNotNull(message.deliveryContext)).isEmpty())
            assertEquals(0L, reopened.getInAppInteractionsCount())
        }
    }

    @Test
    fun `should persist complete clicked payload and bound retry attempts`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubDeliveryMessage()
            val click = InAppInteraction(
                interactionId = requireNotNull(message.iid),
                time = ZonedDateTime.now().withNano(0),
                messageInstanceId = message.messageInstanceId,
                status = InAppInteractionStatus.CLICKED,
                deliveryContext = message.deliveryContext,
                deliveryExpiresAt = message.expiresAt,
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
            assertTrue(store.getPendingInteractions(requireNotNull(message.deliveryContext)).isEmpty())
        }
    }

    @Test
    fun `should retain terminal failed state after acknowledgment and database reopen`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val message = stubDeliveryMessage()
            store.insertInteraction(InAppInteraction.createFailed(requireNotNull(message.iid), 8001, "render failure")
                .copy(deliveryContext = message.deliveryContext, deliveryExpiresAt = message.expiresAt).toDb())

            whenn()
            val shown = store.isDeliveryShown(requireNotNull(message.deliveryContext), requireNotNull(message.iid))

            then()
            assertTrue(shown)
            val observation = store.getInteractions().single()
            assertTrue(store.deleteInteraction(observation))
            val reopened = RetenoDatabaseManagerInAppInteractionImpl(database)
            assertTrue(reopened.isDeliveryShown(requireNotNull(message.deliveryContext), requireNotNull(message.iid)))
            assertTrue(reopened.getPendingInteractions(requireNotNull(message.deliveryContext)).isEmpty())
        }
    }

    @Test
    fun `should restore ordinary click owner independently from server iid provenance and delete it after acknowledgment`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val click = InAppInteraction(
                interactionId = "ordinary-iid",
                time = ZonedDateTime.now().withNano(0),
                messageInstanceId = 71,
                status = InAppInteractionStatus.CLICKED,
                deliveryContext = "account-a",
                action = InteractionAction("OPEN_URL", "primary-cta", "https://example.com/orders/123")
            ).toDb()
            store.insertInteraction(click)

            whenn()
            val reopened = RetenoDatabaseManagerInAppInteractionImpl(database)
            val hiddenInB = reopened.getPendingInteractions("account-b")
            val restored = reopened.getPendingInteractions("account-a").single()
            val payload = restored.toRemote()
            val removed = reopened.deleteInteraction(restored)

            then()
            assertTrue(hiddenInB.isEmpty())
            assertEquals(click.interactionId, payload.interactionId)
            assertEquals(click.time, payload.time)
            assertEquals(click.action, restored.action)
            assertFalse(payload.serverAssignedIid == true)
            assertFalse(reopened.isDeliveryShown("account-a", click.interactionId))
            assertTrue(removed)
            assertTrue(reopened.getInteractions().isEmpty())
        }
    }

    @Test
    fun `should expire owned ordinary observations without retaining delivery suppression markers`() = runUnitTest {
        given()
        TestDatabase.open {
            val store = RetenoDatabaseManagerInAppInteractionImpl(database)
            val now = ZonedDateTime.now().withNano(0)
            val old = InAppInteraction(
                interactionId = "old-ordinary-iid",
                time = now.minusDays(2),
                messageInstanceId = 71,
                status = InAppInteractionStatus.OPENED,
                deliveryContext = "account-a"
            ).toDb()
            val recent = old.copy(interactionId = "recent-ordinary-iid", time = now.toInstant().toString())
            store.insertInteraction(old)
            store.insertInteraction(recent)

            whenn()
            val removed = store.deleteInteractionsByTime(Instant.from(now.minusDays(1)).toString())
            val retained = store.getInteractions()

            then()
            assertEquals(listOf(old.interactionId), removed.map { it.interactionId })
            assertEquals(listOf(recent.interactionId), retained.map { it.interactionId })
            assertFalse(store.isDeliveryShown("account-a", recent.interactionId))
        }
    }

    private companion object {
        const val SECOND_IID = "22222222-2222-4222-8222-222222222222"
    }
}
