package com.reteno.core.fixtures

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.internal.log.LogEventQueue
import com.reteno.core.internal.log.RemoteLogRecorder
import io.mockk.every
import io.mockk.mockk

internal class RecordingLogEventQueue : LogEventQueue {
    val events = mutableListOf<RetenoLogEvent>()
    var onSubmit: (RetenoLogEvent) -> Unit = {}

    override fun submitRetenoLogEvent(event: RetenoLogEvent) {
        events += event
        onSubmit(event)
    }
}

internal fun recordingRemoteLog(queue: RecordingLogEventQueue): RemoteLogRecorder {
    val configRepository = mockk<ConfigRepository>().also {
        every { it.getDeviceId() } returns stubDeviceId()
    }
    return RemoteLogRecorder(lazy { configRepository }, FakeAppPlatform(), queue)
}
