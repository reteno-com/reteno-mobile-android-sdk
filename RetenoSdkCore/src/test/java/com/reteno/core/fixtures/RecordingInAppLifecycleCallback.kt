package com.reteno.core.fixtures

import com.reteno.core.view.iam.callback.InAppCloseData
import com.reteno.core.view.iam.callback.InAppData
import com.reteno.core.view.iam.callback.InAppErrorData
import com.reteno.core.view.iam.callback.InAppLifecycleCallback

internal class RecordingInAppLifecycleCallback(private val main: MarkingDispatcher) : InAppLifecycleCallback {
    val calls = mutableListOf<String>()
    val calledOnMain = mutableListOf<Boolean>()
    val failing = mutableSetOf<String>()

    override fun beforeDisplay(inAppData: InAppData) = record("beforeDisplay ${inAppData.describe()}")

    override fun onDisplay(inAppData: InAppData) = record("onDisplay ${inAppData.describe()}")

    override fun beforeClose(closeData: InAppCloseData) =
        record("beforeClose ${closeData.describe()} ${closeData.closeAction}")

    override fun afterClose(closeData: InAppCloseData) =
        record("afterClose ${closeData.describe()} ${closeData.closeAction}")

    override fun onError(errorData: InAppErrorData) =
        record("onError ${errorData.describe()} ${errorData.errorMessage}")

    private fun InAppData.describe() = "$source $id"

    private fun record(call: String) {
        calls += call
        calledOnMain += main.isRunningHere
        if (call.substringBefore(" ") in failing) throw IllegalStateException("host failed in $call")
    }
}
