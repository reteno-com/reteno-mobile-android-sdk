package com.reteno.core.domain.controller

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.data.repository.RecommendationRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvent
import com.reteno.core.domain.model.recommendation.post.RecomEventType
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.recommendation.GetRecommendationResponseCallback
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime


class RecommendationControllerTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val RECOM_VARIANT_ID = "r12345v123456"

        private const val PRODUCT_ID = "w12345s1345"
        private const val CATEGORY = "category_here"
        private val FIELDS = listOf<String>("field1", "field2", "field3")

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockObjectSchedulerUtils()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockObjectSchedulerUtils()
        }
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var recommendationRepository: RecommendationRepository
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: RecommendationController

    override fun before() {
        super.before()
        SUT = RecommendationController(recommendationRepository)
    }

    @Test
    fun whenGetRecommendations_thenGetRecommendationsCalledOnRepository() {
        // Given
        val recomRequest = RecomRequest(listOf(PRODUCT_ID), CATEGORY, FIELDS)
        val responseCallback: GetRecommendationResponseCallback<ResponseFull> = ResponseCallback()

        // When
        SUT.getRecommendation(
            RECOM_VARIANT_ID,
            recomRequest,
            ResponseFull::class.java,
            responseCallback
        )

        // Then
        verify(exactly = 1) {
            recommendationRepository.getRecommendation(
                RECOM_VARIANT_ID,
                recomRequest,
                ResponseFull::class.java,
                responseCallback
            )
        }
    }

    @Test
    fun whenTrackRecommendations_thenRecomEventPassedToRepository() {
        // Given
        val recomEvent = RecomEvent(RecomEventType.CLICKS, ZonedDateTime.now(), PRODUCT_ID)
        val recomEvents = RecomEvents(RECOM_VARIANT_ID, listOf(recomEvent))

        // When
        SUT.trackRecommendations(recomEvents)

        // Then
        verify(exactly = 1) {
            recommendationRepository.saveRecommendations(recomEvents)
        }
    }

    @Test
    fun whenPushRecommendations_thenPushCalledOnRepository() {
        // When
        SUT.pushRecommendations()

        // Then
        verify(exactly = 1) {
            recommendationRepository.pushRecommendations()
        }
    }

    @Test
    fun whenClearOldRecommendations_thenClearOldRecommendationsCalledOnRepository() {
        val mockData = mockk<ZonedDateTime>()
        every { SchedulerUtils.getOutdatedTime() } returns mockData

        // When
        SUT.clearOldRecommendations()

        // Then
        verify(exactly = 1) { recommendationRepository.clearOldRecommendations(mockData) }
        verify(exactly = 1) { SchedulerUtils.getOutdatedTime() }
    }

    // region helper classes -----------------------------------------------------------------------
    private class ResponseFull(override val productId: String = PRODUCT_ID) : RecomBase

    private class ResponseCallback : GetRecommendationResponseCallback<ResponseFull> {
        override fun onSuccess(response: Recoms<ResponseFull>) {}
        override fun onSuccessFallbackToJson(response: String) {}
        override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {}
    }
    // endregion helper classes --------------------------------------------------------------------

}