package com.reteno.di.base

import com.reteno.util.Logger

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