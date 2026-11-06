package com.reteno.core.features.iam

import android.webkit.JavascriptInterface
import com.reteno.core.util.Logger

internal abstract class RetenoAndroidHandler {

    @JavascriptInterface
    fun postMessage(event: String?): String {
        /*@formatter:off*/ Logger.d(TAG, "postMessage(): WEB -> NATIVE")
        /*@formatter:on*/
        onMessagePosted(event)
        return ""
    }

    abstract fun onMessagePosted(event: String?)

    companion object {
        private val TAG: String = RetenoAndroidHandler::class.java.simpleName
    }
}