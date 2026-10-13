package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.util.Util.formatToRemote
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

internal class ClearOldDataAction(
    private val interactionsDb: RetenoDatabaseManagerInteraction,
    private val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction,
    private val eventsDb: RetenoDatabaseManagerEvents,
    private val inboxDb: RetenoDatabaseManagerAppInbox,
    private val recomEventsDb: RetenoDatabaseManagerRecomEvents,
    private val wrappedLinksDb: RetenoDatabaseManagerWrappedLink,
    private val outdatedTime: () -> ZonedDateTime,
    private val reportRemoved: (String) -> Unit,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(): Result<Unit> = runner("ClearOldData") {
        val before = outdatedTime().formatToRemote()
        args("before" to before)
        clearInteractions(before)
        clearInAppInteractions(before)
        clearEvents(before)
        clearInboxStatuses(before)
        clearRecommendationEvents(before)
        clearWrappedLinks(before)
    }

    private suspend fun clearInteractions(before: String) = runner("ClearOldInteractions") {
        val removed = step("delete outdated interactions from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { interactionsDb.deleteInteractionByTime(before) }
        }
        if (removed.isNotEmpty()) reportRemovedRows { removed.groupingBy { it.status }.eachCount().map { (status, count) -> "Removed interactions($status) - $count" } }
    }

    private suspend fun clearInAppInteractions(before: String) = runner("ClearOldInAppInteractions") {
        val removed = step("delete outdated in-app interactions from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { inAppInteractionsDb.deleteInteractionsByTime(before) }
        }
        if (removed.isNotEmpty()) reportRemovedRows { removed.groupingBy { it.status }.eachCount().map { (status, count) -> "Removed inAppInteractions($status) - $count" } }
    }

    private suspend fun clearEvents(before: String) = runner("ClearOldEvents") {
        val removed = step("delete outdated events from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { eventsDb.deleteEventsByTime(before) }
        }
        if (removed.isNotEmpty()) reportRemovedRows { removed.groupingBy { it.eventTypeKey }.eachCount().map { (type, count) -> "Removed events($type) - $count" } }
    }

    private suspend fun clearInboxStatuses(before: String) = runner("ClearOldInboxStatuses") {
        val removed = step("delete outdated inbox statuses from local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) { inboxDb.deleteAppInboxMessagesByTime(before) }
        }
        if (removed > 0) reportRemovedRows { listOf("Outdated Inbox: - $removed") }
    }

    private suspend fun clearRecommendationEvents(before: String) = runner("ClearOldRecommendationEvents") {
        val removed = step("delete outdated recommendation events from local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) { recomEventsDb.deleteRecomEventsByTime(before) }
        }
        if (removed > 0) reportRemovedRows { listOf("Outdated Recommendation Events: - $removed") }
    }

    private suspend fun clearWrappedLinks(before: String) = runner("ClearOldWrappedLinks") {
        val removed = step("delete outdated link clicks from local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) { wrappedLinksDb.deleteWrappedLinksByTime(before) }
        }
        if (removed > 0) reportRemovedRows { listOf("Removed wrapped links - $removed") }
    }

    private suspend fun ActionTrace.reportRemovedRows(messages: () -> List<String>) {
        step("report removed rows to remote log") { messages().forEach(reportRemoved) }
    }
}
