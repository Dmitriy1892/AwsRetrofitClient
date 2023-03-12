package com.coldfier.aws.s3.v4.client.internal

import com.coldfier.aws.s3.core.AwsHeader
import com.coldfier.aws.s3.internal.*
import com.coldfier.aws.s3.internal.date.getGmt0Date
import com.coldfier.aws.s3.internal.date.toIso8601FullString
import com.coldfier.aws.s3.internal.date.toIso8601ShortString
import com.coldfier.aws.s3.internal.hash.Hash
import com.coldfier.aws.s3.internal.hash.HmacHash
import com.coldfier.aws.s3.internal.request.body.bodyBytes
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern


internal class AwsSigningV4Interceptor(
    private val credentialsStore: AwsCredentialsStore,
    private val endpointPrefix: String,
    private val awsRegion: String,
    private val awsService: String
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

        val headersBuilder = Headers.Builder()

        outHeadersList.forEach { (key, value) -> headersBuilder.add(key, value) }

        return headersBuilder.build()
    }

    private fun getSignInfo(request: Request, date: Date): SignInfo {
        val plainHeaders = mutableListOf<Pair<String, String>>()
        val xAmzHeaders = mutableListOf<Pair<String, String>>()

        var contentTypeHeader = Pair(
            AwsConstants.TRUE_CONTENT_TYPE_HEADER,
            AwsConstants.DEFAULT_CONTENT_TYPE_VALUE
        )

        request.headers.forEach { (key, value) ->
            when {
                key.contains(AwsConstants.X_AMZ_KEY_START, true) ->
                    xAmzHeaders.add(key to value)

                key.contains(AwsHeader.CONTENT_TYPE, true) ->
                    contentTypeHeader = Pair(AwsConstants.TRUE_CONTENT_TYPE_HEADER, value)

                else -> plainHeaders.add(key to value)
            }
        }

        plainHeaders.add(contentTypeHeader)
        plainHeaders.add(AwsConstants.HOST_HEADER to request.url.toUrl().host)

        ////
        val bodyBytes = request.bodyBytes()
        val bodyHash = Hash.SHA_256.calculate(bodyBytes).toHexString()
        xAmzHeaders.add(AwsConstants.X_AMZ_CONTENT_SHA256_HEADER to bodyHash)

        xAmzHeaders.add(AwsConstants.X_AMZ_DATE_HEADER to date.toIso8601FullString())

        val sortedPlainHeaders = plainHeaders.sortedBy { it.first }
        val sortedCanonicalHeadersWithoutCopies = normalizeCanonicalHeaders(xAmzHeaders)

        return SignInfo(
            plainHeaders = sortedPlainHeaders,
            canonicalHeaders = sortedCanonicalHeadersWithoutCopies,
            bodyHash = bodyHash
        )
    }

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

    private fun getAuthValue(request: Request, signInfo: SignInfo, date: Date): String {
        val signedHeaders = getSignedHeaders(signInfo)

        val canonicalRequest = getCanonicalRequest(request, signInfo, signedHeaders)

        val stringToSign = getStringToSign(date, canonicalRequest)

        val signature = getSignature(date, stringToSign)

        val credential = String.format(
            AwsConstants.CREDENTIAL_VALUE_FORMAT,
            credentialsStore.accessKey,
            date.toIso8601ShortString(),
            awsRegion,
            awsService
        )

        return String.format(
            AwsConstants.AUTH_V4_HEADER_VALUE_FORMAT,
            credential,
            signedHeaders,
            signature
        )
    }

    private fun getCanonicalRequest(
        request: Request,
        signInfo: SignInfo,
        signedHeaders: String
    ): String {
        val url = request.url.toUrl()

        val canonicalUri = urlEncode(
            url.path.replace(endpointPrefix, "").substringBefore("?"),
            true
        )

        val canonicalQueryString = convertQueryStringToCanonical(url.query ?: "")

        val canonicalHeaders = getCanonicalHeaders(signInfo)

        val hashedPayloads = signInfo.bodyHash

        return StringBuilder()
            .appendLine(request.method)
            .appendLine(canonicalUri)
            .appendLine(canonicalQueryString)
            .appendLine(canonicalHeaders)
            .appendLine(signedHeaders)
            .append(hashedPayloads)
            .toString()
    }

    private fun getStringToSign(date: Date, canonicalRequest: String): String {

        val scope = String.format(
            AwsConstants.SCOPE_VALUE_FORMAT,
            date.toIso8601ShortString(),
            awsRegion,
            awsService
        )

        val requestHash = Hash.SHA_256
            .calculate(canonicalRequest.toByteArray())
            .toHexString()

        return StringBuilder()
            .appendLine(AwsConstants.AUTH_V4_ALGORITHM)
            .appendLine(date.toIso8601FullString())
            .appendLine(scope)
            .append(requestHash)
            .toString()
    }

    private fun urlEncode(value: String?, path: Boolean): String {
        return if (value == null) {
            ""
        } else try {
            val encoded = URLEncoder.encode(value, "UTF-8")
            val ENCODED_CHARACTERS_PATTERN = Pattern.compile(
                StringBuilder()
                    .append(Pattern.quote("+"))
                    .append("|")
                    .append(Pattern.quote("*"))
                    .append("|")
                    .append(Pattern.quote("%7E"))
                    .append("|")
                    .append(Pattern.quote("%2F"))
                    .toString()
            )

            val matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded)
            val buffer = StringBuffer(encoded.length)
            while (matcher.find()) {
                var replacement = matcher.group(0) ?: ""

                replacement = when {
                    replacement == "+" -> "%20"
                    replacement == "*" -> "%2A"
                    replacement == "%7E" -> "~"
                    replacement == "%2F" && path -> "/"
                    else -> replacement
                }

                matcher.appendReplacement(buffer, replacement)
            }
            matcher.appendTail(buffer)
            buffer.toString()
        } catch (ex: UnsupportedEncodingException) {
            throw RuntimeException(ex)
        }
    }

    private fun convertQueryStringToCanonical(queryString: String): String =
        if (queryString.isBlank()) queryString
        else queryString
            .split("&")
            .map { keyValue ->
                val keyValueList = keyValue.split("=")
                Pair(keyValueList[0], keyValueList.getOrNull(1) ?: "")
            }
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }

    private fun getCanonicalHeaders(
        signInfo: SignInfo
    ): String {
        val canonicalBuilder = StringBuilder()

        val canonicalHeaderBuilder = StringBuilder()

        val canonicalHeaders = mutableListOf<Pair<String, String>>().apply {
            addAll(
                signInfo.plainHeaders
                    .filter {
                        !(it.first == AwsConstants.TRUE_CONTENT_TYPE_HEADER && it.second.isBlank())
                    }
            )
            addAll(signInfo.canonicalHeaders)
        }

        canonicalHeaders.forEachIndexed { index, (key, value) ->
            val out =
                if (index != canonicalHeaders.lastIndex) "${key.lowercase()}:$value\n" else "${key.lowercase()}:$value"

            canonicalHeaderBuilder.append(out)
        }

        return canonicalBuilder
            .appendLine(canonicalHeaderBuilder.toString())
            .toString()
    }

    private fun getSignedHeaders(signInfo: SignInfo): String = signInfo.plainHeaders
        .filter { !(it.first == AwsConstants.TRUE_CONTENT_TYPE_HEADER && it.second.isBlank()) }
        .joinToString(";") { it.first.lowercase() }
        .plus(";")
        .plus(
            signInfo.canonicalHeaders
                .joinToString(";") { it.first.lowercase() }
        )


    private fun getSignature(date: Date, stringToSign: String): String {
        val secret = String.format(AwsConstants.SECRET_VALUE_FORMAT, credentialsStore.secretKey)
        val dateString = date.toIso8601ShortString()

        val hmacHash = HmacHash.SHA_256
        val dateKey = hmacHash.calculate(secret.toByteArray(), dateString.toByteArray())
        val dateRegionKey = hmacHash.calculate(dateKey, awsRegion.toByteArray())
        val dateRegionServiceKey = hmacHash.calculate(dateRegionKey, awsService.toByteArray())
        val signingKey = hmacHash.calculate(dateRegionServiceKey, AwsConstants.AWS_V4_TERMINATOR.toByteArray())

        val bytes = hmacHash.calculate(signingKey, stringToSign.toByteArray())
        return bytes.toHexString()
    }
}