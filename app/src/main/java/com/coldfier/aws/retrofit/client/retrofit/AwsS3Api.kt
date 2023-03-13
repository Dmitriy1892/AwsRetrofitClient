package com.coldfier.aws.retrofit.client.retrofit

import com.coldfier.aws.retrofit.client.AwsHeader
import com.coldfier.aws.retrofit.client.multipart.models.response.UploadInfoResponse
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AwsS3Api {

    companion object {
        const val S3_PATH = "/s3"
    }

    @POST("$S3_PATH/{bucket}/{object}")
    suspend fun uploadFile(
        @Path("bucket") bucket: String = "2a9e8de5-30d6-4d70-ad2e-2b9b00c00cf3",
        @Path("object") obj: String = "testObj",
        @Header("Content-Type") contentType: String = "file/*",
        @Body body: RequestBody
    ): Response<Any>

    @PUT("$S3_PATH/{bucket}/{object}")
    suspend fun uploadOneShot(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "binary/octet-stream",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObj",
        @Body body: RequestBody
    ): Response<Any>

    @PUT("$S3_PATH/{bucket}/{object}?append")
    suspend fun appendUpload(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "binary/octet-stream",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObjNew",
        @Query("position") position: Long,
        @Body body: RequestBody
    )

    @GET("$S3_PATH/{bucket}")
    suspend fun getBucket(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "application/xml",
        @Header("X-Amz-Meta-Inf") a: String = "one",
        @Header("X-Amz-Meta-Inf") b: String = "two",
        @Path("bucket") bucket: String = "test",
    )

    @GET("$S3_PATH/{bucket}/{object}")
    suspend fun getObjectChunked(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "application/json",
        @Header("range") range: String = "bytes=0-7",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObjNew",
    ): Response<ResponseBody>

    // Step 1 for multipart upload - receiving UploadInfo with UploadId
    @POST("$S3_PATH/{bucket}/{object}?uploads")
    suspend fun requestMultipartUploadInfo(
        @Path("bucket") bucket: String,
        @Path("object") objectNameWithExtension: String,
        @Body body: RequestBody = byteArrayOf().toRequestBody()
    ): UploadInfoResponse

    @PUT("$S3_PATH/{bucket}/{object}")
    suspend fun uploadMultipartChunk(
        @Header("Content-Length") contentLength: Long,
        @Path("bucket") bucket: String,
        @Path("object") objectNameWithExtension: String,
        @Query("partNumber") partNumber: Int,
        @Query("uploadId") uploadId: String
    )
}