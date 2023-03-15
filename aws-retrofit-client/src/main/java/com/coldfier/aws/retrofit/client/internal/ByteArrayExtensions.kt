package com.coldfier.aws.retrofit.client.internal

import com.google.common.io.BaseEncoding

internal fun ByteArray.toHexString(): String {
    val hexString = StringBuilder(2 * this.size)
    for (i in this.indices) {
        var hex = Integer.toHexString(this[i].toInt())
        if (hex.length == 1) {
            hexString.append("0")
        } else if (hex.length == 8) {
            hex = hex.substring(6)
        }
        hexString.append(hex)
    }
    return hexString.toString()
}

internal fun ByteArray.toBase64String(): String = BaseEncoding.base64().encode(this)