package com.coldfier.aws.s3.internal.hash

import java.security.DigestException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

enum class Hash(private val value: String) {
    MD5("MD5"),
    SHA_256("SHA-256");

    @Throws(
        NoSuchAlgorithmException::class,
        DigestException::class
    )
    fun calculate(body: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance(value)
        return messageDigest.digest(body)
    }
}