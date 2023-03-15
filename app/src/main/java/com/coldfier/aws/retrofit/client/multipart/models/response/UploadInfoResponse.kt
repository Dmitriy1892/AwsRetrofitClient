package com.coldfier.aws.retrofit.client.multipart.models.response

import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml

@Xml(name = "InitiateMultipartUploadResult")
data class UploadInfoResponse @JvmOverloads constructor(

    @PropertyElement(name = "Bucket")
    val bucket: String,

    @PropertyElement(name = "Key")
    val key: String,

    @PropertyElement(name = "UploadId")
    val uploadId: String
)
