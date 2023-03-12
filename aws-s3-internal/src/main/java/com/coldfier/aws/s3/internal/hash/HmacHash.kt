package com.coldfier.aws.s3.internal.hash

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class HmacHash(private val value: String) {
    SHA_1("HmacSHA1"),
    SHA_256("HmacSHA256");

    @Throws(
        IllegalArgumentException::class,
        IllegalStateException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class
    )
    fun calculate(secretKey: ByteArray, dataToHash: ByteArray): ByteArray {
        val secret = SecretKeySpec(secretKey, value)
        val mac = Mac.getInstance(value)
        mac.init(secret)

        return mac.doFinal(dataToHash)
    }
}