package com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal

import com.coldfier.aws.s3.internal.*
import com.coldfier.aws.s3.internal.date.toRfc2822String
import com.coldfier.aws.s3.internal.hash.Hash
import com.coldfier.aws.s3.internal.hash.HmacHash
import okhttp3.Request
import java.util.*

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
        val signature = getSignature(
            request,
            signInfo,
            date
        )

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

        val stringToSign = getStringToSign(request, signInfo, date)

        val signature = HmacHash.SHA_1.calculate(
            credentialsStore.secretKey.toByteArray(),
            stringToSign.toByteArray()
        )
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

        val resource = request.url.toUrl().path.replace(endpointPrefix, "")
        stringToSignBuilder.append(resource)

        return stringToSignBuilder.toString()
    }
}