package com.reteno.core.fixtures

import com.reteno.core.internal.action.device.DeviceLanguage
import com.reteno.core.internal.action.trace.ActionTrace
import java.util.Locale

internal class FakeDeviceLanguage(var detected: String = "en-US") : DeviceLanguage {
    var explicit: String? = null
    val calls = mutableListOf<String>()

    override suspend fun resolve(trace: ActionTrace): String = explicit ?: detected

    override suspend fun settle(trace: ActionTrace, displayed: Locale) {
        calls += "settle ${displayed.toLanguageTag()}"
    }

    override suspend fun useExplicit(trace: ActionTrace, languageCode: String) {
        calls += "use explicit $languageCode"
        explicit = languageCode
    }

    override suspend fun useDetected(trace: ActionTrace): Boolean {
        calls += "use detected"
        val forgotten = explicit != null
        explicit = null
        return forgotten
    }
}
