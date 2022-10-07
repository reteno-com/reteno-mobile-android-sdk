package com.reteno.di.base

import com.reteno.util.Logger
import java.lang.ref.WeakReference

abstract class ProviderWeakReference<T> : Provider<T>() {

    private var reference = WeakReference<T>(null)

    override fun get(): T {
        var instance = reference.get()
        if (instance == null) {
            instance = create()
            reference = WeakReference(instance)
        }
        /*@formatter:off*/ Logger.i(ProviderNewInstance.TAG, "get(): ", javaClass.simpleName, "instance = ", instance)
        /*@formatter:on*/
        return instance!!
    }
}