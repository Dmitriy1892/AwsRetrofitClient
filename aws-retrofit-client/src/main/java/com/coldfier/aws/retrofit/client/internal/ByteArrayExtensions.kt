package com.coldfier.aws.retrofit.client.internal

import com.google.common.io.BaseEncoding

internal fun ByteArray.toHexString(): String = this.joinToString("") { "%02x".format(it) }

internal fun ByteArray.toBase64String(): String = BaseEncoding.base64().encode(this)