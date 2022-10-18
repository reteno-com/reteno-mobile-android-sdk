package com.reteno.core.di.base

abstract class Provider<T> {

    abstract fun get(): T

    protected abstract fun create(): T
}