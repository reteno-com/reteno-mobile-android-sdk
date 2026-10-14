package com.reteno.core.data.repository

import com.reteno.core.R
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.IamJsEventType
import com.reteno.core.util.Util
import io.mockk.coEvery
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

    private fun createRepository(dispatcher: CoroutineDispatcher) {
        SUT = IamRepositoryImpl(
            application,
            apiClient,
            fileManager,
            dispatcher
        )
    }
}