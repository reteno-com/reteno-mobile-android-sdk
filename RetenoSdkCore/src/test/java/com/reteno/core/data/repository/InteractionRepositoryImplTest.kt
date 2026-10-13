package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class InteractionRepositoryImplTest : BaseRobolectricTest() {

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

        private const val SERVER_ERROR_NON_REPEATABLE = 500
        private const val SERVER_ERROR_REPEATABLE = 400
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var apiClient: ApiClient
    @RelaxedMockK
    private lateinit var databaseManagerInteraction: RetenoDatabaseManagerInteraction
    @RelaxedMockK
    private lateinit var databaseManagerInAppInteraction: RetenoDatabaseManagerInAppInteraction

    private lateinit var SUT: InteractionRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        SUT = InteractionRepositoryImpl(databaseManagerInteraction, databaseManagerInAppInteraction) {}
    }

    @Test
    fun givenValidInteraction_whenInteractionSent_thenSaveInteraction() = runTest {
        // Given
        val interaction = Interaction(INTERACTION_STATUS, CURRENT_TIMESTAMP, TOKEN)
        val dbInteraction = interaction.toDb(INTERACTION_ID)

        // When
        SUT.saveInteraction(INTERACTION_ID, interaction)

        // Then
        verify(exactly = 1) { databaseManagerInteraction.insertInteraction(dbInteraction) }
    }

}