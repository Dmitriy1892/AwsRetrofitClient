package com.coldfier.aws.retrofit.client

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coldfier.aws.retrofit.client.multipart.MultipartUploadManager
import com.coldfier.aws.retrofit.client.multipart.models.request.CompleteMultipartUploadRequest
import com.coldfier.aws.retrofit.client.retrofit.Injector
import kotlinx.coroutines.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    private companion object {
        const val FILE_CHUNK_SIZE = 5_242_880 //5 MB - minimum size for multipart upload
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            uploadFile(uri.toString())
        }
    }

    private val externalStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private val awsS3Api = Injector.provideAwsS3Api()

    private val multipartUploadManager = MultipartUploadManager(awsS3Api)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
    }

    private fun initViews() {
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                externalStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        }
    }

    private fun uploadFile(uri: String) {
        lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            val point = 0
        }) {
            uploadFileInternal(uri)
        }
    }

    @SuppressLint("Range")
    suspend fun uploadFileInternal(path: String) = withContext(Dispatchers.IO) {
        val contentResolver = contentResolver
        val uri = Uri.parse(path)

        contentResolver.query(uri, null, null, null, null)
            .use { cursor ->
                if (cursor == null || cursor.count == 0) throw FileNotFoundException()
                cursor.moveToFirst()
                val fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    .replace("[', ]+".toRegex(), "")
                val fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))

                val bucket = "test"
                val uploadId = multipartUploadManager.requestUploadInfo(bucket, fileName).uploadId

                val etags = mutableListOf<com.coldfier.aws.retrofit.client.multipart.models.request.UploadPartRequest>()

                var partNumber = 1

                contentResolver.openInputStream(uri).use { inputStream ->
                    checkNotNull(inputStream)

                    while (inputStream.available() > 0) {
                        val chunkSize =
                            if (inputStream.available() >= FILE_CHUNK_SIZE) FILE_CHUNK_SIZE
                            else inputStream.available()

                        val buffer = ByteArray(chunkSize)
                        inputStream.read(buffer)

                        // TODO - encrypt bytes here, if need

                        buffer.inputStream()

                        val etag = multipartUploadManager.uploadChunk(
                            fileSize,
                            bucket,
                            fileName,
                            partNumber,
                            uploadId,
                            buffer.toRequestBody()
                        )
                        etags.add(com.coldfier.aws.retrofit.client.multipart.models.request.UploadPartRequest(partNumber, etag))
                        partNumber++

                        val percent = partNumber * FILE_CHUNK_SIZE * 100 / fileSize

                        withContext(Dispatchers.Main) {
                            val progressValue = if (percent.toInt() <= 100) percent.toInt() else 100

                            val progressView = findViewById<ProgressBar>(R.id.progress)

                            ObjectAnimator.ofInt(progressView, "progress", progressValue)
                                .setDuration(500)
                                .start()
                        }
                    }

                    multipartUploadManager.completeMultipartUpload(
                        bucket,
                        fileName,
                        uploadId,
                        CompleteMultipartUploadRequest(etags)
                    )
                }
            }
    }
}