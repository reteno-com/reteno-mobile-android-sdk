package com.reteno.core.internal.action.user

import com.reteno.core.internal.action.iam.CheckInAppSegments
import com.reteno.core.internal.iam.InAppMessagesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

internal interface SegmentationRefresh {
    fun request()
}

@OptIn(FlowPreview::class)
internal class SegmentationRefreshImpl(
    private val checkSegments: CheckInAppSegments,
    private val state: InAppMessagesState,
    scope: CoroutineScope
) : SegmentationRefresh {

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            requests.debounce(DEBOUNCE_MILLIS).collect {
                checkSegments(state.messages(), persist = false)
            }
        }
    }

    override fun request() {
        requests.tryEmit(Unit)
    }

    companion object {
        const val DEBOUNCE_MILLIS = 5_000L
    }
}
