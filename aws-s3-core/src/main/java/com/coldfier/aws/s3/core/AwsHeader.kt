package com.coldfier.aws.s3.core

object AwsHeader {

    /**
     * [CONTENT_TYPE] constant is needed for custom "Content-Type" request headers because
     * 'OkHttp' 'Interceptor's not catch the "Content-Type" header. This constant is temporary fix
     * that case (appears on 'Retrofit' version '2.9.0' and 'OkHttp' version '4.10.0').
     *
     * Usage - just add [CONTENT_TYPE] as header in your request instead of "Content-Type" header
     */
    const val CONTENT_TYPE = "Content-Temp-Type"
}