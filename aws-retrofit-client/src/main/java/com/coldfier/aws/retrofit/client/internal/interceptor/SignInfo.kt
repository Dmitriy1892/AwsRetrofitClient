package com.coldfier.aws.retrofit.client.internal.interceptor

internal data class SignInfo(
    val plainHeaders: List<Pair<String, String>>,
    val canonicalHeaders: List<Pair<String, String>>,
    val bodyHash: String
)