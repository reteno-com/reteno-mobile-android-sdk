package com.reteno.core.data.remote.ds

import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.Events

class EventsRepositoryImpl(
    private val apiClient: ApiClient
) : EventsRepository {

    override fun sendOutcomeEvent(events: Events, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.Events, events.toJson(), responseHandler)
    }

}