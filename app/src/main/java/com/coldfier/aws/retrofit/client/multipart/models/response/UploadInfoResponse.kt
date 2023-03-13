package com.coldfier.aws.retrofit.client.multipart.models.response

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "InitiateMultipartUploadResult", strict = false)
data class UploadInfoResponse(

    @Element(name = "Bucket")
    val bucket: String,

    @Element(name = "Key")
    val key: String,

    @Element(name = "UploadId")
    val uploadId: String
)
