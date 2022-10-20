package com.reteno.core.data.repository

import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction

internal class InteractionRepositoryImpl(private val apiClient: ApiClient) : InteractionRepository {

    override fun sendInteraction(
        interactionId: String,
        interaction: Interaction,
        responseHandler: ResponseCallback
    ) {
        apiClient.put(
            ApiContract.RetenoApi.InteractionStatus(interactionId),
            interaction.toJson(),
            responseHandler
        )
    }
}