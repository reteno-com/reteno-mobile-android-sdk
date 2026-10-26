package com.reteno.core.fixtures

import com.reteno.core.internal.log.RemoteLog
import com.reteno.core.internal.log.RetenoLogSink
import com.reteno.core.util.Logger

internal fun Logger.installCapturing(sink: RetenoLogSink, remoteLog: RemoteLog) {
    install(FakeLogSink(), FakeRemoteLog())
    install(sink, remoteLog)
}
