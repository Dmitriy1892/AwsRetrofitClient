package com.coldfier.aws.retrofit.client.internal.interceptor

import com.coldfier.aws.retrofit.client.internal.AwsConstants
import com.coldfier.aws.retrofit.client.internal.AwsCredentialsStore
import com.coldfier.aws.retrofit.client.internal.date.toRfc2822String
import com.coldfier.aws.retrofit.client.internal.hash.Hash
import com.coldfier.aws.retrofit.client.internal.hash.HmacHash
import com.coldfier.aws.retrofit.client.internal.toBase64String
import okhttp3.Request
import java.util.Date

internal class AwsSigningV2Interceptor(
    private val credentialsStore: AwsCredentialsStore,
    private val endpointPrefix: String
) : AwsInterceptor(credentialsStore) {

    override fun addInfoToHeaders(
        headersInternal: HeadersInternal,
        request: Request,
        bodyHash: String,
        date: Date
    ): HeadersInternal {
        val plainHeaders = headersInternal.plainHeaders.toMutableList().apply {
            add(AwsConstants.CONTENT_MD5_HEADER to bodyHash)
            add(AwsConstants.DATE_HEADER to date.toRfc2822String())
        }

        return headersInternal.copy(plainHeaders = plainHeaders)
    }

    override fun getBodyHash(bodyBytes: ByteArray): String =
        Hash.MD5.calculate(bodyBytes).toBase64String()

    override fun getAuthValue(request: Request, signInfo: SignInfo, date: Date): String {
        val signature = getSignature(request, signInfo, date)

        return String.format(
            AwsConstants.AUTH_V2_HEADER_VALUE_FORMAT,
            credentialsStore.accessKey,
            signature
        )
    }

    private fun getSignature(request: Request, signInfo: SignInfo, date: Date): String {
        if (request.method == "POST") {
            val errorMessage = "POST-requests authentication not implemented, use PUT-requests"
            throw IllegalStateException(errorMessage)
        }

        val secretKey = credentialsStore.secretKey.toByteArray()
        val stringToSign = getStringToSign(request, signInfo, date).toByteArray()

        val signature = HmacHash.SHA_1.calculate(secretKey, stringToSign)
        return signature.toBase64String()
    }

    private fun getStringToSign(request: Request, signInfo: SignInfo, date: Date): String {
        val contentType = signInfo.plainHeaders
            .firstOrNull { it.first == AwsConstants.TRUE_CONTENT_TYPE_HEADER }
            ?.second
            ?: ""

        val stringToSignBuilder = StringBuilder()
            .appendLine(request.method)
            .appendLine(signInfo.bodyHash)
            .appendLine(contentType)
            .appendLine(date.toRfc2822String())

        val canonicalHeaderBuilder = StringBuilder()

        signInfo.canonicalHeaders.forEachIndexed { index, (key, value) ->
            canonicalHeaderBuilder.append("${key.lowercase()}:${value.lowercase()}")

            val isNotLastIndex = index != signInfo.canonicalHeaders.lastIndex
            if (isNotLastIndex) canonicalHeaderBuilder.appendLine()
        }

        val canonicalHeaderString = canonicalHeaderBuilder.toString()

        if (canonicalHeaderString.isNotBlank())
            stringToSignBuilder.appendLine(canonicalHeaderString)

        val path = request.url.toUrl().path
        var pathForCanonical = if (endpointPrefix.isBlank()) path
            else path.replaceBefore(endpointPrefix, "")
                .replace(endpointPrefix, "")

        pathForCanonical =
            if (pathForCanonical.first().toString() == "/") pathForCanonical else "/$pathForCanonical"
        stringToSignBuilder.append(pathForCanonical)

        return stringToSignBuilder.toString()
    }
}