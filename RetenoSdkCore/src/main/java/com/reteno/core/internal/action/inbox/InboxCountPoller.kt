package com.reteno.core.internal.action.inbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.internal.coroutine.RetenoDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal interface InboxCountPolling {
    fun subscribe(callback: RetenoResultCallback<Int>)

    fun unsubscribe(callback: RetenoResultCallback<Int>)

    fun unsubscribeAll()

    fun refresh()

    fun resume()

    fun pause()
}

internal class InboxCountPoller(
    private val getCount: GetInboxMessagesCount,
    private val scope: CoroutineScope,
    private val dispatchers: RetenoDispatchers
) : InboxCountPolling {

    private val lock = Any()
    private val subscribers = LinkedHashSet<RetenoResultCallback<Int>>()
    private var lastCount: Int? = null
    private var inForeground = false
    private var polling: Job? = null

    private val subscribersCallback = object : RetenoResultCallback<Int> {
        override fun onSuccess(result: Int) {
            publish(result)
        }

        override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
            currentSubscribers().forEach { it.onFailure(statusCode, response, throwable) }
        }
    }

    override fun subscribe(callback: RetenoResultCallback<Int>) {
        synchronized(lock) {
            if (!subscribers.add(callback)) return
            lastCount?.let { count -> scope.launch(dispatchers.main) { callback.onSuccess(count) } }
            startPollingIfNeeded()
        }
    }

    override fun unsubscribe(callback: RetenoResultCallback<Int>) {
        synchronized(lock) {
            subscribers.remove(callback)
            if (subscribers.isEmpty()) stopPolling()
        }
    }

    override fun unsubscribeAll() {
        synchronized(lock) {
            subscribers.clear()
            stopPolling()
        }
    }

    override fun refresh() {
        synchronized(lock) { if (polling == null) return }
        scope.launch { getCount(subscribersCallback) }
    }

    override fun resume() {
        synchronized(lock) {
            inForeground = true
            startPollingIfNeeded()
        }
    }

    override fun pause() {
        synchronized(lock) {
            inForeground = false
            polling?.cancel()
            polling = null
        }
    }

    private fun startPollingIfNeeded() {
        if (!inForeground || subscribers.isEmpty() || polling != null) return
        polling = scope.launch {
            while (true) {
                getCount(subscribersCallback)
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopPolling() {
        polling?.cancel()
        polling = null
        lastCount = null
    }

    private fun publish(count: Int) {
        val listeners = synchronized(lock) {
            if (subscribers.isEmpty() || count == lastCount) return
            lastCount = count
            subscribers.toList()
        }
        listeners.forEach { it.onSuccess(count) }
    }

    private fun currentSubscribers(): List<RetenoResultCallback<Int>> = synchronized(lock) { subscribers.toList() }

    companion object {
        const val POLL_INTERVAL_MILLIS = 30_000L
    }
}
