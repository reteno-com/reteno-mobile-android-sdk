package com.reteno.core.fixtures

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

internal fun stubPng(width: Int, height: Int): ByteArray {
    val header = ByteBuffer.allocate(13).putInt(width).putInt(height).put(GRAYSCALE_DEPTH).put(0).put(0).put(0).put(0)
    val png = ByteArrayOutputStream()
    png.write(SIGNATURE)
    png.writeChunk("IHDR", header.array())
    png.writeChunk("IDAT", deflate(ByteArray(height * (width + 1))))
    png.writeChunk("IEND", ByteArray(0))
    return png.toByteArray()
}

private fun ByteArrayOutputStream.writeChunk(type: String, data: ByteArray) {
    val typed = type.toByteArray() + data
    write(ByteBuffer.allocate(4).putInt(data.size).array())
    write(typed)
    write(ByteBuffer.allocate(4).putInt(CRC32().apply { update(typed) }.value.toInt()).array())
}

private fun deflate(bytes: ByteArray): ByteArray {
    val output = ByteArrayOutputStream()
    DeflaterOutputStream(output).use { it.write(bytes) }
    return output.toByteArray()
}

private const val GRAYSCALE_DEPTH: Byte = 8
private val SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
