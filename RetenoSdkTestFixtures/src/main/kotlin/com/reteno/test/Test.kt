package com.reteno.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class TestContext(
    var givenCalled: Boolean = false,
    var whenCalled: Boolean = false,
    var thenCalled: Boolean = false
)

class TestHolder : AbstractCoroutineContextElement(Key) {
    private val context = TestContext()
    private val workScopes = mutableListOf<CoroutineScope>()

    fun givenCalled() {
        context.givenCalled = true
    }

    fun whenCalled() {
        context.whenCalled = true
    }

    fun thenCalled() {
        context.thenCalled = true
    }

    fun assertFormat() {
        check(context.givenCalled) { "Given step is missing" }
        check(context.whenCalled) { "When step is missing" }
        check(context.thenCalled) { "Then step is missing" }
    }

    internal fun register(scope: CoroutineScope) {
        workScopes.add(scope)
    }

    internal fun cancelWorkScopes() {
        workScopes.forEach { it.cancel() }
        workScopes.clear()
    }

    companion object Key : CoroutineContext.Key<TestHolder>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun runUnitTest(context: CoroutineContext = TestHolder(), body: suspend TestScope.() -> Unit) =
    runTest(context) {
        val holder = requireNotNull(currentCoroutineContext()[TestHolder.Key])
        try {
            body()
        } finally {
            holder.cancelWorkScopes()
        }
        holder.assertFormat()
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.workScope(): CoroutineScope {
    val holder = requireNotNull(coroutineContext[TestHolder.Key])
    val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
    holder.register(scope)
    return scope
}

suspend fun given() {
    requireNotNull(currentCoroutineContext()[TestHolder.Key]).givenCalled()
}

suspend fun whenn() {
    requireNotNull(currentCoroutineContext()[TestHolder.Key]).whenCalled()
}

suspend fun then() {
    requireNotNull(currentCoroutineContext()[TestHolder.Key]).thenCalled()
}
