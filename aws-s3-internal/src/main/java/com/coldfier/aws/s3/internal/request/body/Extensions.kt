package com.coldfier.aws.s3.internal.request.body

import okhttp3.Request

fun Request.bodyBytes(): ByteArray {
    val sink = FakeSink()
    this.body?.writeTo(sink)
    return sink.byteArray ?: byteArrayOf()
}