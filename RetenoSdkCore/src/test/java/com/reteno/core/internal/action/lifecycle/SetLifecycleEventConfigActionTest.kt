package com.reteno.core.internal.action.lifecycle

import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SetLifecycleEventConfigActionTest {

    private class SutFixture(scope: TestScope) {
        val env = LifecycleActionFixture(scope, LifecycleTrackingOptions.ALL)
        val sut = SetLifecycleEventConfigAction(env.filter, env.traced.runner)
    }

    @Test
    fun `should apply new options to shared filter`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(LifecycleTrackingOptions.NONE)

        then()
        assertEquals(false, LifecycleEventType.values().any { fixture.env.filter.isEnabled(it) })
        assertTrue(fixture.env.traced.sink.lines.any { it.contains("✓ apply lifecycle tracking options") })
    }
}
