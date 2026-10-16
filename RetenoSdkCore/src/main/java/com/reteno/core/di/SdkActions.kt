package com.reteno.core.di

import com.reteno.core.internal.action.event.EventActions
import com.reteno.core.internal.action.iam.InAppActions
import com.reteno.core.internal.action.inbox.InboxActions
import com.reteno.core.internal.action.init.SdkControl
import com.reteno.core.internal.action.interaction.InteractionSubmitter
import com.reteno.core.internal.action.lifecycle.LifecycleActions
import com.reteno.core.internal.action.push.PushActions
import com.reteno.core.internal.action.recommendation.RecommendationActions
import com.reteno.core.internal.action.sync.SyncActions
import com.reteno.core.internal.action.user.ContactActions

internal interface SdkQueries {
    val deviceId: String

    val defaultNotificationChannel: String

    fun canPresentMessages(): Boolean

    fun isActivityPresented(): Boolean

    fun hasDataForSync(): Boolean
}

internal interface SdkActions {
    val control: SdkControl

    val queries: SdkQueries

    val contact: ContactActions

    val events: EventActions

    val lifecycle: LifecycleActions

    val inApp: InAppActions

    val interactions: InteractionSubmitter

    val push: PushActions

    val sync: SyncActions

    val inbox: InboxActions

    val recommendation: RecommendationActions
}

internal class SdkActionsImpl(
    override val control: SdkControl,
    override val queries: SdkQueries,
    override val contact: ContactActions,
    override val events: EventActions,
    override val lifecycle: LifecycleActions,
    override val inApp: InAppActions,
    override val interactions: InteractionSubmitter,
    override val push: PushActions,
    override val sync: SyncActions,
    override val inbox: InboxActions,
    override val recommendation: RecommendationActions
) : SdkActions
