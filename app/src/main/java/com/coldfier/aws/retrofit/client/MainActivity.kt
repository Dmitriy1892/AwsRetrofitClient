package com.coldfier.aws.retrofit.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coldfier.aws.retrofit.client.retrofit.Injector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {
    private val awsS3Api = Injector.provideAwsS3Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            val point = 0
        }) {
            val initial = byteArrayOf(0,1,8,7,9,1,3)

            val replacement = byteArrayOf(9,9,9,9,9,9)
            uploadFile(replacement)
        }
    }

    private suspend fun uploadFile(bytes: ByteArray) {
        val body = bytes.toRequestBody()

        awsS3Api.getBucket()

//        awsS3Api.uploadOneShot(
//            body = body
//        )

//        awsS3Api.appendUpload(
//            contentMd5 = md5Hash(bytes),
//            body = body,
//            position = 4
//        )

//        val result = awsS3Api.getObjectChunked(
//            contentMd5 = ""
//        )

//        val bytes = result.body()?.bytes()

//        val responseBody = result.body()

        val breaker = 0

//        awsS3Api.requestMultipartUpload(
//            body = body
//        )
    }
}