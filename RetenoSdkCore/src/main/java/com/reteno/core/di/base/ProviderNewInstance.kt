package com.reteno.core.di.base

import com.reteno.core.util.Logger

abstract class ProviderNewInstance<T> : Provider<T>() {

    override fun get(): T {
        val instance = create()
        /*@formatter:off*/ Logger.i(TAG, "get(): ", javaClass.simpleName, "instance = ", instance)
        /*@formatter:on*/
        return instance
    }

    companion object {
        private val TAG: String = ProviderNewInstance::class.java.simpleName
    }
}