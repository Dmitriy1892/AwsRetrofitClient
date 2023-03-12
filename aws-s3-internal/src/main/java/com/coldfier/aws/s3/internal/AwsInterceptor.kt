package com.coldfier.aws.s3.internal

import com.coldfier.aws.s3.core.AwsHeader
import com.coldfier.aws.s3.internal.date.getGmt0Date
import com.coldfier.aws.s3.internal.request.body.bodyBytes
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.Date

abstract class AwsInterceptor(
    private val credentialsStore: AwsCredentialsStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        var s3Request = signRequest(request)

        val response = chain.proceed(s3Request)

        val responseCode = response.code
        return if (responseCode == 403 || responseCode == 400) {
            runBlocking { credentialsStore.updateCredentials() }
            s3Request = signRequest(request)

            chain.proceed(s3Request)
        } else response
    }

    private fun signRequest(request: Request): Request {
        val awsHeaders = calculateAwsHeaders(request)

        return request.newBuilder()
            .headers(awsHeaders)
            .build()
    }

    private fun calculateAwsHeaders(request: Request): Headers {
        val date = getGmt0Date()
        val signInfo = getSignInfo(request, date)

        val authValue = getAuthValue(request, signInfo, date)

        val outHeadersList = mutableListOf<Pair<String, String>>().apply {
            addAll(signInfo.plainHeaders)
            addAll(signInfo.canonicalHeaders)
            add(AwsConstants.AUTH_HEADER_KEY to authValue)
        }

        val newHeadersBuilder = Headers.Builder()

        outHeadersList.forEach { (key, value) -> newHeadersBuilder.add(key, value) }

        return newHeadersBuilder.build()
    }

    private fun getSignInfo(request: Request, date: Date): SignInfo {
        val headersInternal = separateHeaders(request)

        val bodyBytes = request.bodyBytes()
        val bodyHash = getBodyHash(bodyBytes)

        val convertedHeaders = addInfoToHeaders(headersInternal, request, bodyHash, date)
        val plainHeaders = convertedHeaders.plainHeaders
        val xAmzHeaders = convertedHeaders.xAmzHeaders

        val sortedPlainHeaders = plainHeaders.sortedBy { it.first }
        val sortedCanonicalHeadersWithoutCopies = normalizeCanonicalHeaders(xAmzHeaders)

        return SignInfo(
            plainHeaders = sortedPlainHeaders,
            canonicalHeaders = sortedCanonicalHeadersWithoutCopies,
            bodyHash = bodyHash
        )
    }

    private fun separateHeaders(request: Request): HeadersInternal {
        val plainHeaders = mutableListOf<Pair<String, String>>()
        val xAmzHeaders = mutableListOf<Pair<String, String>>()

        var contentTypeHeader = Pair(AwsConstants.TRUE_CONTENT_TYPE_HEADER, "")

        request.headers.forEach { (key, value) ->
            when {
                key.contains(AwsConstants.X_AMZ_KEY_START, true) ->
                    xAmzHeaders.add(key to value)

                key.contains(AwsHeader.CONTENT_TYPE, true) ->
                    contentTypeHeader = Pair(AwsConstants.TRUE_CONTENT_TYPE_HEADER, value)

                else -> plainHeaders.add(key to value)
            }
        }

        if (contentTypeHeader.second.isNotBlank()) plainHeaders.add(contentTypeHeader)
        plainHeaders.add(AwsConstants.HOST_HEADER to request.url.toUrl().host)

        return HeadersInternal(plainHeaders, xAmzHeaders)
    }

    protected abstract fun addInfoToHeaders(
        headersInternal: HeadersInternal,
        request: Request,
        bodyHash: String,
        date: Date
    ): HeadersInternal

    protected abstract fun getBodyHash(bodyBytes: ByteArray): String

    protected abstract fun getAuthValue(request: Request, signInfo: SignInfo, date: Date): String

    private fun normalizeCanonicalHeaders(
        headers: List<Pair<String, String>>
    ): List<Pair<String, String>> {
        val sortedCanonicalHeaders = headers.sortedBy { (key, value) -> "$key:$value" }

        val sortedCanonicalWithoutCopies = mutableListOf<Pair<String, String>>()

        sortedCanonicalHeaders.forEachIndexed { index, pair ->
            if (index == 0) sortedCanonicalWithoutCopies.add(pair)
            else {
                val prevIndex = index - 1

                if (sortedCanonicalHeaders[prevIndex].first == pair.first) {
                    val prevHeader = sortedCanonicalWithoutCopies.first { it.first == pair.first }
                    val newPrevHeader = prevHeader.copy(
                        second = prevHeader.second + "," + pair.second
                    )

                    sortedCanonicalWithoutCopies[prevIndex] = newPrevHeader
                } else {
                    sortedCanonicalWithoutCopies.add(pair)
                }
            }
        }

        return sortedCanonicalWithoutCopies
    }

    protected data class HeadersInternal(
        val plainHeaders: List<Pair<String, String>>,
        val xAmzHeaders: List<Pair<String, String>>
    )
}