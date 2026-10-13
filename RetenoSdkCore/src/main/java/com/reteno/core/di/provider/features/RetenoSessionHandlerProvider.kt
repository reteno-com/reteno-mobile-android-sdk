package com.reteno.core.di.provider.features

import com.reteno.core.di.base.ProviderStrongReference
import com.reteno.core.di.provider.SharedPrefsManagerProvider
import com.reteno.core.internal.action.event.LogEvent
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.SessionTracker
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

internal class RetenoSessionHandlerProvider(
    private val sharedPrefsManagerProvider: SharedPrefsManagerProvider,
    private val lifecycleEventFilter: LifecycleEventFilter,
    private val logEvent: () -> LogEvent
) : ProviderStrongReference<RetenoSessionHandler>() {

    override fun create(): RetenoSessionHandler = SessionTracker(
        prefs = sharedPrefsManagerProvider.get(),
        filter = lifecycleEventFilter,
        logEvent = logEvent(),
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        clock = System::currentTimeMillis,
        newSessionId = { UUID.randomUUID().toString() }
    )
}
