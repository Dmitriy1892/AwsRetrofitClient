package com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal

import android.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class AwsSigningV2Interceptor(
    private val credentialsStore: AwsCredentialsStore,
    private val urlAdditionalInfoPath: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        var s3Request = convertToS3Request(request)

        val response = chain.proceed(s3Request)

        val responseCode = response.code
        return if (responseCode == 403 || responseCode == 400) {
            runBlocking { credentialsStore.updateCredentials() }
            s3Request = convertToS3Request(request)

            chain.proceed(s3Request)
        } else response
    }

    data class HeadersInternal(
        val plainHeaders: List<Pair<String, String>>,
        val canonicalHeaders: List<Pair<String, String>>,
        val contentMd5Value: String,
        val accessKey: String,
        val secretKey: String
    )

    private fun convertToS3Request(request: Request): Request {

        val s3Headers = convertHeadersToS3Headers(request)

        val newRequest = request.newBuilder()
            .headers(s3Headers)
            .build()

        return newRequest
    }

    private fun convertHeadersToS3Headers(request: Request): Headers {

        val sink = FakeSink()
        request.body?.writeTo(sink)

        val headersInternal = getHeadersModelInternal(request.headers, sink.byteArray)

        val date = getDate()

        val signature = getSignature(
            request,
            date,
            headersInternal.contentMd5Value,
            headersInternal.secretKey,
            headersInternal.canonicalHeaders
        )

        val outHeadersList = mutableListOf<Pair<String, String>>().apply {
            addAll(headersInternal.plainHeaders)
            add(AwsConstants.DATE_HEADER to date)

            val authHeaderValue =
                String.format(AwsConstants.AUTH_HEADER_VALUE_FORMAT, headersInternal.accessKey, signature)
            add(AwsConstants.AUTH_HEADER_KEY to authHeaderValue)

            addAll(headersInternal.canonicalHeaders)
        }

        val newHeadersBuilder = Headers.Builder()

        outHeadersList.forEach { (key, value) ->
            newHeadersBuilder.add(key, value)
        }

        return newHeadersBuilder.build()
    }

    private fun getHeadersModelInternal(headers: Headers, body: ByteArray?): HeadersInternal {

        val canonicalHeadersUnsorted = mutableListOf<Pair<String, String>>()
        val plainHeaders = mutableListOf<Pair<String, String>>()

        val contentMd5Value = if (body == null) "" else md5Hash(body)
        plainHeaders.add(AwsConstants.CONTENT_MD5_HEADER to contentMd5Value)

        headers.forEach { (key, value) ->
            when {
                key.contains(AwsConstants.X_AMZ_KEY_START) ->
                    canonicalHeadersUnsorted.add(key.lowercase() to value.lowercase())

                else -> plainHeaders.add(key to value)
            }
        }

        val sortedCanonicalHeadersWithoutCopies =
            normalizeAmzHeadersToCanonical(canonicalHeadersUnsorted)

        return HeadersInternal(
            plainHeaders = plainHeaders,
            canonicalHeaders = sortedCanonicalHeadersWithoutCopies,
            contentMd5Value = contentMd5Value,
            accessKey = credentialsStore.accessKey,
            secretKey = credentialsStore.secretKey
        )
    }

    private fun normalizeAmzHeadersToCanonical(
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

    private fun getSignature(
        request: Request,
        date: String,
        contentMd5Value: String,
        secretKey: String,
        sortedCanonicalHeaders: List<Pair<String, String>>
    ): String {

        if (request.method == "POST") {
            val errorMessage = "POST-requests authentication not implemented, use PUT-requests"
            throw IllegalStateException(errorMessage)
        }

        val stringToSignBuilder = StringBuilder()

        val type = request.header(AwsConstants.CONTENT_TYPE_HEADER)
            ?: request.header(AwsConstants.CONTENT_TEMP_TYPE)
            ?: "binary/octet-stream"

        stringToSignBuilder
            .appendLine(request.method)
            .appendLine(contentMd5Value)
            .appendLine(type)
            .appendLine(date)


        val canonicalHeaderBuilder = StringBuilder()

        sortedCanonicalHeaders.forEachIndexed { index, (key, value) ->
            val out =
                if (index != sortedCanonicalHeaders.lastIndex) "$key:$value\n" else "$key:$value"

            canonicalHeaderBuilder.append(out)
        }

        val canonicalHeaderString = canonicalHeaderBuilder.toString()

        val resource = request.url.toUrl().path.replace(urlAdditionalInfoPath, "")

        if (canonicalHeaderString.isNotBlank())
            stringToSignBuilder.appendLine(canonicalHeaderString)

        stringToSignBuilder.append(resource)

        val stringToSign = stringToSignBuilder.toString()

        return hmacSignature(secretKey, stringToSign).replace("\n", "")
    }

    private fun getDate(): String {
        val calendar = Calendar.getInstance()
        val date = calendar.time
        date.time = date.time - calendar.timeZone.rawOffset

        val sdf = SimpleDateFormat(AwsConstants.DATE_FORMAT_PATTERN, Locale.US)
        return sdf.format(date)
    }

    private fun hmacSignature(secretKey: String, canonicalHeaders: String): String = try {
        val secret = SecretKeySpec(secretKey.toByteArray(), AwsConstants.HMAC_ALGORITHM)
        val mac = Mac.getInstance(AwsConstants.HMAC_ALGORITHM)
        mac.init(secret)
        val bytes = mac.doFinal(canonicalHeaders.toByteArray())

        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        ""
    }

    private fun md5Hash(data: ByteArray): String {
        val md5Digest = MessageDigest.getInstance(AwsConstants.MD5_ALGORITHM)
        md5Digest.update(data, 0, data.size)
        return Base64.encodeToString(md5Digest.digest(), Base64.DEFAULT)
            .substringBeforeLast("\n")
    }
}