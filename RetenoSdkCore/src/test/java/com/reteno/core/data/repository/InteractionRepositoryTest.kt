package com.reteno.core.data.repository

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction
import com.reteno.core.model.interaction.InteractionStatus
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class InteractionRepositoryTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val TOKEN = "some_token"
        private const val INTERACTION_ID = "interaction_id"
        private val INTERACTION_STATUS = InteractionStatus.DELIVERED
        private const val CURRENT_TIMESTAMP = "2022-11-22T11:11:11Z"

        private val EXPECTED_API_CONTRACT_URL =
            ApiContract.RetenoApi.InteractionStatus(INTERACTION_ID).url
        private const val EXPECTED_URL =
            "https://api.reteno.com/api/v1/interactions/$INTERACTION_ID/status"
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    // endregion helper fields ---------------------------------------------------------------------

    private lateinit var SUT: InteractionRepositoryImpl

    @Before
    override fun before() {
        super.before()
        SUT = InteractionRepositoryImpl(apiClient)
    }

    @Test
    fun givenValidInteraction_whenInteractionSent_thenApiClientPutsInteractionWithCorrectParameters() {
        // Given
        val interaction = Interaction(INTERACTION_STATUS, CURRENT_TIMESTAMP, TOKEN)
        val expectedInteractionJson =
            "{\"status\":\"$INTERACTION_STATUS\",\"time\":\"$CURRENT_TIMESTAMP\",\"token\":\"$TOKEN\"}"

        val apiContractCaptured = slot<ApiContract>()
        val jsonBodyCaptured = slot<String>()
        every {
            apiClient.put(
                url = capture(apiContractCaptured),
                jsonBody = capture(jsonBodyCaptured),
                responseHandler = any()
            )
        } just runs

        // When
        SUT.sendInteraction(INTERACTION_ID, interaction, object : ResponseCallback {
            override fun onSuccess(response: String) {}
            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {}
        })

        // Then
        verify(exactly = 1) { apiClient.put(any(), any(), any()) }

        assertEquals(EXPECTED_API_CONTRACT_URL, EXPECTED_URL)
        assertEquals(EXPECTED_API_CONTRACT_URL, apiContractCaptured.captured.url)
        assertEquals(expectedInteractionJson, jsonBodyCaptured.captured)
    }
}