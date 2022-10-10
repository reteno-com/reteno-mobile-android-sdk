package com.reteno.di.base

import com.reteno.util.Logger

abstract class ProviderNewInstance<T> : Provider<T>() {

    override fun get(): T {
        val instance = create()
        /*@formatter:off*/ Logger.i(TAG, "get(): ", javaClass.simpleName, "instance = ", instance)
        /*@formatter:on*/
        return instance
    }

    companion object {
        val TAG: String = ProviderNewInstance::class.java.simpleName
    }
}