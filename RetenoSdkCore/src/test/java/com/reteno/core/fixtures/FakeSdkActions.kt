package com.reteno.core.fixtures

import com.reteno.core.di.SdkActions

internal class FakeSdkActions(val log: FakeCallLog = FakeCallLog()) : SdkActions {
    override val control = FakeSdkControl(log)
    override val queries = FakeSdkQueries()
    override val contact = FakeContactActions(log)
    override val events = FakeEventActions(log)
    override val lifecycle = FakeLifecycleActions(log)
    override val inApp = FakeInAppActions(log)
    override val interactions = FakeInteractionSubmitter(log)
    override val push = FakePushActions(log)
    override val sync = FakeSyncActions(log)
    override val inbox = FakeInboxActions(log)
    override val recommendation = FakeRecommendationActions(log)
}
