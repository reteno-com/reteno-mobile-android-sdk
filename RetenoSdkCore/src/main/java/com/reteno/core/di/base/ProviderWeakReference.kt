package com.reteno.core.di.base

import com.reteno.core.util.Logger
import java.lang.ref.WeakReference

abstract class ProviderWeakReference<T> : Provider<T>() {

    private var reference = WeakReference<T>(null)

    override fun get(): T = synchronized(this) {
        var instance = reference.get()
        if (instance == null) {
            instance = create()
            reference = WeakReference(instance)
        }
        /*@formatter:off*/ Logger.i(TAG, "get(): ", javaClass.simpleName, "instance = ", instance)
        /*@formatter:on*/
        return instance!!
    }

    companion object {
        private val TAG: String = ProviderWeakReference::class.java.simpleName
    }
}