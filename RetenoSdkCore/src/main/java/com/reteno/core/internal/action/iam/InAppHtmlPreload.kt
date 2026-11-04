package com.reteno.core.internal.action.iam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface InAppHtmlPreloading {
    fun request()
}

internal class InAppHtmlPreload(
    private val preload: PreloadIamHtml,
    private val scope: CoroutineScope
) : InAppHtmlPreloading {

    override fun request() {
        scope.launch { preload() }
    }
}
