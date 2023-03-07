package com.coldfier.aws.s3.v2.retrofit.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.AwsCredentials
import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.AwsRetrofit
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val gsonConverter = GsonConverterFactory.create(gson)

        // create Retrofit that works with AWS S3 protocol
        val awsS3Retrofit = AwsRetrofit.create(
            baseUrl = "https://s3-example.com",
            urlAdditionalInfoPath = "/v1",
            converterFactory = gsonConverter,
            okHttpClientBuilder = OkHttpClient.Builder(),
            credentialsUpdater = {
                AwsCredentials(
                    accessKey = "s9om1e2k1e3y",
                    secretKey = "a7n3o4t7h1e4r1k7e4y"
                )
            }
        )

        // create your own API interface with Retrofit methods and pass into 'create' function
        awsS3Retrofit.create(Any::class.java)
    }
}