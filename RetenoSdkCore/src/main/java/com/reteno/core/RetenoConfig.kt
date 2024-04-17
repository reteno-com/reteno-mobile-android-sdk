package com.reteno.core

/**
 * @param isPausedInAppMessages - indicates paused/resumed state for in-app messages
 * @property platform - current platform name (Note that this property is mutable for multiplatform usage
 * and it should not be changed in other use cases).
 *
 * */
class RetenoConfig @JvmOverloads constructor(
    var isPausedInAppMessages: Boolean = false,
) {
    var platform: String = "Android"
}