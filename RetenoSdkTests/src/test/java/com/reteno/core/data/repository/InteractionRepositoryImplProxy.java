package com.reteno.core.data.repository;

import com.reteno.core.data.remote.api.ApiClient;
import com.reteno.core.data.remote.ds.InteractionRepositoryImpl;
import com.reteno.core.domain.ResponseCallback;
import com.reteno.core.model.interaction.Interaction;

class InteractionRepositoryImplProxy {

    private InteractionRepositoryImpl interactionRepositoryImpl;

    InteractionRepositoryImplProxy(ApiClient apiClient) {
        interactionRepositoryImpl = new InteractionRepositoryImpl(apiClient);
    }

    void sendInteraction(
            String interactionId,
            Interaction interaction,
            ResponseCallback responseHandler
    ) {
        interactionRepositoryImpl.sendInteraction(interactionId, interaction, responseHandler);
    }
}
