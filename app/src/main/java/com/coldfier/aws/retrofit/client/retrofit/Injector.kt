package com.coldfier.aws.retrofit.client.retrofit

import com.coldfier.aws.retrofit.client.AwsCredentials
import com.coldfier.aws.retrofit.client.AwsRetrofitClientFactory
import com.coldfier.aws.retrofit.client.AwsSigning
import com.coldfier.aws.retrofit.client.BuildConfig
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit


object Injector {

    fun provideAwsS3Api(): AwsS3Api = provideRetrofit().create(AwsS3Api::class.java)

    private fun provideRetrofit(
        okHttpClient: OkHttpClient = provideOkHttpClient(),
        baseUrl: String = "https://s3-sample-server.sample"
    ): Retrofit {

        val tik = TikXml.Builder()
            .writeDefaultXmlDeclaration(false)
            .build()

        val xmlConverterFactory = TikXmlConverterFactory.create(tik)

        return AwsRetrofitClientFactory.create(
            baseUrl = baseUrl,
            endpointPrefix = AwsS3Api.S3_PATH,
            awsSigning = AwsSigning.V4,
            converterFactory = xmlConverterFactory,
            okHttpClient = okHttpClient,
            isNeedToLoggingRequests = BuildConfig.DEBUG,
            credentials = AwsCredentials(
                accessKey = "sampleAccessKey",
                secretKey = "sampleSecretKey"
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