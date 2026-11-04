package com.reteno.core.internal.iam

import com.reteno.core.domain.controller.IamFetchResult

internal interface InAppScreen {
    fun attach(events: InAppScreenEvents)

    fun create(html: IamFetchResult): Boolean

    fun canPresent(): Boolean

    fun present(): Boolean

    fun resize(heightDp: Int)

    fun hide()

    fun destroy()

    fun openLink(url: String)

    fun sendCustomData(data: Map<String, String>): Boolean
}

internal interface InAppScreenEvents {
    fun onWidgetMessage(htmlId: String, message: String?)

    fun onDismissed(htmlId: String)

    fun onActivityStarted()

    fun onActivityStopped()
}
