package com.reteno.core.internal.log

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.platform.AppPlatform

internal fun interface LogEventQueue {
    fun submitRetenoLogEvent(event: RetenoLogEvent)
}

internal class RemoteLogRecorder(
    private val configRepository: Lazy<ConfigRepository>,
    private val platform: AppPlatform,
    private val queue: LogEventQueue
) : RemoteLog {

    private val origin by lazy { platform.logEventOrigin() }

    override fun record(message: String, level: LogLevel) {
        submit(level) { Redact.uploadable(message) }
    }

    override fun recordError(message: String, error: Throwable) {
        submit(LogLevel.ERROR) { "${Redact.uploadable(message)}: ${Redact.uploadableStackTrace(error)}" }
    }

    override fun recordEvent(event: RetenoLogEvent) {
        RemoteLogGuard.unlessMuted {
            val uploadable = event.copy(
                errorMessage = event.errorMessage?.let(Redact::uploadable),
                version = event.version ?: origin.appVersion,
                bundleId = origin.packageName,
                deviceId = deviceId()
            )
            queue.submitRetenoLogEvent(uploadable)
        }
    }

    private fun submit(level: LogLevel, message: () -> String) {
        RemoteLogGuard.unlessMuted {
            val event = RetenoLogEvent(
                osVersion = origin.osVersion,
                version = origin.appVersion,
                device = origin.deviceModel,
                logLevel = level,
                errorMessage = message(),
                bundleId = origin.packageName,
                deviceId = deviceId()
            )
            queue.submitRetenoLogEvent(event)
        }
    }

    private fun deviceId(): String =
        runCatching { configRepository.value.getDeviceId().id }.getOrDefault(UNKNOWN_DEVICE)

    private companion object {
        const val UNKNOWN_DEVICE = "uninitialized"
    }
}
