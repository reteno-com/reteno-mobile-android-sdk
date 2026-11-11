package com.reteno.core.internal.action.user

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.log.RemoteLog

internal fun ActionTrace.languageCodeToKeep(languageCode: String?, remoteLog: RemoteLog, action: String): String? {
    val code = languageCode ?: return null
    val valid = UserAttributesValidator.languageCodeWithRegion(code)
    if (valid == null) {
        warn { "languageCode dropped: it must contain a region, e.g. de-AT" }
        remoteLog.record("$action: languageCode without region dropped", LogLevel.WARNING)
    }
    return valid
}
