package com.reteno.di.base

abstract class Provider<T> {

    abstract fun get(): T

    protected abstract fun create(): T
}