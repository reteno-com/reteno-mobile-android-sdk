package com.reteno.core.internal.action.user

import com.reteno.core.domain.controller.IamController
import com.reteno.core.internal.action.trace.ActionRunner
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
    private val iamController: IamController,
    scope: CoroutineScope,
    private val runner: ActionRunner
) : SegmentationRefresh {

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            requests.debounce(DEBOUNCE_MILLIS).collect {
                runner("RefreshSegmentation") {
                    step("refresh in-app segmentation") {
                        iamController.refreshSegmentation()
                    }
                }
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
