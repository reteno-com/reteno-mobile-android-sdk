package com.reteno.core.data.local.database.manager


internal class RetenoDatabaseManagerImpl(
    private val deviceManager: RetenoDatabaseManagerDevice,
    private val userManager: RetenoDatabaseManagerUser,
    private val interactionManager: RetenoDatabaseManagerInteraction,
    private val eventsManager: RetenoDatabaseManagerEvents,
    private val appInboxManager: RetenoDatabaseManagerAppInbox,
    private val recomEventsManager: RetenoDatabaseManagerRecomEvents,
    private val wrappedLinkManager: RetenoDatabaseManagerWrappedLink,
    private val logEventManager: RetenoDatabaseManagerLogEvent,
    private val inAppInteractionManager: RetenoDatabaseManagerInAppInteraction
) : RetenoDatabaseManager {

    override fun hasDataForSync(): Boolean {
        val deviceCount = deviceManager.getUnSyncedDeviceCount()
        val userCount = userManager.getUnSyncedUserCount()
        val interactionCount = interactionManager.getInteractionCount()
        val eventCount = eventsManager.getEventsCount()
        val appInboxCount = appInboxManager.getAppInboxMessagesCount()
        val recomEventsCount = recomEventsManager.getRecomEventsCount()
        val wrappedLinksCount = wrappedLinkManager.getWrappedLinksCount()
        val logEventsCount = logEventManager.getLogEventsCount()
        val inAppInteractionsCount = inAppInteractionManager.getInAppInteractionsCount()

        val isDataMissing = deviceCount == 0L &&
            userCount == 0L &&
            interactionCount == 0L &&
            eventCount == 0L &&
            appInboxCount == 0L &&
            recomEventsCount == 0L &&
            wrappedLinksCount == 0L &&
            logEventsCount == 0L &&
            inAppInteractionsCount == 0L
        return isDataMissing.not()
    }
}
