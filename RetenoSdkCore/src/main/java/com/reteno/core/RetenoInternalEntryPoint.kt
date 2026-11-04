package com.reteno.core

/**
 * Gives Reteno modules access to the SDK facade. Android components of those modules can't
 * receive it through a constructor, so they read it here.
 */
@InternalRetenoApi
object RetenoInternalEntryPoint {

    @Volatile
    private var installed: RetenoInternalFacade? = null

    val facade: RetenoInternalFacade
        get() = installed ?: Reteno.instance as RetenoInternalFacade

    /**
     * Replaces the facade the modules use; `null` restores the SDK instance.
     */
    fun install(facade: RetenoInternalFacade?) {
        installed = facade
    }
}
