package com.coldfier.aws.retrofit.client.internal.body

import okio.*
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

internal class FakeSink : BufferedSink {

    var byteArray: ByteArray? = null

    override val buffer: Buffer = Buffer()

    override fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedSink {
        this.byteArray = source
        return this
    }

    override fun write(source: ByteArray): BufferedSink = write(source, 0, source.size)
    override fun buffer(): Buffer = buffer
    override fun close() = Unit
    override fun emit(): BufferedSink = this
    override fun emitCompleteSegments(): BufferedSink = this
    override fun flush() = Unit
    override fun isOpen(): Boolean = false
    override fun outputStream(): OutputStream = FileOutputStream("")
    override fun timeout(): Timeout = Timeout.NONE
    override fun write(byteString: ByteString): BufferedSink = this
    override fun write(byteString: ByteString, offset: Int, byteCount: Int): BufferedSink = this
    override fun write(source: Source, byteCount: Long): BufferedSink = this
    override fun write(source: Buffer, byteCount: Long) = Unit
    override fun write(src: ByteBuffer?): Int = 0
    override fun writeAll(source: Source): Long = 0L
    override fun writeByte(b: Int): BufferedSink = this
    override fun writeDecimalLong(v: Long): BufferedSink = this
    override fun writeHexadecimalUnsignedLong(v: Long): BufferedSink = this
    override fun writeInt(i: Int): BufferedSink = this
    override fun writeIntLe(i: Int): BufferedSink = this
    override fun writeLong(v: Long): BufferedSink = this
    override fun writeLongLe(v: Long): BufferedSink = this
    override fun writeShort(s: Int): BufferedSink = this
    override fun writeShortLe(s: Int): BufferedSink = this
    override fun writeString(string: String, charset: Charset): BufferedSink = this
    override fun writeUtf8(string: String): BufferedSink = this
    override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedSink = this
    override fun writeUtf8CodePoint(codePoint: Int): BufferedSink = this
    override fun writeString(
        string: String,
        beginIndex: Int,
        endIndex: Int,
        charset: Charset
    ): BufferedSink = this
}