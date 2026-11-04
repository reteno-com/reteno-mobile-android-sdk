package com.reteno.core.fixtures

import com.reteno.core.domain.model.event.Event
import com.reteno.core.internal.action.event.LogEvent
import com.reteno.core.internal.action.event.LogScreenViewEvent
import io.mockk.coEvery
import io.mockk.mockk

internal fun recordingLogEvent(logged: MutableList<Event>): LogEvent = mockk<LogEvent>().also {
    coEvery { it(any()) } answers {
        logged += firstArg<Event>()
        Result.success(Unit)
    }
}

internal fun recordingScreenView(logged: MutableList<String>): LogScreenViewEvent = mockk<LogScreenViewEvent>().also {
    coEvery { it(any()) } answers {
        logged += firstArg<String>()
        Result.success(Unit)
    }
}
