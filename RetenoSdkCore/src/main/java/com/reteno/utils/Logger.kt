package com.reteno.utils

import timber.log.Timber

object Logger {

    @JvmStatic
    fun v(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Timber.v(message)
    }

    @JvmStatic
    fun d(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Timber.d(message)
    }

    @JvmStatic
    fun i(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Timber.i(message)
    }

    @JvmStatic
    fun w(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Timber.w(message)
    }

    @JvmStatic
    fun e(methodName: String, vararg arguments: Any?) {
        val message = buildMessage(methodName, arguments)
        Timber.e(message)
    }





    private fun buildMessage(methodName: String, arguments: Array<out Any?>): String {
        val builder = StringBuilder().append(methodName)
        for (arg in arguments) {
            builder.append(arg)
        }
        return builder.toString()
    }
}