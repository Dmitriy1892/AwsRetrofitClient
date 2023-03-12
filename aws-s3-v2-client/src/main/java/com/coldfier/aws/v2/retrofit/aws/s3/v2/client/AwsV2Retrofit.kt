package com.coldfier.aws.v2.retrofit.aws.s3.v2.client

import com.coldfier.aws.s3.core.AwsCredentials
import com.coldfier.aws.s3.internal.AwsCredentialsStore
import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal.AwsSigningV2Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit

object AwsV2Retrofit {

    /**
     * Function for creating [Retrofit] instance that working with 'aws-s3' protocol
     * Authorization form is 'Authorization: AWS access_key:hmac_headers_with_secret_key'
     * Not working with 'POST'-requests, because 'POST'-requests signing is different for s3 protocol
     *
     * @param [baseUrl] - base url of the 'aws-s3' server
     * @sample [https://s3-example-server.com]
     *
     * @param [endpointPrefix] - prefix of the url
     * @sample [https://s3-example-server.com/s3/bucket-id] - in this case [endpointPrefix] is "/s3"
     *
     * @param [converterFactory] - XML converter factory that needed for parse responses
     *
     * @param [okHttpClient] - [OkHttpClient] that needed to add into AWS [Retrofit] client.
     * Don't add a [HttpLoggingInterceptor] into [okHttpClient] - pass 'true'
     * to the [isNeedToLoggingRequests] instead.
     *
     * @param [isNeedToLoggingRequests] - pass 'true' if you need to logging your requests.
     *
     * @param [credentialsUpdater] - function that provides a way to update
     * an [AwsCredentials.accessKey] and [AwsCredentials.secretKey]
     */
    fun create(
        baseUrl: String,
        endpointPrefix: String,
        converterFactory: Converter.Factory,
        okHttpClient: OkHttpClient,
        isNeedToLoggingRequests: Boolean,
        credentials: AwsCredentials,
        credentialsUpdater: () -> AwsCredentials
    ): Retrofit {

        val okHttpClientBuilder = okHttpClient.newBuilder()

        val credentialsStore = AwsCredentialsStore(credentials, credentialsUpdater)
        val interceptor = AwsSigningV2Interceptor(credentialsStore, endpointPrefix)
        okHttpClientBuilder.addInterceptor(interceptor)

        if (isNeedToLoggingRequests) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClientBuilder.addNetworkInterceptor(loggingInterceptor)
        }

        val newOkHttpClient = okHttpClientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(converterFactory)
            .client(newOkHttpClient)
            .build()
    }
}