package com.coldfier.aws.s3.v4.client.internal

import com.coldfier.aws.s3.core.AwsCredentials
import com.coldfier.aws.s3.core.AwsHeader
import com.coldfier.aws.s3.internal.AwsConstants
import com.coldfier.aws.s3.internal.AwsCredentialsStore
import com.coldfier.aws.s3.internal.SignInfo
import com.coldfier.aws.s3.internal.date.toIso8601FullString
import com.coldfier.aws.s3.internal.date.toIso8601ShortString
import com.coldfier.aws.s3.internal.hash.Hash
import com.coldfier.aws.s3.internal.hash.HmacHash
import com.coldfier.aws.s3.internal.request.body.bodyBytes
import com.coldfier.aws.s3.internal.toHexString
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert
import org.junit.Test
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

internal class AwsSigningV4InterceptorTest {

    val mock = AwsSigningV4Interceptor(
        AwsCredentialsStore(
            AwsCredentials(
                "AKIAIOSFODNN7EXAMPLE",
                "ƒ/K7MDENG/bPxRfiCYEXAMPLEKEY"
            )
        ) {
            AwsCredentials(
                "AKIAIOSFODNN7EXAMPLE",
                "ƒ/K7MDENG/bPxRfiCYEXAMPLEKEY"
            )
        },
        endpointPrefix = "",
        AwsConstants.DEFAULT_AWS_REGION,
        AwsConstants.AWS_SERVICE_S3
    )

    @Test
    fun sha256Test() {
        val result = sha256Hash(byteArrayOf())
        Assert.assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            result
        )
    }

    private fun sha256Hash(body: ByteArray): String {
        return Hash.SHA_256.calculate(body).toHexString()
    }

    /////////////////////////////////////////////////////////////////

    @Test
    fun testCanonicalGet() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://examplebucket.s3.amazonaws.com/test.txt")
            .headers(
                Headers.Builder()
                    .add("Range", "bytes=0-9")
                    .build()
            )
            .build()

        val canonicalRequest = getCanonicalRequestResult(request)

        Assert.assertEquals(
            "GET\n" +
                    "/test.txt\n" +
                    "\n" +
                    "host:examplebucket.s3.amazonaws.com\n" +
                    "range:bytes=0-9\n" +
                    "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                    "x-amz-date:20130524T000000Z\n" +
                    "\n" +
                    "host;range;x-amz-content-sha256;x-amz-date\n" +
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",

            canonicalRequest
        )

        val stringToSign = getStringToSign(canonicalRequest)

        Assert.assertEquals(
            "AWS4-HMAC-SHA256\n" +
                    "20130524T000000Z\n" +
                    "20130524/us-east-1/s3/aws4_request\n" +
                    "7344ae5b7ee6c3e7e6b0fe0640412a37625d1fbfff95c48bbb2dc43964946972",
            stringToSign
        )

        val signature = getSignature(getGmt0Date(), stringToSign)

        Assert.assertEquals(
            "f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41",
            signature
        )
    }

    @Test
    fun testCanonicalPut() {
        val request = Request.Builder()
            .method("PUT", "body".toRequestBody())
            .url("https://examplebucket.s3.amazonaws.com/test\$file.text")
            .headers(
                Headers.Builder()
                    .add("Date", "Fri, 24 May 2013 00:00:00 GMT")
                    .add("x-amz-storage-class", "REDUCED_REDUNDANCY")
                    .build()
            )
            .build()

        val canonicalRequest = getCanonicalRequestResult(request)

        Assert.assertEquals(
            "PUT\n" +
                    "/test%24file.text\n" +
                    "\n" +
                    "date:Fri, 24 May 2013 00:00:00 GMT\n" +
                    "host:examplebucket.s3.amazonaws.com\n" +
                    "x-amz-content-sha256:230d8358dc8e8890b4c58deeb62912ee2f20357ae92a5cc861b98e68fe31acb5\n" +
                    "x-amz-date:20130524T000000Z\n" +
                    "x-amz-storage-class:REDUCED_REDUNDANCY\n" +
                    "\n" +
                    "date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class\n" +
                    "230d8358dc8e8890b4c58deeb62912ee2f20357ae92a5cc861b98e68fe31acb5",

            canonicalRequest
        )

        val stringToSign = getStringToSign(canonicalRequest)

        Assert.assertEquals(
            "AWS4-HMAC-SHA256\n" +
                    "20130524T000000Z\n" +
                    "20130524/us-east-1/s3/aws4_request\n" +
                    "40614b5f21bb2d56c22e099ab3a8c2b5fada69f342d6185efb08b83f5685bddc",
            stringToSign
        )

        val signature = getSignature(getGmt0Date(), stringToSign)

        Assert.assertEquals(
            "d2126a8412560892481229ddfca7e79616881aa1845d2a4d2ab9de60c1a17a8e",
            signature
        )
    }

    @Test
    fun getBucketLifecycle() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://examplebucket.s3.amazonaws.com/?lifecycle")
            .headers(
                Headers.Builder()
                    .build()
            )
            .build()

        val canonicalRequest = getCanonicalRequestResult(request)

        Assert.assertEquals(
            "GET\n" +
                    "/\n" +
                    "lifecycle=\n" +
                    "host:examplebucket.s3.amazonaws.com\n" +
                    "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                    "x-amz-date:20130524T000000Z\n" +
                    "\n" +
                    "host;x-amz-content-sha256;x-amz-date\n" +
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",

            canonicalRequest
        )

        val stringToSign = getStringToSign(canonicalRequest)

        Assert.assertEquals(
            "AWS4-HMAC-SHA256\n" +
                    "20130524T000000Z\n" +
                    "20130524/us-east-1/s3/aws4_request\n" +
                    "9766c798316ff2757b517bc739a67f6213b4ab36dd5da2f94eaebf79c77395ca",
            stringToSign
        )

        val signature = getSignature(getGmt0Date(), stringToSign)

        Assert.assertEquals(
            "fea454ca298b7da1c68078a5d1bdbfbbe0d65c699e0f91ac7a200a0136783543",
            signature
        )
    }

    @Test
    fun getBucket() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://examplebucket.s3.amazonaws.com/?max-keys=2&prefix=J")
            .headers(
                Headers.Builder()
                    .build()
            )
            .build()

        val canonicalRequest = getCanonicalRequestResult(request)

        Assert.assertEquals(
            "GET\n" +
                    "/\n" +
                    "max-keys=2&prefix=J\n" +
                    "host:examplebucket.s3.amazonaws.com\n" +
                    "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                    "x-amz-date:20130524T000000Z\n" +
                    "\n" +
                    "host;x-amz-content-sha256;x-amz-date\n" +
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",

            canonicalRequest
        )

        val stringToSign = getStringToSign(canonicalRequest)

        Assert.assertEquals(
            "AWS4-HMAC-SHA256\n" +
                    "20130524T000000Z\n" +
                    "20130524/us-east-1/s3/aws4_request\n" +
                    "df57d21db20da04d7fa30298dd4488ba3a2b47ca3a489c74750e0f1e7df1b9b7",
            stringToSign
        )

        val signature = getSignature(getGmt0Date(), stringToSign)

        Assert.assertEquals(
            "34b48302e7b5fa45bde8084f4b7868a86f0a534bc59db6670ed5711ef69dc6f7",
            signature
        )
    }

    private fun getSignature(date: Date, stringToSign: String): String {
        val secret = "AWS4wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
        val dateString = getScopeDate(date)
        val dateKey = hmacSha256Hash(secret.toByteArray(), dateString)
        val dateRegionKey = hmacSha256Hash(dateKey, "us-east-1")
        val dateRegionServiceKey = hmacSha256Hash(dateRegionKey, "s3")
        val signingKey = hmacSha256Hash(dateRegionServiceKey, "aws4_request")

        val bytes = hmacSha256Hash(signingKey, stringToSign)
        return bytesToHex(bytes)
    }

    private fun hmacSha256Hash(byteArray: ByteArray, stringToSign: String): ByteArray {
        return HmacHash.SHA_256.calculate(byteArray, stringToSign.toByteArray())
    }

    private fun getStringToSign(canonicalRequest: String): String {
        val method = getStringToSignMethod()
        return method.invoke(mock, getGmt0Date(), canonicalRequest) as String
    }

    private fun getStringToSignMethod(): Method {
        val method = AwsSigningV4Interceptor::class.java.getDeclaredMethod(
            "getStringToSign",
            Date::class.java,
            String::class.java
        )
        method.isAccessible = true

        return method
    }

    private fun getCanonicalRequestResult(request: Request): String {
        val date = getGmt0Date()

        val headersInternal = convertHeaders(request, date)

        val signedHeaders = headersInternal.plainHeaders
            .filter {
                !(it.first == AwsConstants.TRUE_CONTENT_TYPE_HEADER.lowercase() && it.second.isBlank())
            }
            .joinToString(";") { it.first } + ";" +
                headersInternal.canonicalHeaders.joinToString(";") { it.first }

        val result = getCanonicalRequestMethod().invoke(mock, request, headersInternal, signedHeaders) as String
        return result
    }

    private fun getCanonicalRequestMethod(): Method {
        val method = AwsSigningV4Interceptor::class.java.getDeclaredMethod(
            "getCanonicalRequest",
            Request::class.java,
            SignInfo::class.java,
            String::class.java
        )
        method.isAccessible = true

        return method
    }

    private fun convertHeaders(request: Request, date: Date): SignInfo {
        val plainHeaders = mutableListOf<Pair<String, String>>()
        val xAmzHeaders = mutableListOf<Pair<String, String>>()

        var contentTypeHeader = Pair(AwsConstants.TRUE_CONTENT_TYPE_HEADER.lowercase(), "")

        val headers = request.headers
        headers.forEach { (key, value) ->
            when {
                key.contains(AwsHeader.CONTENT_TYPE) ->
                    contentTypeHeader = Pair(AwsConstants.TRUE_CONTENT_TYPE_HEADER.lowercase(), value)

                key.contains(AwsConstants.X_AMZ_KEY_START) ->
                    xAmzHeaders.add(key.lowercase() to value)

                else -> plainHeaders.add(key.lowercase() to value)
            }
        }

        plainHeaders.add(contentTypeHeader)
        plainHeaders.add("host" to request.url.toUrl().host)

        val shaHeader = sha256Hash(request.bodyBytes())
        xAmzHeaders.add("x-amz-content-sha256" to shaHeader)

        xAmzHeaders.add("x-amz-date" to getXamzDate(date))

        val sortedPlainHeaders = plainHeaders.sortedBy { it.first }
        val sortedCanonicalHeaders = xAmzHeaders.sortedBy { it.first }

        return SignInfo(
            plainHeaders = sortedPlainHeaders,
            canonicalHeaders = sortedCanonicalHeaders,
            bodyHash = shaHeader
        )
    }

    private fun getGmt0Date(): Date {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        return sdf.parse("20130524T000000Z")!!
    }

    private fun getXamzDate(date: Date): String {
        return date.toIso8601FullString()
    }

    private fun getScopeDate(date: Date): String {
        return date.toIso8601ShortString()
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}