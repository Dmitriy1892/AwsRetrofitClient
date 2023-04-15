package com.coldfier.aws.retrofit.client.internal.interceptor

import com.coldfier.aws.retrofit.client.internal.AwsConstants
import com.coldfier.aws.retrofit.client.internal.AwsCredentialsStore
import com.coldfier.aws.retrofit.client.internal.date.toIso8601FullString
import com.coldfier.aws.retrofit.client.internal.date.toIso8601ShortString
import com.coldfier.aws.retrofit.client.internal.hash.Hash
import com.coldfier.aws.retrofit.client.internal.hash.HmacHash
import com.coldfier.aws.retrofit.client.internal.toHexString
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern

internal class AwsSigningV4Interceptor(
    private val credentialsStore: AwsCredentialsStore,
    private val endpointPrefix: String,
    private val awsRegion: String,
    private val awsService: String
) : AwsInterceptor(credentialsStore) {

    override fun getBodyHash(bodyBytes: ByteArray): String =
        Hash.SHA_256.calculate(bodyBytes).toHexString()

    override fun addInfoToHeaders(
        headersInternal: HeadersInternal,
        request: Request,
        bodyHash: String,
        date: Date
    ): HeadersInternal {
        val xAmzHeaders = headersInternal.xAmzHeaders.toMutableList().apply {
            add(AwsConstants.X_AMZ_CONTENT_SHA256_HEADER to bodyHash)
            add(AwsConstants.X_AMZ_DATE_HEADER to date.toIso8601FullString())
        }

        return headersInternal.copy(xAmzHeaders = xAmzHeaders)
    }

    override fun getAuthValue(request: Request, signInfo: SignInfo, date: Date): String {
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

        var pathForCanonical = if (endpointPrefix.isBlank()) url.path
            else url.path.replaceBefore(endpointPrefix, "").replace(endpointPrefix, "")

        pathForCanonical =
            if (pathForCanonical.first().toString() == "/") pathForCanonical else "/$pathForCanonical"

        val canonicalUri = pathForCanonical.substringBeforeLast("?")
        val encodedCanonicalUri = if (canonicalUri.isBlank()) "/" else urlEncode(canonicalUri, true)
        val outCanonicalUri = if (encodedCanonicalUri.startsWith("/")) encodedCanonicalUri else "/$encodedCanonicalUri"

        val canonicalQueryString = convertQueryStringToCanonical(url.query ?: "")

        val canonicalHeaders = getCanonicalHeaders(signInfo)

        val hashedPayloads = signInfo.bodyHash

        return StringBuilder()
            .appendLine(request.method)
            .appendLine(outCanonicalUri)
            .appendLine(canonicalQueryString)
            .appendLine(canonicalHeaders)
            .appendLine(signedHeaders)
            .append(hashedPayloads)
            .toString()
    }

    private fun urlEncode(value: String?, path: Boolean): String {
        return if (value == null) {
            ""
        } else try {
            val encoded: String = URLEncoder.encode(value, "UTF-8")

            val pattern = java.lang.StringBuilder()

            pattern
                .append(Pattern.quote("+"))
                .append("|")
                .append(Pattern.quote("*"))
                .append("|")
                .append(Pattern.quote("%7E"))
                .append("|")
                .append(Pattern.quote("%2F"))

            val ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString())

            val matcher: Matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded)

            val buffer = StringBuffer(encoded.length)
            while (matcher.find()) {
                var replacement = matcher.group(0)
                if ("+" == replacement) {
                    replacement = "%20"
                } else if ("*" == replacement) {
                    replacement = "%2A"
                } else if ("%7E" == replacement) {
                    replacement = "~"
                } else if (path && "%2F" == replacement) {
                    replacement = "/"
                }
                matcher.appendReplacement(buffer, replacement)
            }
            matcher.appendTail(buffer)
            buffer.toString()
        } catch (ex: UnsupportedEncodingException) {
            throw RuntimeException(ex)
        }
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

    private fun convertQueryStringToCanonical(queryString: String): String =
        if (queryString.isBlank()) queryString
        else queryString
            .split("&")
            .map { keyValue ->
                val keyValueList = keyValue.split("=")
                Pair(keyValueList[0], keyValueList.getOrNull(1) ?: "")
            }
            .map { (key, value) ->
                val encodedKey = urlEncode(key, false)
                val encodedValue = urlEncode(value, false)

                Pair(encodedKey, encodedValue)
            }
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }

    private fun getCanonicalHeaders(
        signInfo: SignInfo
    ): String {
        val canonicalHeaderBuilder = StringBuilder()

        val canonicalHeaders = mutableListOf<Pair<String, String>>().apply {
            val plainHeaders = signInfo.plainHeaders
                .filter { isNeedToSign(it.first) }

            addAll(plainHeaders)
            addAll(signInfo.canonicalHeaders)
        }

        canonicalHeaders.forEachIndexed { index, (key, value) ->
            val canonicalKey = key.replace("\\s+".toRegex(), " ").lowercase()
            val canonicalValue = value.replace("\\s+".toRegex(), " ")
            val out = "$canonicalKey:$canonicalValue"
            canonicalHeaderBuilder.append(out)

            canonicalHeaderBuilder.appendLine()
        }

        return canonicalHeaderBuilder.toString()
    }

    private fun isNeedToSign(header: String): Boolean {
        return AwsConstants.DATE_HEADER.equals(header, ignoreCase = true)
                || AwsConstants.CONTENT_MD5_HEADER.equals(header, ignoreCase = true)
                || AwsConstants.HOST_HEADER.equals(header, ignoreCase = true)
                || header.contains(AwsConstants.X_AMZ_KEY_START, ignoreCase = true)
    }

    private fun getSignedHeaders(signInfo: SignInfo): String = signInfo.plainHeaders
        .filter { isNeedToSign(it.first) }
        .joinToString(";") { it.first.lowercase() }
        .plus(";")
        .plus(
            signInfo.canonicalHeaders.joinToString(";") { it.first.lowercase() }
        )


    private fun getSignature(date: Date, stringToSign: String): String {
        val secret = String.format(AwsConstants.SECRET_VALUE_FORMAT, credentialsStore.secretKey)
        val dateString = date.toIso8601ShortString()

        val hmacHash = HmacHash.SHA_256
        val dateKey = hmacHash.calculate(secret.toByteArray(), dateString.toByteArray())
        val dateRegionKey = hmacHash.calculate(dateKey, awsRegion.toByteArray())
        val dateRegionServiceKey = hmacHash.calculate(dateRegionKey, awsService.toByteArray())
        val signingKey = hmacHash.calculate(
            dateRegionServiceKey,
            AwsConstants.AWS_V4_TERMINATOR.toByteArray()
        )

        val bytes = hmacHash.calculate(signingKey, stringToSign.toByteArray())
        return bytes.toHexString()
    }
}