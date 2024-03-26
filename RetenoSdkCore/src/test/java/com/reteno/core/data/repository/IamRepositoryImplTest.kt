package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.domain.ResponseCallback
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test


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
    private lateinit var sharedPrefsManager: SharedPrefsManager

    @RelaxedMockK
    private lateinit var databaseManager: RetenoDatabaseManagerInAppMessages

    private lateinit var SUT: IamRepository
    // endregion helper fields ---------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun before() {
        super.before()
        SUT = IamRepositoryImpl(apiClient, sharedPrefsManager, databaseManager, UnconfinedTestDispatcher())
    }

    @Test
    fun givenBaseHtmlVersionRemoteDiffersFromLocal_whenGetBaseHtml_thenBaseHtmlContentRemoteSavedLocallyLocalVersionUpdated() {
        runBlocking {
            // Given
            every { sharedPrefsManager.getIamBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
            every { apiClient.head(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                val headers = mapOf<String, List<String>>(HEADER_X_AMZ_META_VERSION to listOf<String>(BASE_HTML_VERSION_REMOTE))
                callback.onSuccess(headers, BASE_HTML_VERSION_REMOTE)
            }
            every { apiClient.get(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                callback.onSuccess(BASE_HTML_CONTENT_REMOTE)
            }

            // When
            val result = SUT.getBaseHtml()

            // Then
            verify(exactly = 1) { sharedPrefsManager.saveIamBaseHtmlVersion(eq(BASE_HTML_VERSION_REMOTE)) }
            verify(exactly = 1) { sharedPrefsManager.saveIamBaseHtmlContent(eq(BASE_HTML_CONTENT_REMOTE)) }
            assertEquals(BASE_HTML_CONTENT_REMOTE, result)
        }
    }

    @Test
    fun givenBaseHtmlVersionRemoteEqualsLocal_whenGetBaseHtml_thenBaseHtmlContentLocalReturned() {
        runBlocking {
            // Given
            every { sharedPrefsManager.getIamBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
            every { sharedPrefsManager.getIamBaseHtmlContent() } returns BASE_HTML_CONTENT_LOCAL
            every { apiClient.head(ApiContract.InAppMessages.BaseHtml, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                val headers = mapOf<String, List<String>>(HEADER_X_AMZ_META_VERSION to listOf<String>(BASE_HTML_VERSION_LOCAL))
                callback.onSuccess(headers, BASE_HTML_VERSION_LOCAL)
            }

            // When
            val result = SUT.getBaseHtml()

            // Then
            assertEquals(BASE_HTML_CONTENT_LOCAL, result)
        }
    }

  /*  @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun whenGetWidget_thenDispatchersIoUsed() {
        runBlocking {
            // Given
            val interactionId = "widgetIdHere"
            val widget = "widgetJsonHere"

            var usedThreadName: String? = null

            coEvery { apiClient.get(ApiContract.InAppMessages.GetInnAppWidgetByInteractionId(interactionId), any(), any()) }  coAnswers  {
                val callback = thirdArg<ResponseCallback>()
                usedThreadName = Thread.currentThread().name
                callback.onSuccess(mapOf<String, List<String>>(), widget)
            }

            // When
            SUT.getWidgetRemote(interactionId)

            // Then
            assertNotNull(usedThreadName)
            val expectedPrefix = "DefaultDispatcher-worker-"
            assert(usedThreadName!!.startsWith(expectedPrefix))
        }
    }*/
}