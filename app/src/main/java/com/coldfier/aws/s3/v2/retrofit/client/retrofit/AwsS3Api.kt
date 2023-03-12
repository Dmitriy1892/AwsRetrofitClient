package com.coldfier.aws.s3.v2.retrofit.client.retrofit

import com.coldfier.aws.s3.core.AwsHeader
import okhttp3.RequestBody
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

    @POST("$S3_PATH/{bucket}/{object}?uploads")
    suspend fun requestMultipartUpload(
        @Header(AwsHeader.CONTENT_TYPE) contentType: String = "binary/octet-stream",
        @Path("bucket") bucket: String = "test",
        @Path("object") obj: String = "newMultipartObj",
        @Body body: RequestBody
    ): Response<Any>
}