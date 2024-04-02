package com.reteno.core.view.iam.callback

interface InAppLifecycleCallback {
    fun beforeDisplay(inAppData: InAppData)
    fun onDisplay(inAppData: InAppData)
    fun beforeClose(closeData: InAppCloseData)
    fun afterClose(closeData: InAppCloseData)
    fun onError(errorData: InAppErrorData)
}