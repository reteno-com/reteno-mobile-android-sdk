package com.reteno.core.fixtures

import com.reteno.core.data.remote.api.HttpConnections
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

internal class RecordedRequest(
    val method: String,
    val target: String,
    val headers: Map<String, String>,
    val body: String
) {

    fun header(name: String): String? = headers[name.lowercase()]
}

internal class ScriptedResponse(
    val code: Int,
    val bytes: ByteArray = ByteArray(0),
    val headers: Map<String, String> = emptyMap(),
    val declaresLength: Boolean = true
) {

    constructor(code: Int, body: String, headers: Map<String, String> = emptyMap()) :
        this(code, body.toByteArray(), headers)
}

internal class LocalHttpServer private constructor(private val server: ServerSocket) {

    private val answers = ConcurrentLinkedQueue<ScriptedResponse>()

    val requests = CopyOnWriteArrayList<RecordedRequest>()

    val opened = CopyOnWriteArrayList<HttpURLConnection>()

    val port: Int
        get() = server.localPort

    val connections = HttpConnections { url ->
        val target = URL(url).file
        val connection = URL("http://127.0.0.1:$port$target").openConnection() as HttpURLConnection
        opened += connection
        connection
    }

    fun answer(vararg responses: ScriptedResponse) {
        answers.addAll(responses)
    }

    private fun serve() {
        while (true) {
            val socket = runCatching { server.accept() }.getOrNull() ?: return
            runCatching { exchange(socket) }
        }
    }

    private fun exchange(socket: Socket) = socket.use {
        val input = BufferedInputStream(it.getInputStream())
        val request = readRequest(input)
        requests += request
        val response = answers.poll() ?: return@use
        val body = response.bytes
        val head = buildString {
            append("HTTP/1.1 ${response.code} Scripted\r\n")
            response.headers.forEach { (name, value) -> append("$name: $value\r\n") }
            if (response.declaresLength) {
                append("Content-Length: ${body.size}\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        it.getOutputStream().apply {
            write(head.toByteArray())
            write(body)
            flush()
        }
    }

    private fun readRequest(input: InputStream): RecordedRequest {
        val requestLine = readLine(input).split(" ")
        val headers = generateSequence { readLine(input).takeIf { line -> line.isNotEmpty() } }
            .associate { line -> line.substringBefore(":").trim().lowercase() to line.substringAfter(":").trim() }
        val body = when {
            headers["transfer-encoding"] == "chunked" -> readChunked(input)
            headers["content-length"] != null -> readBytes(input, headers.getValue("content-length").toInt())
            else -> ByteArray(0)
        }
        return RecordedRequest(requestLine[0], requestLine[1], headers, String(body))
    }

    private fun readChunked(input: InputStream): ByteArray {
        val body = ByteArrayOutputStream()
        while (true) {
            val size = readLine(input).substringBefore(";").trim().toInt(16)
            body.write(readBytes(input, size))
            readLine(input)
            if (size == 0) return body.toByteArray()
        }
    }

    private fun readBytes(input: InputStream, count: Int): ByteArray {
        val bytes = ByteArray(count)
        var read = 0
        while (read < count) {
            read += input.read(bytes, read, count - read)
        }
        return bytes
    }

    private fun readLine(input: InputStream): String {
        val line = ByteArrayOutputStream()
        while (true) {
            val next = input.read()
            if (next == -1 || next == '\n'.code) return line.toString().trimEnd('\r')
            line.write(next)
        }
    }

    companion object {

        suspend fun <T> serve(block: suspend (LocalHttpServer) -> T): T {
            val server = LocalHttpServer(ServerSocket(0, 10, InetAddress.getLoopbackAddress()))
            val worker = thread { server.serve() }
            try {
                return block(server)
            } finally {
                server.server.close()
                worker.join()
            }
        }
    }
}
