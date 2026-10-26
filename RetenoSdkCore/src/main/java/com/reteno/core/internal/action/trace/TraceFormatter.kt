package com.reteno.core.internal.action.trace

internal object TraceFormatter {

    const val MAX_LINE = 1_000

    private const val NAME_WIDTH = 32
    private const val DETAIL_WIDTH = 14
    private const val DURATION_WIDTH = 7
    private const val DEPTH_INDENT = "│  "

    fun start(depth: Int, id: String, name: String, args: String): String {
        val arguments = if (args.isEmpty()) "" else "  $args"
        return line(depth, "┌─ [$id] $name$arguments")
    }

    fun stepSuccess(depth: Int, id: String, name: String, detail: String, millis: Long): String =
        line(depth, "│  [$id] ✓ ${columns(name, detail, millis)}")

    fun stepFailure(depth: Int, id: String, name: String, error: String, millis: Long): String =
        line(depth, "│  [$id] ✗ ${columns(name, error, millis)}")

    fun note(depth: Int, id: String, message: String): String =
        line(depth, "│  [$id] → $message")

    fun end(depth: Int, id: String, name: String, result: String, millis: Long): String =
        line(depth, "└─ [$id] $name $result  $millis ms")

    fun collapsed(depth: Int, id: String, name: String, args: String, result: String, millis: Long): String {
        val arguments = if (args.isEmpty()) "" else "  $args"
        return line(depth, "── [$id] $name$arguments $result  $millis ms")
    }

    private fun columns(name: String, detail: String, millis: Long): String =
        name.padEnd(NAME_WIDTH) + " " + detail.padEnd(DETAIL_WIDTH) + " " + "$millis ms".padStart(DURATION_WIDTH)

    private fun line(depth: Int, body: String): String {
        val text = DEPTH_INDENT.repeat(depth) + body
        return if (text.length > MAX_LINE) text.take(MAX_LINE - 1) + "…" else text
    }
}
