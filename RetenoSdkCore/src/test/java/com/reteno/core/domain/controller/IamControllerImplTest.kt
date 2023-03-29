package com.reteno.core.domain.controller

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.data.repository.IamRepository
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.controller.IamControllerImpl.Companion.TIMEOUT
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor.*
import org.mockito.Mock.*
import org.mockito.Mockito.*


@OptIn(ExperimentalCoroutinesApi::class)
class IamControllerImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val WIDGET_ID = "widgetId"
        private const val WIDGET = "widgetContent"
        private const val BASE_HTML_PREFIX = "BaseHtmlContentPrefix"
        private const val BASE_HTML_SUFFIX = "BaseHtmlContentSuffix"
        private const val BASE_HTML_CONTENT = "$BASE_HTML_PREFIX\${documentModel}$BASE_HTML_SUFFIX"
        private const val FULL_HTML = "$BASE_HTML_PREFIX$WIDGET$BASE_HTML_SUFFIX"

        private const val DELAY_BASE_HTML = 1000L
        private const val DELAY_WIDGET = 1000L
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var iamRepository: IamRepository

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    private lateinit var SUT: IamController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        Dispatchers.setMain(dispatcher)

        SUT = IamControllerImpl(iamRepository)

        coEvery { iamRepository.getBaseHtml() } coAnswers {
            delay(DELAY_BASE_HTML)
            BASE_HTML_CONTENT
        }
        coEvery { iamRepository.getWidgetRemote(any()) } coAnswers {
            delay(DELAY_WIDGET)
            WidgetModel(WIDGET)
        }
    }

    override fun after() {
        super.after()
        Dispatchers.resetMain()
    }

    @Test
    fun given_whenFetchIamFullHtml_thenShouldLoadDataConcurrently() {
        // When
        SUT.fetchIamFullHtml(WIDGET_ID)
        scheduler.advanceUntilIdle()

        // Then
        assertEquals(DELAY_BASE_HTML, scheduler.currentTime)
    }

    @Test
    fun given_whenFetchIamFullHtml_thenFullHtmlFlowChangesToSuccess() {
        // Given
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)

        // Then
        scheduler.advanceTimeBy(DELAY_BASE_HTML)
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        scheduler.runCurrent()
        assertEquals(ResultDomain.Success(FULL_HTML), SUT.fullHtmlStateFlow.value)

        SUT.reset()
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
    }

    @Test
    fun given_whenFetchIamFullHtmlTimeoutReached_thenFullHtmlFlowChangesToError() {
        coEvery { iamRepository.getBaseHtml() } coAnswers {
            delay(TIMEOUT)
            BASE_HTML_CONTENT
        }

        // Given
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)

        // Then
        scheduler.advanceTimeBy(TIMEOUT)
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        scheduler.runCurrent()
        assert(SUT.fullHtmlStateFlow.value is ResultDomain.Error)
        val result = SUT.fullHtmlStateFlow.value as ResultDomain.Error
        assert(result.errorBody.contains("TIMEOUT"))

        SUT.reset()
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
    }

    @Test
    fun given_whenReset_thenClearDataSetLoading() {
        // Given
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)

        // When
        SUT.fetchIamFullHtml(WIDGET_ID)
        scheduler.advanceTimeBy(DELAY_BASE_HTML)

        // Then
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
        // When
        scheduler.runCurrent()
        // Then
        assertEquals(ResultDomain.Success(FULL_HTML), SUT.fullHtmlStateFlow.value)
        // When
        SUT.reset()
        // Then
        assertEquals(ResultDomain.Loading, SUT.fullHtmlStateFlow.value)
    }
}