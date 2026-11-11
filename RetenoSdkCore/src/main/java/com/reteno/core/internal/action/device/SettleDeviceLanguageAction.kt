package com.reteno.core.internal.action.device

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.HostUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

internal interface SettleDeviceLanguage {
    suspend operator fun invoke(): Result<Unit>
}

internal class SettleDeviceLanguageAction(
    private val deviceLanguage: DeviceLanguage,
    private val hostUi: HostUi,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SettleDeviceLanguage {

    @Volatile
    private var settledInThisProcess = false

    override suspend operator fun invoke(): Result<Unit> = runner("SettleDeviceLanguage") {
        settle()
    }

    private suspend fun ActionTrace.settle() {
        if (settledInThisProcess) {
            note { "device language already settled in this process" }
            return
        }
        val displayed = step("wait for the visible screen to stay resumed for a second") {
            awaitSettledScreen()
        }
        deviceLanguage.settle(this, displayed)
        settledInThisProcess = true
    }

    private suspend fun awaitSettledScreen(): Locale {
        val resumes = Channel<Unit>(Channel.CONFLATED)
        hostUi.watchResumedActivities { resumes.trySend(Unit) }
        try {
            return settledScreenLocale(resumes)
        } finally {
            hostUi.stopWatchingResumedActivities()
        }
    }

    private suspend fun settledScreenLocale(resumes: Channel<Unit>): Locale {
        resumes.receive()
        while (true) {
            val resumedAgain = withTimeoutOrNull(QUIET_MILLIS) { resumes.receive() }
            if (resumedAgain == null) {
                val locale = withContext(dispatchers.main) { hostUi.displayedLocale() }
                if (locale != null) return locale
                resumes.receive()
            }
        }
    }
}

private const val QUIET_MILLIS = 1_000L
