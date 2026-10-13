package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.recordingLogEvent
import com.reteno.core.internal.lifecycle.AppForegroundState
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.LifecycleEvents
import kotlinx.coroutines.test.TestScope

internal class LifecycleActionFixture(scope: TestScope, options: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL) {
    val logged = mutableListOf<Event>()
    val filter = LifecycleEventFilter(options)
    val lifecycleEvents = LifecycleEvents(filter, recordingLogEvent(logged))
    val platform = FakeAppPlatform()
    val state = AppForegroundState()
    val traced = TestActionRunner(scope)

    val loggedKeys: List<String>
        get() = logged.map { it.eventTypeKey }
}
