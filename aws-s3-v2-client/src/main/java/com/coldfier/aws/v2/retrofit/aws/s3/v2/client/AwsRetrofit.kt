package com.coldfier.aws.v2.retrofit.aws.s3.v2.client

import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal.AwsCredentialsStore
import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal.AwsSigningV2Interceptor
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit

class AwsRetrofit {

    companion object {

        /**
         * Function for creating [Retrofit] instance that working with 'aws-s3' protocol
         * Authorization form is 'Authorization: AWS access_key:hmac_headers_with_secret_key'
         * Not working with 'POST'-requests, because 'POST'-requests signing is different for s3 protocol
         *
         * @param [baseUrl] - base url of the 'aws-s3' server
         * @sample [https://s3-example-server.com]
         *
         * @param [urlAdditionalInfoPath] - path of server that describes additional info
         * @sample [https://s3-example-server.com/s3/bucket-id] - in this case [urlAdditionalInfoPath] is [/s3]
         *
         * @param [converterFactory] - JSON/XML converter factory that needed for parse responses
         *
         * @param [okHttpClientBuilder] - [OkHttpClient.Builder] that needed to add into AWS [Retrofit] client
         *
         * @param [credentialsUpdater] - function that provides a way to update an [AwsCredentials.accessKey] and [AwsCredentials.secretKey]
         */
        fun create(
            baseUrl: String,
            urlAdditionalInfoPath: String,
            converterFactory: Converter.Factory,
            okHttpClientBuilder: OkHttpClient.Builder,
            credentialsUpdater: () -> AwsCredentials
        ): Retrofit {

            val credentialsStore = AwsCredentialsStore(credentialsUpdater)
            val interceptor = AwsSigningV2Interceptor(credentialsStore, urlAdditionalInfoPath)
            val okHttpClient = okHttpClientBuilder.addInterceptor(interceptor).build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(converterFactory)
                .client(okHttpClient)
                .build()
        }
    }
}