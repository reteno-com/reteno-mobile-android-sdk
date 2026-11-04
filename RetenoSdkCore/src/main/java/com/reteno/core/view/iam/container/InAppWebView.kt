package com.reteno.core.view.iam.container

import android.content.Context
import android.graphics.Color
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.reteno.core.features.iam.RetenoAndroidHandler
import com.reteno.core.internal.log.Redact
import com.reteno.core.util.Logger
import com.reteno.core.view.iam.AndroidInAppScreen

internal fun createInAppWebView(context: Context, jsInterface: RetenoAndroidHandler, html: String): WebView =
    WebView(context).apply {
        webChromeClient = InAppConsole
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        setBackgroundColor(Color.TRANSPARENT)
        addJavascriptInterface(jsInterface, AndroidInAppScreen.JS_INTERFACE_NAME)
        loadDataWithBaseURL("", html, "text/html", "UTF-8", "")
    }

internal object InAppConsole : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Logger.d(
            TAG,
            "onConsoleMessage(): ",
            Redact.text(consoleMessage.message()),
            " at line ",
            consoleMessage.lineNumber()
        )
        return true
    }

    private const val TAG = "InAppWebView"
}
