package com.reteno.core.data.repository

import com.google.gson.JsonObject
import com.reteno.core.R
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.mappers.mapDbToInAppMessages
import com.reteno.core.data.local.mappers.mapResponseToInAppMessages
import com.reteno.core.data.local.mappers.toDB
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.model.iam.displayrules.DisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResponse
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResult
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyDisplayRules
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageListResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesContentResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessagesList
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.util.Util
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalCoroutinesApi::class)
class IamRepositoryImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val BASE_HTML_VERSION_LOCAL = "111"
        private const val BASE_HTML_VERSION_REMOTE = "222"

        private const val BASE_HTML_CONTENT_LOCAL = "ContentLocal"
        private const val BASE_HTML_CONTENT_REMOTE = "ContentRemote"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var databaseManagerInApp: RetenoDatabaseManagerInAppMessages

    private lateinit var SUT: IamRepository
    // endregion helper fields ---------------------------------------------------------------------

    @Test
    fun givenBaseHtmlVersionRemoteDiffersFromLocal_whenGetBaseHtml_thenBaseHtmlContentRemoteSavedLocallyLocalVersionUpdated() {
        runTest {

            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
            every { sharedPrefsManager.getIamBaseUrl() } returns null
            every { apiClient.head(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                val headers = mapOf<String, List<String>>(
                    HEADER_X_AMZ_META_VERSION to listOf<String>(BASE_HTML_VERSION_REMOTE)
                )
                callback.onSuccess(headers, BASE_HTML_VERSION_REMOTE)
            }
            every { apiClient.get(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                callback.onSuccess(BASE_HTML_CONTENT_REMOTE)
            }

            // When
            val result = SUT.getBaseHtml()

            // Then
            verify(exactly = 1) {
                sharedPrefsManager.saveIamBaseHtmlVersion(
                    eq(
                        BASE_HTML_VERSION_REMOTE
                    )
                )
            }
            verify(exactly = 1) {
                sharedPrefsManager.saveIamBaseHtmlContent(
                    eq(
                        BASE_HTML_CONTENT_REMOTE
                    )
                )
            }
            assertEquals(BASE_HTML_CONTENT_REMOTE, result)
        }
    }

    @Test
    fun givenBaseHtmlVersionRemoteEqualsLocal_whenGetBaseHtml_thenBaseHtmlContentLocalReturned() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
            every { sharedPrefsManager.getIamBaseHtmlContent() } returns BASE_HTML_CONTENT_LOCAL
            every { sharedPrefsManager.getIamBaseUrl() } returns null
            every { apiClient.head(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                val headers = mapOf<String, List<String>>(
                    HEADER_X_AMZ_META_VERSION to listOf<String>(BASE_HTML_VERSION_LOCAL)
                )
                callback.onSuccess(headers, BASE_HTML_VERSION_LOCAL)
            }

            // When
            val result = SUT.getBaseHtml()

            // Then
            assertEquals(BASE_HTML_CONTENT_LOCAL, result)
        }
    }

    @Test
    fun whenGetWidget_thenDispatchersIoUsed() = runTest {
        // Given
        createRepository(StandardTestDispatcher(testScheduler))
        val interactionId = "widgetIdHere"
        val widget = "{ model:\"widgetJsonHere\",personalisation:\"widgetJsonHere\"}"

        var usedThreadName: String? = null

        coEvery {
            apiClient.get(
                ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(
                    interactionId
                ), any(), any()
            )
        } coAnswers {
            val callback = thirdArg<ResponseCallback>()
            usedThreadName = coroutineContext.toString()
            callback.onSuccess(mapOf(), widget)
        }

        // When
        SUT.getWidgetRemote(interactionId)

        advanceUntilIdle()
        // Then
        assertNotNull(usedThreadName)
        val expectedDispatcher = "StandardTestDispatcher"
        assert(usedThreadName!!.contains(expectedDispatcher))
    }

    @Test
    fun givenInteractionId_whenGetWidgetRemoteSuccess_thenWidgetModelReturned() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val interactionId = "widgetIdHere"
            val widget = "{ \"model\": {\"type\":\"FULL\"}, \"personalisation\": {}}"
            val expected = WidgetModel(
                layoutType = null,
                model = "{\"type\":\"FULL\"}".fromJson(),
                personalization =  "{}".fromJson(),
                layoutParams = null
            )

            coEvery {
                apiClient.get(
                    ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(
                        interactionId
                    ), any(), any()
                )
            } coAnswers {
                val callback = thirdArg<ResponseCallback>()
                callback.onSuccess(mapOf(), widget)
            }

            // When
            val result = SUT.getWidgetRemote(interactionId)

            // Then
            assertEquals(expected, result)
        }
    }

    @Test
    fun givenInteractionId_whenGetWidgetRemoteError_thenEmptyWidgetModelReturned() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val interactionId = "widgetIdHere"
            val expected = WidgetModel(
                layoutType = InAppMessageContent.InAppLayoutType.FULL,
                model = (Util.readFromRaw(application, R.raw.widget) ?: "{}").fromJson(),
                layoutParams = null
            )

            // When
            coEvery {
                apiClient.get(
                    ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(
                        interactionId
                    ), any(), any()
                )
            } coAnswers {
                val callback = thirdArg<ResponseCallback>()
                callback.onFailure(400, "", null)
            }
            val result = SUT.getWidgetRemote(interactionId)

            // Then
            assertEquals(expected, result)
        }
    }

    @Test
    fun givenInteractionId_whenWidgetInitFailed_thenRemoteCalled() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val interactionId = "widgetIdHere"

            // When
            SUT.widgetInitFailed(interactionId, IamJsEvent(IamJsEventType.WIDGET_INIT_FAILED, null))

            // Then
            verify { apiClient.post(ApiContract.InAppMessages.WidgetInitFailed, any(), any()) }
        }
    }

    @Test
    fun givenInteractionId_whenGetInAppMessagesErrorIN_APP_NO_CHANGES_CODE_thenGetLocalMessages() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val inaAppDb = listOf(
                InAppMessageDb(
                    rowId = "12",
                    messageInstanceId = 12L,
                    messageId = 13L,
                    displayRules = JsonObject().toJson(),
                    lastShowTime = null,
                    layoutType = null,
                    model = null,
                    position = null
                )
            )
            every { sharedPrefsManager.getIamEtag() } returns null
            coEvery { databaseManagerInApp.getInAppMessages(any()) } returns inaAppDb

            // When
            coEvery {
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInAppMessages,
                    headers = null,
                    queryParams = null,
                    responseHandler = any()
                )
            } coAnswers {
                val callback = arg<ResponseCallback>(3)
                callback.onFailure(304, "", null)
            }
            val result = SUT.getInAppMessages()

            // Then
            assertEquals(
                result,
                InAppMessagesList(
                    messages = inaAppDb.mapDbToInAppMessages(),
                    isFromRemote = false
                )
            )
        }
    }

    @Test
    fun givenInteractionId_whenGetInAppMessagesErrorUnknown_thenGetEmptyList() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null

            // When
            coEvery {
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInAppMessages,
                    headers = null,
                    queryParams = null,
                    responseHandler = any()
                )
            } coAnswers {
                val callback = arg<ResponseCallback>(3)
                callback.onFailure(404, "", null)
            }
            val result = SUT.getInAppMessages()

            // Then
            assertEquals(
                result,
                InAppMessagesList()
            )
        }
    }

    @Test
    fun givenInteractionId_whenGetInAppMessagesSuccess_thenGetRemoteResult() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val inaAppDb = listOf(
                InAppMessageDb(
                    rowId = "12",
                    messageInstanceId = 12L,
                    messageId = 13L,
                    displayRules = JsonObject().toJson(),
                    lastShowTime = null,
                    layoutType = null,
                    model = null,
                    position = null
                )
            )
            val response = InAppMessageListResponse(
                messages = listOf(
                    InAppMessageResponse(
                        10L,
                        11L,
                        displayRules = JsonObject()
                    )
                )
            )
            every { sharedPrefsManager.getIamEtag() } returns null
            coEvery { databaseManagerInApp.getInAppMessages(any()) } returns inaAppDb

            // When
            coEvery {
                apiClient.get(
                    url = ApiContract.InAppMessages.GetInAppMessages,
                    headers = null,
                    queryParams = null,
                    responseHandler = any()
                )
            } coAnswers {
                val callback = arg<ResponseCallback>(3)
                callback.onSuccess(response.toJson())
            }
            val result = SUT.getInAppMessages()

            // Then
            assertEquals(
                result,
                InAppMessagesList(
                    messages = response.messages.mapResponseToInAppMessages(),
                    etag = null,
                    isFromRemote = true
                )
            )
        }
    }

    @Test
    fun givenInteractionId_whenGetInAppMessagesContentSuccess_thenReturnResponse() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val response = InAppMessagesContentResponse(
                contents = listOf(
                    InAppMessageContent(
                        messageInstanceId = 10L,
                        layoutType = InAppMessageContent.InAppLayoutType.FULL,
                        model = JsonObject(),
                        layoutParams = null
                    )
                )
            )
            every { sharedPrefsManager.getIamEtag() } returns null

            // When
            coEvery {
                apiClient.postWithRetry(
                    url = ApiContract.InAppMessages.GetInAppMessagesContent,
                    jsonBody = any(),
                    retryCount = any(),
                    responseHandler = any()
                )
            } coAnswers {
                val callback = arg<ResponseCallback>(3)
                callback.onSuccess(response.toJson())
            }
            val result = SUT.getInAppMessagesContent(listOf(12L))

            // Then
            assertEquals(
                result,
                response.contents
            )
        }
    }

    @Test
    fun givenInteractionId_whenGetInAppMessagesContentError_thenReturnEmptyList() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null

            // When
            coEvery {
                apiClient.postWithRetry(
                    url = ApiContract.InAppMessages.GetInAppMessagesContent,
                    jsonBody = any(),
                    retryCount = any(),
                    responseHandler = any()
                )
            } coAnswers {
                val callback = arg<ResponseCallback>(3)
                callback.onFailure(400, null, null)
            }
            val result = SUT.getInAppMessagesContent(listOf(12L))

            // Then
            assertEquals(
                emptyList<InAppMessageContent>(),
                result
            )
        }
    }

    @Test
    fun givenSaveInAppMessages_whenFromRemote_thenSave() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null

            // When
            SUT.saveInAppMessages(
                InAppMessagesList(etag = "tag", isFromRemote = true)
            )

            // Then
            verify { databaseManagerInApp.deleteAllInAppMessages() }
            verify { databaseManagerInApp.insertInAppMessages(emptyList()) }
            verify { sharedPrefsManager.saveIamEtag("tag") }
        }
    }

    @Test
    fun givenSaveInAppMessages_whenNotFromRemote_thenIgnore() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null

            // When
            SUT.saveInAppMessages(
                InAppMessagesList(etag = "tag", isFromRemote = false)
            )

            // Then
            verify(exactly = 0) { databaseManagerInApp.deleteAllInAppMessages() }
            verify(exactly = 0) { databaseManagerInApp.insertInAppMessages(emptyList()) }
            verify(exactly = 0) { sharedPrefsManager.saveIamEtag("tag") }
        }
    }

    @Test
    fun givenUpdateInAppMessages_whenNotFromRemote_thenIgnore() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null
            val inapp = InAppMessage(
                messageId = 2,
                messageInstanceId = 2,
                displayRules = DisplayRules(
                    frequency = FrequencyDisplayRules().apply {
                        predicates.add(FrequencyRule.TimesPerTimeUnit(TimeUnit.MINUTES, 2))
                    },
                    targeting = null,
                    schedule = null,
                    async = null
                ),
                content = null,
                lastShowTime = System.currentTimeMillis() - 60000L,
                showCount = 1,
                displayRulesJson = JsonObject()
            )

            // When
            SUT.updateInAppMessages(listOf(inapp))

            // Then
            verify { databaseManagerInApp.deleteInAppMessages(listOf(inapp.toDB())) }
            verify { databaseManagerInApp.insertInAppMessages(listOf(inapp.toDB())) }
        }
    }

    @Test
    fun givenCheckUserInSegments_whenRemoteFailed_thenReturnEmptyList() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            every { sharedPrefsManager.getIamEtag() } returns null
            coEvery {
                apiClient.post(
                    url = ApiContract.InAppMessages.CheckUserInSegments,
                    jsonBody = any(),
                    responseHandler = any()
                )
            } coAnswers {
                val callback = thirdArg<ResponseCallback>()
                callback.onFailure(400, null, null)
            }

            // When
            val result = SUT.checkUserInSegments(listOf(12L))

            // Then
            assertEquals(emptyList<AsyncRulesCheckResult>(), result)
        }
    }

    @Test
    fun givenCheckUserInSegments_whenRemoteSuccess_thenReturnDataList() {
        runTest {
            // Given
            createRepository(StandardTestDispatcher(testScheduler))
            val response = AsyncRulesCheckResponse(
                listOf(
                    AsyncRulesCheckResult(
                        10L,
                        true
                    )
                )
            )
            every { sharedPrefsManager.getIamEtag() } returns null
            coEvery {
                apiClient.post(
                    url = ApiContract.InAppMessages.CheckUserInSegments,
                    jsonBody = any(),
                    responseHandler = any()
                )
            } coAnswers {
                val callback = thirdArg<ResponseCallback>()
                callback.onSuccess(response.toJson())
            }

            // When
            val result = SUT.checkUserInSegments(listOf(10L))

            // Then
            assertEquals(response.checks, result)
        }
    }

    private fun createRepository(dispatcher: CoroutineDispatcher) {
        SUT = IamRepositoryImpl(
            application,
            apiClient,
            sharedPrefsManager,
            databaseManagerInApp,
            dispatcher
        )
    }
}