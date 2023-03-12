package com.coldfier.aws.s3.v2.retrofit.client.retrofit

import com.coldfier.aws.s3.core.AwsCredentials
import com.coldfier.aws.s3.v4.client.AwsV4Retrofit
import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.AwsV2Retrofit
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Injector {

    fun provideAwsS3Api(): AwsS3Api = provideRetrofit().create(AwsS3Api::class.java)

    private fun provideRetrofit(
        okHttpClient: OkHttpClient = provideOkHttpClient(),
        baseUrl: String = "https://sample-s3-aws.com"
    ): Retrofit {
        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .setLenient()
            .create()

        val gsonConverter = GsonConverterFactory.create(gson)

//        return AwsV4Retrofit.create(
//            baseUrl = baseUrl,
//            endpointPrefix = AwsS3Api.S3_PATH,
//            converterFactory = gsonConverter,
//            okHttpClient = okHttpClient,
//            isNeedToLoggingRequests = true,
//            credentials = AwsCredentials(
//                accessKey = "STRX1YO0EOL9SLW2J1J5",
//                secretKey = "F6gXYCRvjIhcPGlLphKIhrvbPcIMi5QlW6TkguwD"
//            ),
//            credentialsUpdater = {
//                AwsCredentials(
//                    accessKey = "STRX1YO0EOL9SLW2J1J5",
//                    secretKey = "F6gXYCRvjIhcPGlLphKIhrvbPcIMi5QlW6TkguwD"
//                )
//            }
//        )

        return AwsV2Retrofit.create(
            baseUrl = baseUrl,
            endpointPrefix = AwsS3Api.S3_PATH,
            converterFactory = gsonConverter,
            okHttpClient = okHttpClient,
            isNeedToLoggingRequests = true,
            credentials = AwsCredentials(
                accessKey = "STRX1YO0EOL9SLW2J1J5",
                secretKey = "F6gXYCRvjIhcPGlLphKIhrvbPcIMi5QlW6TkguwD"
            ),
            credentialsUpdater = {
                AwsCredentials(
                    accessKey = "STRX1YO0EOL9SLW2J1J5",
                    secretKey = "F6gXYCRvjIhcPGlLphKIhrvbPcIMi5QlW6TkguwD"
                )
            }
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