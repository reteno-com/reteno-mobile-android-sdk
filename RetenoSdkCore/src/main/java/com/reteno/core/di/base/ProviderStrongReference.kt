package com.reteno.core.di.base

import com.reteno.core.util.Logger

abstract class ProviderStrongReference<T> : Provider<T>() {

    private var instance: T? = null

    override fun get(): T {
        if (instance == null) {
            instance = create()
            /*@formatter:off*/ Logger.i(ProviderNewInstance.TAG, "get(): ", javaClass.simpleName, "instance = ", instance)
            /*@formatter:on*/
        }
        return instance!!
    }
}