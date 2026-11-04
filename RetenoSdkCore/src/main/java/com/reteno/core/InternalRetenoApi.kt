package com.reteno.core

/**
 * Marks API that exists only for communication between Reteno SDK modules (Push, FCM, HMS).
 * It is not part of the public SDK contract and can change or disappear in any release.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Internal Reteno SDK API used by Reteno modules only. It can change without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalRetenoApi
