package com.reteno.core.internal.action.push

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.withContext

internal class SaveDefaultNotificationChannelAction(
    private val configRepository: ConfigRepository,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(channel: String): Result<Unit> = runner("SaveDefaultNotificationChannel") {
        step("save default notification channel to local settings") {
            withContext(dispatchers.io) { configRepository.saveDefaultNotificationChannel(channel) }
        }
    }
}
