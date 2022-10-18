package com.reteno.core.data.remote.ds

import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.Events

interface EventsRepository {

    fun sendOutcomeEvent(events: Events, responseHandler: ResponseCallback)

}