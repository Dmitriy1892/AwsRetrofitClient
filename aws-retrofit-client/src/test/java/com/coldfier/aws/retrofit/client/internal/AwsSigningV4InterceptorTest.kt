package com.coldfier.aws.retrofit.client.internal

import com.coldfier.aws.retrofit.client.AwsCredentials
import com.coldfier.aws.retrofit.client.internal.date.toIso8601ShortString
import com.coldfier.aws.retrofit.client.internal.hash.Hash
import com.coldfier.aws.retrofit.client.internal.hash.HmacHash
import com.coldfier.aws.retrofit.client.internal.interceptor.AwsInterceptor
import com.coldfier.aws.retrofit.client.internal.interceptor.AwsSigningV4Interceptor
import com.coldfier.aws.retrofit.client.internal.interceptor.SignInfo
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert
import org.junit.Test
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

        val canonicalRequest = getCanonicalRequest(request)

        Assert.assertEquals(
            "GET\n" +
                    "/test.txt\n" +
                    "\n" +
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
                    "e155673fa5bcd4b855a77a15b98fce3d10f286f93a203d6d98d2eb51f885f9b7",
            stringToSign
        )

        val signature = getSignature(getGmt0Date(), stringToSign)

        Assert.assertEquals(
            "df548e2ce037944d03f3e68682813b093763996d597cf890ca3d9037fd231eb4",
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

        val canonicalRequest = getCanonicalRequest(request)

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

        val canonicalRequest = getCanonicalRequest(request)

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

        val canonicalRequest = getCanonicalRequest(request)

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
        val secret = String.format(AwsConstants.SECRET_VALUE_FORMAT, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
        val dateString = date.toIso8601ShortString()

        val hmacHash = HmacHash.SHA_256
        val dateKey = hmacHash.calculate(secret.toByteArray(), dateString.toByteArray())
        val dateRegionKey = hmacHash.calculate(dateKey, AwsConstants.DEFAULT_AWS_REGION.toByteArray())
        val dateRegionServiceKey = hmacHash.calculate(dateRegionKey, AwsConstants.AWS_SERVICE_S3.toByteArray())
        val signingKey = hmacHash.calculate(
            dateRegionServiceKey,
            AwsConstants.AWS_V4_TERMINATOR.toByteArray()
        )

        val bytes = hmacHash.calculate(signingKey, stringToSign.toByteArray())
        return bytes.toHexString()
    }

    private fun getStringToSign(canonicalRequest: String): String {
        val method = AwsSigningV4Interceptor::class.java.getDeclaredMethod(
            "getStringToSign",
            Date::class.java,
            String::class.java
        )
        method.isAccessible = true

        return method.invoke(mock, getGmt0Date(), canonicalRequest) as String
    }

    private fun getCanonicalRequest(request: Request): String {
        val method = mock.javaClass.getDeclaredMethod(
            "getCanonicalRequest",
            Request::class.java,
            SignInfo::class.java,
            String::class.java
        )
        method.isAccessible = true

        val signInfo = getSignInfo(request, getGmt0Date())
        val signedHeaders = getSignedHeaders(signInfo)

        return method.invoke(mock, request, signInfo, signedHeaders) as String
    }

    private fun getSignedHeaders(signInfo: SignInfo): String {
        val method = mock.javaClass.getDeclaredMethod(
            "getSignedHeaders",
            SignInfo::class.java
        )

        method.isAccessible = true

        return method.invoke(mock, signInfo) as String
    }

    private fun getSignInfo(request: Request, date: Date): SignInfo {
        val method = AwsInterceptor::class.java.getDeclaredMethod(
            "getSignInfo",
            Request::class.java,
            Date::class.java
        )

        method.isAccessible = true

        return method.invoke(mock, request, date) as SignInfo
    }

    private fun getGmt0Date(): Date {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        return sdf.parse("20130524T000000Z")!!
    }
}