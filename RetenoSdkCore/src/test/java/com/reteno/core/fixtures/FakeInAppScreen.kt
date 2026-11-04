package com.reteno.core.fixtures

import com.reteno.core.domain.controller.IamFetchResult
import com.reteno.core.internal.iam.InAppScreen
import com.reteno.core.internal.iam.InAppScreenEvents

internal class FakeInAppScreen : InAppScreen {
    val calls = mutableListOf<String>()
    var events: InAppScreenEvents? = null
    var canPresent = true
    var presents = true
    var creates = true
    var deliversCustomData = true
    val customData = mutableListOf<Map<String, String>>()
    var htmlId = "nothing created"

    override fun attach(events: InAppScreenEvents) {
        this.events = events
    }

    override fun create(html: IamFetchResult): Boolean {
        calls += "create ${html.id}"
        htmlId = html.id
        return creates
    }

    override fun canPresent(): Boolean = canPresent

    override fun present(): Boolean {
        calls += "present"
        return presents
    }

    override fun resize(heightDp: Int) {
        calls += "resize $heightDp"
    }

    override fun hide() {
        calls += "hide"
    }

    override fun destroy() {
        calls += "destroy"
    }

    override fun openLink(url: String) {
        calls += "open $url"
    }

    override fun sendCustomData(data: Map<String, String>): Boolean {
        customData += data
        return deliversCustomData
    }

    fun postWidgetMessage(message: String?) {
        events!!.onWidgetMessage(htmlId, message)
    }

    fun dismiss() {
        events!!.onDismissed(htmlId)
    }
}
