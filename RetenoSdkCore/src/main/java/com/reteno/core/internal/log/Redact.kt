package com.reteno.core.internal.log

import java.util.Collections
import java.util.IdentityHashMap

internal object Redact {

    private const val NONE = "<none>"
    private const val HIDDEN = "<hidden>"
    private const val VISIBLE_TAIL = 4

    private val urlPattern = Regex("""[a-zA-Z][a-zA-Z0-9+.-]*://[^\s"'<>]+""")
    private val quotedHostPattern = Regex(""""[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+"""")

    fun token(value: String?): String = when {
        value.isNullOrEmpty() -> NONE
        value.length <= VISIBLE_TAIL -> HIDDEN
        else -> "…" + value.takeLast(VISIBLE_TAIL)
    }

    fun email(value: String?): String {
        if (value.isNullOrEmpty()) return NONE
        val at = value.indexOf('@')
        if (at <= 0) return HIDDEN
        val lastDot = value.lastIndexOf('.')
        val topLevelDomain = if (lastDot > at) value.substring(lastDot) else ""
        return "${value.first()}***@***$topLevelDomain"
    }

    fun phone(value: String?): String {
        if (value.isNullOrEmpty()) return NONE
        val digits = value.filter { it.isDigit() }
        return if (digits.length <= VISIBLE_TAIL) "***" else "***" + digits.takeLast(VISIBLE_TAIL)
    }

    fun fields(vararg fields: Pair<String, Any?>): String =
        fields.filter { it.second != null }.joinToString(prefix = "{", postfix = "}") { it.first }

    fun text(value: String): String = value
        .replace(urlPattern, "<url>")
        .replace(quotedHostPattern, "\"<host>\"")

    fun summary(error: Throwable): String = describe(error.javaClass.simpleName, error)

    fun stackTrace(error: Throwable): String {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val builder = StringBuilder()
        var current: Throwable? = error
        while (current != null && visited.add(current)) {
            if (current !== error) builder.append("Caused by: ")
            builder.append(describe(current.javaClass.name, current)).append('\n')
            current.stackTrace.forEach { builder.append("\tat ").append(it).append('\n') }
            current = current.cause
        }
        return builder.toString().trimEnd()
    }

    private fun describe(className: String, error: Throwable): String {
        val message = error.message ?: return className
        return "$className: ${text(message)}"
    }
}
