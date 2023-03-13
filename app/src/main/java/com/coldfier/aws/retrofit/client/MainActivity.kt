package com.coldfier.aws.retrofit.client

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coldfier.aws.retrofit.client.multipart.MultipartUploadManager
import com.coldfier.aws.retrofit.client.retrofit.Injector
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

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
                val fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))

                val bucket = "test"
                val uploadId = multipartUploadManager.requestUploadInfo(bucket, fileName).uploadId


                contentResolver.openInputStream(uri).use { inputStream ->
                    checkNotNull(inputStream)

                    var rangeStart = 0
                    var rangeEnd = fileSize

                    val buffer = ByteArray(fileSize.toInt())
                    val wroteBytes = inputStream.read(buffer)

                    multipartUploadManager.uploadChunk(
                        fileSize,
                        bucket,
                        fileName,
                        1,
                        uploadId,
                    )

                    val point = 1
                }
            }
    }
}