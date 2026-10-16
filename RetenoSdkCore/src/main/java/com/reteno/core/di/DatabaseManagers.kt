package com.reteno.core.di

import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.manager.RetenoDatabaseManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInboxImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDeviceImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEventsImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteractionImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessagesImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteractionImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEvent
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerLogEventImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEventsImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUserImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLinksImpl

internal class DatabaseManagers(
    val devicesDb: RetenoDatabaseManagerDevice,
    val usersDb: RetenoDatabaseManagerUser,
    val interactionsDb: RetenoDatabaseManagerInteraction,
    val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction,
    val eventsDb: RetenoDatabaseManagerEvents,
    val inboxDb: RetenoDatabaseManagerAppInbox,
    val inAppMessagesDb: RetenoDatabaseManagerInAppMessages,
    val recomEventsDb: RetenoDatabaseManagerRecomEvents,
    val wrappedLinksDb: RetenoDatabaseManagerWrappedLink,
    val logEventsDb: RetenoDatabaseManagerLogEvent,
    val databaseManager: RetenoDatabaseManager
)

internal fun databaseManagers(database: RetenoDatabase): DatabaseManagers {
    val devicesDb = RetenoDatabaseManagerDeviceImpl(database)
    val usersDb = RetenoDatabaseManagerUserImpl(database)
    val interactionsDb = RetenoDatabaseManagerInteractionImpl(database)
    val inAppInteractionsDb = RetenoDatabaseManagerInAppInteractionImpl(database)
    val eventsDb = RetenoDatabaseManagerEventsImpl(database)
    val inboxDb = RetenoDatabaseManagerAppInboxImpl(database)
    val recomEventsDb = RetenoDatabaseManagerRecomEventsImpl(database)
    val wrappedLinksDb = RetenoDatabaseManagerWrappedLinksImpl(database)
    val logEventsDb = RetenoDatabaseManagerLogEventImpl(database)
    return DatabaseManagers(
        devicesDb = devicesDb,
        usersDb = usersDb,
        interactionsDb = interactionsDb,
        inAppInteractionsDb = inAppInteractionsDb,
        eventsDb = eventsDb,
        inboxDb = inboxDb,
        inAppMessagesDb = RetenoDatabaseManagerInAppMessagesImpl(database),
        recomEventsDb = recomEventsDb,
        wrappedLinksDb = wrappedLinksDb,
        logEventsDb = logEventsDb,
        databaseManager = RetenoDatabaseManagerImpl(
            devicesDb,
            usersDb,
            interactionsDb,
            eventsDb,
            inboxDb,
            recomEventsDb,
            wrappedLinksDb,
            logEventsDb,
            inAppInteractionsDb
        )
    )
}
