package com.coldfier.aws.s3.internal

import com.google.common.io.BaseEncoding

fun ByteArray.toHexString(): String {
    val hexString = StringBuilder(2 * this.size)
    for (i in this.indices) {
        val hex = Integer.toHexString(0xff and this[i].toInt())
        if (hex.length == 1) {
            hexString.append('0')
        }
        hexString.append(hex)
    }
    return hexString.toString()
}

fun ByteArray.toBase64String(): String = BaseEncoding.base64().encode(this)