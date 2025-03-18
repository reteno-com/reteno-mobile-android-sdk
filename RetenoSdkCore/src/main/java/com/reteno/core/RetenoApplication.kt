package com.reteno.core

@Deprecated("Interface can be removed. Access Reteno instance via Reteno.instance")
interface RetenoApplication {

    fun getRetenoInstance(): Reteno
}