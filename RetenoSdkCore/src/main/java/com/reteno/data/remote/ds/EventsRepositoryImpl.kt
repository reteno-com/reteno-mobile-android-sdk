package com.reteno.data.remote.ds

import com.reteno.data.remote.api.ApiClient
import com.reteno.data.remote.api.ApiContract
import com.reteno.data.remote.mapper.toJson
import com.reteno.domain.ResponseCallback
import com.reteno.model.Events

class EventsRepositoryImpl(
    private val apiClient: ApiClient
) : EventsRepository {

    override fun sendOutcomeEvent(events: Events, responseHandler: ResponseCallback) {
        apiClient.post(ApiContract.MobileApi.Events, events.toJson(), responseHandler)
    }

}