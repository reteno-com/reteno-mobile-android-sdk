package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.domain.ResponseCallback
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


class InAppMessagesRepositoryImplTest : BaseRobolectricTest() {

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

    private lateinit var SUT: InAppMessagesRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = InAppMessagesRepositoryImpl(apiClient, sharedPrefsManager)
    }

    @Test
    fun givenBaseHtmlVersionRemoteDiffersFromLocal_whenGetBaseHtml_thenBaseHtmlContentRemoteSavedLocallyLocalVersionUpdated() {
        runBlocking {
            // Given
            every { sharedPrefsManager.getInAppMessagesBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
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
            verify(exactly = 1) { sharedPrefsManager.saveInAppMessagesBaseHtmlVersion(eq(BASE_HTML_VERSION_REMOTE)) }
            verify(exactly = 1) { sharedPrefsManager.saveInAppMessagesBaseHtmlContent(eq(BASE_HTML_CONTENT_REMOTE)) }
            assertEquals(BASE_HTML_CONTENT_REMOTE, result)
        }
    }

    @Test
    fun givenBaseHtmlVersionRemoteEqualsLocal_whenGetBaseHtml_thenBaseHtmlContentLocalReturned() {
        runBlocking {
            // Given
            every { sharedPrefsManager.getInAppMessagesBaseHtmlVersion() } returns BASE_HTML_VERSION_LOCAL
            every { sharedPrefsManager.getInAppMessagesBaseHtmlContent() } returns BASE_HTML_CONTENT_LOCAL
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

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun whenGetWidget_thenDispatchersIoUsed() {
        runBlocking {
            // Given
            val widgetId = "widgetIdHere"
            val widget = "widgetJsonHere"

            var usedThreadName: String? = null

            every { apiClient.get(ApiContract.MobileApi.InAppMessages, any(), any()) } answers {
                val callback = thirdArg<ResponseCallback>()
                usedThreadName = Thread.currentThread().name
                callback.onSuccess(mapOf<String, List<String>>(), widget)
            }

            // When
            SUT.getWidget(widgetId)

            // Then
            assertNotNull(usedThreadName)
            val expectedPrefix = "DefaultDispatcher-worker-"
            assert(usedThreadName!!.startsWith(expectedPrefix))
        }
    }
}