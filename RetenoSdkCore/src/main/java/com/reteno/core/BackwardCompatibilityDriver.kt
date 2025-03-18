@file:Suppress("FunctionName")

package com.reteno.core

import android.app.Application


@Deprecated(message = "Deprecated API, use Reteno.instance instead", replaceWith = ReplaceWith(expression = "Reteno.instance"))
fun RetenoImpl(application: Application): Reteno = Reteno.instance

@Deprecated(message = "Deprecated API, use static function Reteno.initWithConfig() instead", replaceWith = ReplaceWith(expression = "Reteno.initWith(config)"))
fun RetenoImpl(
    application: Application,
    accessKey: String,
    config: RetenoConfig = RetenoConfig()
): Reteno {
    Reteno.initWithConfig(config.copy(accessKey = accessKey))
    return Reteno.instance
}
