package com.reteno.core.fixtures

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.iam.DisplayOffer
import com.reteno.core.internal.action.iam.InAppDisplay

internal class RecordingInAppDisplay(var offer: DisplayOffer = DisplayOffer.LOADING) : InAppDisplay {
    val shown = mutableListOf<Long>()

    override suspend fun show(message: InAppMessage): DisplayOffer {
        shown += message.messageId
        return offer
    }
}
