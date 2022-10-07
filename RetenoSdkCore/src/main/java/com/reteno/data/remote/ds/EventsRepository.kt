package com.reteno.data.remote.ds

import com.reteno.domain.ResponseCallback
import com.reteno.model.Events

interface EventsRepository {

    fun sendOutcomeEvent(events: Events, responseHandler: ResponseCallback)

}