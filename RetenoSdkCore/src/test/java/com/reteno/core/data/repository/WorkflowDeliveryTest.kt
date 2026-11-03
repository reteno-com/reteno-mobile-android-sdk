package com.reteno.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteractionImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessagesImpl
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.mappers.mapResponseToInAppMessages
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InAppInteraction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], manifest = Config.NONE)
class WorkflowDeliveryTest {
    private lateinit var context: Context
    private lateinit var database: RetenoDatabaseImpl
    private val api = mockk<ApiClient>(relaxed = true)
    private val prefs = mockk<SharedPrefsManager>(relaxed = true)
    private val files = mockk<FileManager>(relaxed = true)
    private var owner = "application-account-contact-A"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DbSchema.DATABASE_NAME)
        database = RetenoDatabaseImpl(context)
        every { api.workflowContext() } answers { owner }
        every { prefs.getIamEtag() } returns null
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(DbSchema.DATABASE_NAME)
    }

    @Test
    fun mixed_discovery_matches_same_snapshot_content_by_iid_and_keeps_request_bodies_separate() = runTest {
        val expiry = Instant.now().plusSeconds(3600).toString()
        val list = discovery(expiry)
        val workflowBodies = mutableListOf<JsonObject>()
        val ordinaryBodies = mutableListOf<JsonObject>()
        every { api.get(ApiContract.InAppMessages.GetInAppMessages, any(), any(), any()) } answers {
            arg<ResponseCallback>(3).onSuccess(mapOf("ETag" to listOf("v1")), list)
        }
        every { api.postWithRetry(ApiContract.InAppMessages.GetInAppMessagesContent, any(), any(), any()) } answers {
            ordinaryBodies.add(arg<String>(1).fromJson())
            arg<ResponseCallback>(3).onSuccess("{\"contents\":[]}")
        }
        every { api.postInWorkflowContext(owner, ApiContract.InAppMessages.GetInAppMessagesContent, any(), any()) } answers {
            workflowBodies.add(arg<String>(2).fromJson())
            arg<ResponseCallback>(3).onSuccess("""{"contents":[
                {"iid":"22222222-2222-4222-8222-222222222222","messageInstanceId":8001,"layoutType":"FULL","model":{"text":"\u0024{order}","nullable":null},"personalisation":{"order":"order-2"}},
                {"iid":"11111111-1111-4111-8111-111111111111","messageInstanceId":8001,"layoutType":"FULL","model":{"text":"\u0024{order}","nullable":null},"personalisation":{"order":"order-1"}},
                {"iid":"wrong","messageInstanceId":8001,"layoutType":"FULL","model":{}}
            ]}""")
        }
        val repository = repository(StandardTestDispatcher(testScheduler))
        val messages = repository.getInAppMessages()
        verify { api.get(ApiContract.InAppMessages.GetInAppMessages, any(), mapOf("includeWorkflow" to "true"), any()) }
        assertEquals(listOf(null, "11111111-1111-4111-8111-111111111111", "22222222-2222-4222-8222-222222222222"), messages.messages.map { it.iid })
        repository.getInAppMessagesContent(messages.messages.filterNot { it.inWorkflow }.map { it.messageInstanceId })
        val content = repository.getWorkflowContent(messages.messages.filter { it.inWorkflow })
        assertEquals(setOf("11111111-1111-4111-8111-111111111111", "22222222-2222-4222-8222-222222222222"), content.map { it.iid }.toSet())
        assertTrue(content.first().model.asJsonObject.get("nullable").isJsonNull)
        assertEquals("order-1", content.first { it.iid == "11111111-1111-4111-8111-111111111111" }.personalisation!!.asJsonObject.get("order").asString)
        assertEquals(setOf("messages"), workflowBodies.single().keySet())
        assertEquals(2, workflowBodies.single().getAsJsonArray("messages").size())
        assertEquals(setOf("messageInstanceIds"), ordinaryBodies.single().keySet())
        repository.saveInAppMessages(messages)
        assertEquals(3, RetenoDatabaseManagerInAppMessagesImpl(database).getInAppMessages().size)
        assertTrue(repository.isWorkflowContextCurrent(messages.messages.first { it.inWorkflow }))
        owner = "application-account-contact-B"
        assertFalse(repository.isWorkflowContextCurrent(messages.messages.first { it.inWorkflow }))
        assertTrue(repository.getWorkflowContent(messages.messages).isEmpty())
        verify(exactly = 1) { api.postInWorkflowContext(any(), any(), any(), any()) }
    }

    @Test
    fun failed_discovery_is_distinct_from_a_successful_empty_response() = runTest {
        var unavailable = true
        every { api.get(ApiContract.InAppMessages.GetInAppMessages, any(), any(), any()) } answers {
            if (unavailable) arg<ResponseCallback>(3).onFailure(503, "unavailable", null)
            else arg<ResponseCallback>(3).onSuccess("""{"messages":[]}""")
        }
        val repository = repository(StandardTestDispatcher(testScheduler))
        val failure = repository.getInAppMessages()
        assertTrue(failure.messages.isEmpty())
        assertFalse(failure.isSuccessful)
        unavailable = false
        val emptySuccess = repository.getInAppMessages()
        assertTrue(emptySuccess.messages.isEmpty())
        assertTrue(emptySuccess.isSuccessful)
    }

    @Test
    fun acknowledged_opened_survives_reopen_and_keeps_other_iid_and_failed_observations_independent() {
        var store = RetenoDatabaseManagerInAppInteractionImpl(database)
        val now = ZonedDateTime.now()
        val expiry = now.plusHours(1).toInstant().toString()
        val opened = InAppInteraction.createOpened("11111111-1111-4111-8111-111111111111", 8001).copy(
            time = now, workflowContext = owner, workflowExpiresAt = expiry).toDb()
        store.insertInteraction(opened)
        store.insertInteraction(InAppInteraction.createFailed("11111111-1111-4111-8111-111111111111", 8001, "before visible")
            .copy(time = now, workflowContext = owner, workflowExpiresAt = expiry).toDb())
        store.insertInteraction(InAppInteraction.createOpened("22222222-2222-4222-8222-222222222222", 8001)
            .copy(time = now, workflowContext = owner, workflowExpiresAt = expiry).toDb())
        val originalBody = store.getPendingInteractions(owner).first().toRemote().toJson()
        assertEquals(3, store.getPendingInteractions(owner).size)
        assertTrue(store.isWorkflowShown(owner, "11111111-1111-4111-8111-111111111111"))
        assertFalse(store.isWorkflowShown("other-account", "11111111-1111-4111-8111-111111111111"))
        database.close()
        database = RetenoDatabaseImpl(context)
        store = RetenoDatabaseManagerInAppInteractionImpl(database)
        assertEquals(originalBody, store.getPendingInteractions(owner).first().toRemote().toJson())
        val savedOpened = store.getPendingInteractions(owner).first()
        assertTrue(store.deleteInteraction(savedOpened))
        database.close()
        database = RetenoDatabaseImpl(context)
        store = RetenoDatabaseManagerInAppInteractionImpl(database)
        assertTrue(store.isWorkflowShown(owner, "11111111-1111-4111-8111-111111111111"))
        assertEquals(2, store.getPendingInteractions(owner).size)
        assertTrue(store.getPendingInteractions("other-account").isEmpty())
        assertFalse(originalBody.contains("workflowContext"))
        assertTrue(originalBody.fromJson<JsonObject>().get("inWorkflow").asBoolean)
        assertEquals(now.toInstant().epochSecond, Instant.parse(opened.time).epochSecond)
        assertFalse(opened.time.contains('.'))
    }

    @Test
    fun workflow_click_replays_complete_persisted_action_after_failure_and_restart() {
        val iid = "11111111-1111-4111-8111-111111111111"
        val now = ZonedDateTime.now()
        val expiry = now.plusHours(1).toInstant().toString()
        var store = RetenoDatabaseManagerInAppInteractionImpl(database)
        store.insertInteraction(InAppInteraction(
            interactionId = iid,
            time = now,
            messageInstanceId = 8001,
            status = InAppInteractionStatus.CLICKED,
            workflowContext = owner,
            workflowExpiresAt = expiry,
            action = InteractionAction("OPEN_URL", "order-details", "https://example.com/orders/123")
        ).toDb())
        val bodies = mutableListOf<String>()
        every { api.postInWorkflowContext(owner, ApiContract.InAppMessages.RegisterInteraction, any(), any()) } answers {
            bodies.add(arg(2))
            if (bodies.size == 1) arg<ResponseCallback>(3).onFailure(503, "unavailable", null)
            else arg<ResponseCallback>(3).onSuccess("")
        }
        InteractionRepositoryImpl(api, mockk(relaxed = true), store).pushInAppInteractions()
        assertTrue(store.isWorkflowShown(owner, iid))
        database.close()
        database = RetenoDatabaseImpl(context)
        store = RetenoDatabaseManagerInAppInteractionImpl(database)
        val savedClick = store.getInteractions().single { it.status == "CLICKED" }
        store.deferUntil(savedClick, 0)
        InteractionRepositoryImpl(api, mockk(relaxed = true), store).pushInAppInteractions()
        assertEquals(2, bodies.size)
        assertEquals(bodies.first(), bodies.last())
        val body = bodies.last().fromJson<JsonObject>()
        assertEquals(iid, body.get("iid").asString)
        assertEquals("CLICKED", body.get("status").asString)
        assertEquals(8001L, body.get("messageInstanceId").asLong)
        assertEquals(now.toInstant().epochSecond, Instant.parse(body.get("time").asString).epochSecond)
        assertEquals("OPEN_URL", body.getAsJsonObject("action").get("type").asString)
        assertEquals("order-details", body.getAsJsonObject("action").get("targetComponentId").asString)
        assertEquals("https://example.com/orders/123", body.getAsJsonObject("action").get("url").asString)
        assertTrue(body.get("inWorkflow").asBoolean)
        assertTrue(store.getPendingInteractions(owner).isEmpty())
        assertTrue(store.isWorkflowShown(owner, iid))
        verify(exactly = 0) { api.put(any(), any(), any()) }
    }

    @Test
    fun upgrade_from_database_without_in_app_tables_creates_workflow_storage_once() {
        database.close()
        context.deleteDatabase(DbSchema.DATABASE_NAME)
        context.openOrCreateDatabase(DbSchema.DATABASE_NAME, 0, null).use { old ->
            old.version = 8
        }
        database = RetenoDatabaseImpl(context)
        val store = RetenoDatabaseManagerInAppInteractionImpl(database)
        val now = ZonedDateTime.now()
        store.insertInteraction(InAppInteraction(
            interactionId = "11111111-1111-4111-8111-111111111111",
            time = now,
            messageInstanceId = 8001,
            status = InAppInteractionStatus.CLICKED,
            workflowContext = owner,
            workflowExpiresAt = now.plusHours(1).toInstant().toString(),
            action = InteractionAction("OPEN_URL", "details", "https://example.com/details")
        ).toDb())
        assertEquals(1, store.getPendingInteractions(owner).size)
        assertEquals("details", store.getPendingInteractions(owner).single().toRemote().action?.targetComponentId)
        assertTrue(RetenoDatabaseManagerInAppMessagesImpl(database).getInAppMessages().isEmpty())
    }

    @Test
    fun ordinary_frequency_and_pending_execution_deadline_survive_schema_upgrade_and_repeated_discovery() = runTest {
        database.close()
        context.deleteDatabase(DbSchema.DATABASE_NAME)
        context.openOrCreateDatabase(DbSchema.DATABASE_NAME, 0, null).use { old ->
            old.execSQL(InAppMessageSchema.SQL_CREATE_TABLE.substringBefore(", workflowIid") + ")")
            old.execSQL(InAppInteractionSchema.SQL_CREATE_TABLE.substringBefore(", workflowContext") + ")")
            old.execSQL("INSERT INTO InAppMessage(iamId,iamInstanceId,iamDisplayRules,iamLastShowTime,iamShowCount) VALUES(7,8,'{}',12345,4)")
            old.version = 11
        }
        database = RetenoDatabaseImpl(context)
        val messages = RetenoDatabaseManagerInAppMessagesImpl(database)
        val ordinary = messages.getInAppMessages().single()
        assertEquals(4L, ordinary.showCount)
        assertEquals(12345L, ordinary.lastShowTime)
        val deadline = Instant.now().plusSeconds(300)
        var response = discovery(deadline.toString())
        every { api.get(ApiContract.InAppMessages.GetInAppMessages, any(), any(), any()) } answers {
            arg<ResponseCallback>(3).onSuccess(mapOf("ETag" to listOf("v1")), response)
        }
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.saveInAppMessages(repository.getInAppMessages())
        response = discovery(deadline.plusSeconds(3600).toString())
        val refreshed = repository.getInAppMessages()
        assertEquals(deadline, Instant.parse(refreshed.messages.first { it.iid == "11111111-1111-4111-8111-111111111111" }.expiresAt))
        repository.saveInAppMessages(refreshed)
        every { api.get(ApiContract.InAppMessages.GetInAppMessages, any(), any(), any()) } answers {
            arg<ResponseCallback>(3).onFailure(304, null, null)
        }
        assertEquals(setOf("11111111-1111-4111-8111-111111111111", "22222222-2222-4222-8222-222222222222"), repository.getInAppMessages().messages.mapNotNull { it.iid }.toSet())
    }

    @Test
    fun malformed_workflow_and_unknown_enabled_conditions_do_not_remove_valid_mixed_entries() {
        val expiry = Instant.now().plusSeconds(3600).toString()
        val json = discovery(expiry).fromJson<JsonObject>()
        json.getAsJsonArray("messages").add("""{"inWorkflow":true,"messageId":7,"messageInstanceId":8001,"displayRules":{},"expiresAt":"invalid"}""".fromJson<JsonObject>())
        json.getAsJsonArray("messages").add("""{"inWorkflow":true,"iid":"unsupported","messageId":7,"messageInstanceId":8001,"displayRules":{"UNKNOWN":{"enabled":true}},"expiresAt":"$expiry"}""".fromJson<JsonObject>())
        val parsed = json.toString().fromJson<InAppMessageListResponse>().messages.mapResponseToInAppMessages()
        assertEquals(listOf(null, "11111111-1111-4111-8111-111111111111", "22222222-2222-4222-8222-222222222222"), parsed.map { it.iid })
    }

    private fun repository(dispatcher: CoroutineDispatcher): IamRepositoryImpl = IamRepositoryImpl(
        context, api, files, prefs, RetenoDatabaseManagerInAppMessagesImpl(database), dispatcher,
        RetenoDatabaseManagerInAppInteractionImpl(database)
    )

    private fun discovery(expiry: String): String = """{"messages":[
        {"messageId":7,"messageInstanceId":8,"displayRules":{}},
        {"messageId":7,"messageInstanceId":8001,"inWorkflow":true,"iid":"11111111-1111-4111-8111-111111111111","expiresAt":"$expiry","displayRules":{}},
        {"messageId":7,"messageInstanceId":8001,"inWorkflow":true,"iid":"22222222-2222-4222-8222-222222222222","expiresAt":"$expiry","displayRules":{}}
    ]}"""
}
