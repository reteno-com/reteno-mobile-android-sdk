package com.reteno.core.features.recommendation

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.domain.controller.RecommendationController
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.IllegalArgumentException

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationImplTest : BaseUnitTest() {


    @RelaxedMockK
    private lateinit var controller: RecommendationController


    @Test
    fun whenFetchRecommendationCalled_thenCallMirroredToController() = runTest {
        val sut = createSUT()
        sut.fetchRecommendation(
            "",
            RecomRequest(null, null),
            RecomBase::class.java,
            object : GetRecommendationResponseCallback<RecomBase> {
                override fun onSuccess(response: Recoms<RecomBase>) {

                }

                override fun onSuccessFallbackToJson(response: String) {

                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {

                }

            })

        verify { controller.getRecommendation<RecomBase>(any(), any(), any(), any()) }
    }

    @Test
    fun whenFetchRecommendationCalledWIthSuccessResponse_thenCallbackCalled() = runTest {
        val sut = createSUT()
        var result: Recoms<RecomBase>? = null
        val callback = object : GetRecommendationResponseCallback<RecomBase> {
            override fun onSuccess(response: Recoms<RecomBase>) {
                result = response
            }

            override fun onSuccessFallbackToJson(response: String) {
                result = null
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                result = null
            }

        }
        coEvery { controller.getRecommendation<RecomBase>(any(), any(), any(), any()) } answers {
            arg<GetRecommendationResponseCallback<RecomBase>>(3).onSuccess(Recoms(emptyList()))
        }
        sut.fetchRecommendation("", RecomRequest(null, null), RecomBase::class.java, callback)

        assertEquals(Recoms(emptyList()), result)
    }

    @Test
    fun whenFetchRecommendationCalledWIthErrorResponse_thenCallbackCalled() = runTest {
        val sut = createSUT()
        var result: Throwable? = null
        val callback = object : GetRecommendationResponseCallback<RecomBase> {
            override fun onSuccess(response: Recoms<RecomBase>) {
                result = null
            }

            override fun onSuccessFallbackToJson(response: String) {
                result = null
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                result = throwable
            }

        }
        coEvery { controller.getRecommendation<RecomBase>(any(), any(), any(), any()) } answers {
            arg<GetRecommendationResponseCallback<RecomBase>>(3).onFailure(400, null, IllegalArgumentException())
        }
        sut.fetchRecommendation("", RecomRequest(null, null), RecomBase::class.java, callback)

        assertTrue(result is IllegalArgumentException)
    }

    private fun createSUT() = RecommendationImpl(controller)
}