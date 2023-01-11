package com.reteno.core.features.inapp

import android.webkit.JavascriptInterface
import com.reteno.core.util.Logger

internal abstract class RetenoAndroidHandler {

    @JavascriptInterface
    fun postMessage(event: String?): String {
        /*@formatter:off*/ Logger.i(TAG, "postMessage(): WEB -> NATIVE ", "event = [", event, "]")
        /*@formatter:on*/
        onMessagePosted(event)
        return ""
    }

    abstract fun onMessagePosted(event: String?)

    companion object {
        private val TAG: String = RetenoAndroidHandler::class.java.simpleName
    }
}