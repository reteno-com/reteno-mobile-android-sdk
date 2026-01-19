package com.reteno.push.events

internal interface NotificationEventProcessor<T : Any> : EventListener<T> {
    fun notifyListeners(data: T)
}

interface EventListener<T : Any> {
    fun addListener(listener: (T) -> Unit)
    fun removeListener(listener: (T) -> Unit)
}

abstract class SimpleNotificationEventProcessor<T : Any> : NotificationEventProcessor<T> {
    private val notificationListeners = mutableSetOf<(T) -> Unit>()

    override fun addListener(listener: (T) -> Unit) {
        notificationListeners.add(listener)
    }

    override fun removeListener(listener: (T) -> Unit) {
        notificationListeners.remove(listener)
    }

    override fun notifyListeners(data: T) {
        notificationListeners.forEach {
            it.invoke(data)
        }
    }
}