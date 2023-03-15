package com.coldfier.aws.retrofit.client.multipart.models.request

import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml

@Xml(name = "CompleteMultipartUpload")
data class CompleteMultipartUploadRequest @JvmOverloads constructor(

    @Element
    val Part: List<UploadPartRequest>
)
