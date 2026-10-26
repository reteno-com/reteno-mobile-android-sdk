package com.reteno.core.fixtures

internal class FakeCallLog {
    val calls = mutableListOf<String>()
    var failure: Throwable? = null

    fun record(call: String) {
        failure?.let { throw it }
        calls += call
    }
}
