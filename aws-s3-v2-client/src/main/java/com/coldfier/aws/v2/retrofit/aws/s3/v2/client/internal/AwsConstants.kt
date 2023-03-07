package com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal

object AwsConstants {

    internal const val X_AMZ_KEY_START = "x-amz-"

    internal const val DATE_HEADER = "Date"
    internal const val AUTH_HEADER_KEY = "Authorization"
    internal const val AUTH_HEADER_VALUE_FORMAT = "AWS %s:%s"

    internal const val DATE_FORMAT_PATTERN = "EEE, d MMM yyyy HH:mm:ss 'GMT'"

    internal const val CONTENT_MD5_HEADER = "Content-MD5"

    /**
     * [CONTENT_TEMP_TYPE] constant is needed for custom "Content-Type" request headers because
     * 'OkHttp' 'Interceptor's not catch the "Content-Type" header. This constant is temporary fix
     * that case (appears on 'Retrofit' version '2.9.0' and 'OkHttp' version '4.10.0').
     *
     * Usage - just add this as header in your request with [CONTENT_TYPE_HEADER] -
     * if [AwsSigningV2Interceptor] not find [CONTENT_TYPE_HEADER],
     * it gets a value from [CONTENT_TEMP_TYPE] header
     */
    const val CONTENT_TEMP_TYPE = "Content-Temp-Type"
    const val CONTENT_TYPE_HEADER = "Content-Type"

    internal const val MD5_ALGORITHM = "MD5"
    internal const val HMAC_ALGORITHM = "HmacSHA1"
}