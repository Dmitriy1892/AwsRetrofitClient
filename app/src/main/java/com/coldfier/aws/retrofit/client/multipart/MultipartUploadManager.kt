package com.coldfier.aws.retrofit.client.multipart

import com.coldfier.aws.retrofit.client.multipart.models.request.CompleteMultipartUploadRequest
import com.coldfier.aws.retrofit.client.multipart.models.response.UploadInfoResponse
import com.coldfier.aws.retrofit.client.retrofit.AwsS3Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody

class MultipartUploadManager(
    private val awsS3Api: AwsS3Api
) {

    suspend fun requestUploadInfo(
        bucket: String,
        objectNameWithExtension: String
    ): UploadInfoResponse = withContext(Dispatchers.IO) {
        awsS3Api.requestMultipartUploadInfo(
            bucket = bucket,
            objectName = objectNameWithExtension
        )
    }

    suspend fun uploadChunk(
        bucket: String,
        objectNameWithExtension: String,
        partNumber: Int,
        uploadId: String,
        body: RequestBody
    ): String = withContext(Dispatchers.IO) {
        awsS3Api.uploadMultipartChunk(
            bucket = bucket,
            objectName = objectNameWithExtension,
            partNumber = partNumber,
            uploadId = uploadId,
            requestBody = body
        ).headers().get("etag") ?: throw IllegalStateException("No info about uploaded part")
    }

    suspend fun completeMultipartUpload(
        bucket: String,
        objectNameWithExtension: String,
        uploadId: String,
        request: CompleteMultipartUploadRequest
    ) = withContext(Dispatchers.IO) {
        awsS3Api.completeMultipartUpload(
            bucket = bucket,
            objectName = objectNameWithExtension,
            uploadId = uploadId,
            request = request
        )
    }
}