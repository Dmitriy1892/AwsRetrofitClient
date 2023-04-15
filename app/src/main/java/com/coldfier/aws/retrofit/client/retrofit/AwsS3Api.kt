package com.coldfier.aws.retrofit.client.retrofit

import com.coldfier.aws.retrofit.client.AwsHeader
import com.coldfier.aws.retrofit.client.multipart.models.request.CompleteMultipartUploadRequest
import com.coldfier.aws.retrofit.client.multipart.models.response.UploadInfoResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AwsS3Api {

    @POST("{bucket}/{object}")
    suspend fun uploadFile(
        @Path("bucket") bucket: String = "2a9e8de5-30d6-4d70-ad2e-2b9b00c00cf3",
        @Path("object") obj: String = "testObj",
        @Header("Content-Type") contentType: String = "file/*",
        @Body body: RequestBody
    ): Response<Any>

    @PUT("{bucket}/{object}")
    suspend fun uploadOneShot(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "binary/octet-stream",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObj",
        @Body body: RequestBody
    ): Response<Any>

    @PUT("{bucket}/{object}?append")
    suspend fun appendUpload(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "binary/octet-stream",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObjNew",
        @Query("position") position: Long,
        @Body body: RequestBody
    )

    @GET("{bucket}")
    suspend fun getBucket(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "application/xml",
        @Header("X-Amz-Meta-Inf") a: String = "one",
        @Header("X-Amz-Meta-Inf") b: String = "two",
        @Path("bucket") bucket: String = "test",
    )

    @GET("{bucket}/{object}")
    suspend fun getObjectChunked(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "application/json",
        @Header("range") range: String = "bytes=0-7",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "testObjNew",
    ): Response<ResponseBody>

    // Step 1 for multipart upload - receiving UploadInfo with UploadId
    @POST("{bucket}/{object}?uploads")
    suspend fun requestMultipartUploadInfo(
        @Path("bucket") bucket: String,
        @Path("object") objectName: String
    ): UploadInfoResponse

    // Step 2 for multipart upload - sending
    @PUT("{bucket}/{object}")
    suspend fun uploadMultipartChunk(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "application/octet-stream",
        @Path("bucket") bucket: String,
        @Path("object", encoded = true) objectName: String,
        @Query("partNumber") partNumber: Int,
        @Query("uploadId", encoded = true) uploadId: String,
        @Body requestBody: RequestBody
    ): Response<Unit>

    // Step 3 for multipart upload - finish upload
    @POST("{bucket}/{object}")
    suspend fun completeMultipartUpload(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "text/xml",
        @Path("bucket") bucket: String,
        @Path("object", encoded = true) objectName: String,
        @Query("uploadId", encoded = true)uploadId: String,
        @Body request: CompleteMultipartUploadRequest
    )
}