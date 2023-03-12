package com.coldfier.aws.s3.internal

data class SignInfo(
    val plainHeaders: List<Pair<String, String>>,
    val canonicalHeaders: List<Pair<String, String>>,
    val bodyHash: String
)