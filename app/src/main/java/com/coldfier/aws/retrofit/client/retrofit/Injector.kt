package com.coldfier.aws.retrofit.client.retrofit

import com.coldfier.aws.retrofit.client.AwsCredentials
import com.coldfier.aws.retrofit.client.AwsRetrofitClientFactory
import com.coldfier.aws.retrofit.client.BuildConfig
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit

object Injector {

    fun provideAwsS3Api(): AwsS3Api = provideRetrofit().create(AwsS3Api::class.java)

    private fun provideRetrofit(baseUrl: String = "https://s3-sample-url.com/api/v1/"): Retrofit {

        val tik = TikXml.Builder()
            .writeDefaultXmlDeclaration(false)
            .build()

        val xmlConverterFactory = TikXmlConverterFactory.create(tik)

        return AwsRetrofitClientFactory.create(
            baseUrl = baseUrl,
            converterFactory = xmlConverterFactory,
            requestsLoggingLevel = if (BuildConfig.DEBUG) Level.BODY else Level.NONE,
            credentials = AwsCredentials(
                accessKey = "sampleAccessKey",
                secretKey = "sampleSecretKey"
            )
        )
    }
}