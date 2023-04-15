package com.coldfier.aws.retrofit.client

import com.coldfier.aws.retrofit.client.internal.AwsConstants
import com.coldfier.aws.retrofit.client.internal.AwsCredentialsStore
import com.coldfier.aws.retrofit.client.internal.interceptor.AwsInterceptor
import com.coldfier.aws.retrofit.client.internal.interceptor.AwsSigningV2Interceptor
import com.coldfier.aws.retrofit.client.internal.interceptor.AwsSigningV4Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object AwsRetrofitClientFactory {

    /**
     * Function for creating [Retrofit] instance that working with 'aws-s3' protocol
     * Authorization form is 'Authorization: AWS access_key:hmac_headers_with_secret_key'
     *
     * @param [baseUrl] - base url of the 'aws-s3' server
     * @sample [https://s3-example-server.com]
     *
     * @param [converterFactory] - XML converter factory that needed for parse responses
     *
     * @param [requestsLoggingLevel] - pass [HttpLoggingInterceptor.Level] if you need to logging
     * your requests.
     *
     * @param [okHttpClient] - [OkHttpClient] that needed to add into AWS [Retrofit] client.
     * Don't add a [HttpLoggingInterceptor] into [okHttpClient] - pass [requestsLoggingLevel] instead.
     *
     * @param [awsRegion] - AWS region
     * @sample "us-east-1" - added by default
     *
     * @param [awsService] - AWS service
     * @sample "s3" - added by default
     *
     * @param [awsSigning] - AWS signing version, [AwsSigning.V4] by default
     *
     * @param [credentials] - AWS credentials that used for requests signing calculation
     * Includes an [AwsCredentials.accessKey] and [AwsCredentials.secretKey]
     *
     * @param [credentialsUpdater] - function that provides a way to update
     * an [AwsCredentials.accessKey] and [AwsCredentials.secretKey]
     */
    fun create(
        baseUrl: String,
        converterFactory: Converter.Factory,
        requestsLoggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
        okHttpClient: OkHttpClient = getDefaultOkHttpClient(),
        awsRegion: String = AwsConstants.DEFAULT_AWS_REGION,
        awsService: String = AwsConstants.AWS_SERVICE_S3,
        awsSigning: AwsSigning = AwsSigning.V4,
        credentials: AwsCredentials,
        credentialsUpdater: () -> AwsCredentials = { credentials }
    ): Retrofit {
        val path = baseUrl.toHttpUrl().toUrl().path
        val endpointPrefix = if (path == "/") "" else path
        val credentialsStore = AwsCredentialsStore(credentials, credentialsUpdater)
        val awsSigningInterceptor = getAwsSigningInterceptor(
            awsSigning,
            credentialsStore,
            endpointPrefix,
            awsRegion,
            awsService
        )

        val loggingInterceptor = HttpLoggingInterceptor()
            .apply { level = requestsLoggingLevel }

        val newOkHttpClient = okHttpClient.newBuilder()
            .addInterceptor(awsSigningInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(converterFactory)
            .client(newOkHttpClient)
            .build()
    }

    private fun getAwsSigningInterceptor(
        awsSigning: AwsSigning,
        credentialsStore: AwsCredentialsStore,
        endpointPrefix: String,
        awsRegion: String,
        awsService: String
    ): AwsInterceptor = when (awsSigning) {
        AwsSigning.V2 -> AwsSigningV2Interceptor(credentialsStore, endpointPrefix)

        AwsSigning.V4 ->
            AwsSigningV4Interceptor(credentialsStore, endpointPrefix, awsRegion, awsService)
    }

    private fun getDefaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(true)
        .build()
}