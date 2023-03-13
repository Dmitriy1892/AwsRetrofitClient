package com.coldfier.aws.retrofit.client.retrofit

import com.coldfier.aws.retrofit.client.AwsCredentials
import com.coldfier.aws.retrofit.client.AwsRetrofitClientFactory
import com.coldfier.aws.retrofit.client.AwsSigning
import com.coldfier.aws.retrofit.client.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

object Injector {

    fun provideAwsS3Api(): AwsS3Api = provideRetrofit().create(AwsS3Api::class.java)

    private fun provideRetrofit(
        okHttpClient: OkHttpClient = provideOkHttpClient(),
        baseUrl: String = "https://apigw-qa-tq-qdv.vebtech.dev"
    ): Retrofit {

        val xmlConverterFactory = SimpleXmlConverterFactory.create()

        return AwsRetrofitClientFactory.create(
            baseUrl = baseUrl,
            endpointPrefix = AwsS3Api.S3_PATH,
            awsSigning = AwsSigning.V4,
//            awsSigning = AwsSigning.V2,
            converterFactory = xmlConverterFactory,
            okHttpClient = okHttpClient,
            isNeedToLoggingRequests = BuildConfig.DEBUG,
            credentials = AwsCredentials(
                accessKey = "STRX1YO0EOL9SLW2J1J5",
                secretKey = "F6gXYCRvjIhcPGlLphKIhrvbPcIMi5QlW6TkguwD"
            )
        )
    }

    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .build()
    }
}