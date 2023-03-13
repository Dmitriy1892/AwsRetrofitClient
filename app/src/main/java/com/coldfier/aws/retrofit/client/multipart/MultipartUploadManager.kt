package com.coldfier.aws.retrofit.client.multipart

import com.coldfier.aws.retrofit.client.multipart.models.response.UploadInfoResponse
import com.coldfier.aws.retrofit.client.retrofit.AwsS3Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

class MultipartUploadManager(
    private val awsS3Api: AwsS3Api
) {

    suspend fun requestUploadInfo(
        bucket: String,
        fileName: String
    ): UploadInfoResponse = withContext(Dispatchers.IO) {
        awsS3Api.requestMultipartUploadInfo(
            bucket = bucket,
            objectNameWithExtension = fileName
        )
    }

    suspend fun uploadChunk(
        contentLength: Long,
        bucket: String,
        objectNameWithExtension: String,
        partNumber: Int,
        uploadId: String
    ) = withContext(Dispatchers.IO) {
        awsS3Api.uploadMultipartChunk(
            contentLength,
            bucket,
            objectNameWithExtension,
            partNumber,
            uploadId
        )
    }
}