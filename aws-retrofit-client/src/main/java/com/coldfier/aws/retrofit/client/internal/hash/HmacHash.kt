package com.coldfier.aws.retrofit.client.internal.hash

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal enum class HmacHash(private val value: String) {
    SHA_1("HmacSHA1"),
    SHA_256("HmacSHA256");

    @Throws(
        IllegalArgumentException::class,
        IllegalStateException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class
    )
    fun calculate(secretKey: ByteArray, dataToHash: ByteArray): ByteArray =
        Mac.getInstance(value)
            .apply {
                val secret = SecretKeySpec(secretKey, value)
                init(secret)
            }
            .run {
                doFinal(dataToHash)
            }
}