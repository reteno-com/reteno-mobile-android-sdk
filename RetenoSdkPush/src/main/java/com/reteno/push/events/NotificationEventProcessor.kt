package com.reteno.push.events

import com.reteno.core.util.Procedure

internal interface NotificationEventProcessor<T : Any> : EventListener<T> {
    fun notifyListeners(data: T)
}

interface EventListener<T : Any> {
    fun addListener(listener: Procedure<T>)
    fun removeListener(listener: Procedure<T>)
}

abstract class SimpleNotificationEventProcessor<T : Any> : NotificationEventProcessor<T> {
    private val notificationListeners = mutableSetOf<Procedure<T>>()

    override fun addListener(listener: Procedure<T>) {
        notificationListeners.add(listener)
    }

    override fun removeListener(listener: Procedure<T>) {
        notificationListeners.remove(listener)
    }

    override fun notifyListeners(data: T) {
        notificationListeners.forEach {
            it.execute(data)
        }
    }
}