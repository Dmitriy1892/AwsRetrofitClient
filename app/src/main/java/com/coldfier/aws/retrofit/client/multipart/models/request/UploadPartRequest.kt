package com.coldfier.aws.retrofit.client.multipart.models.request

import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml

@Xml(name = "Part")
data class UploadPartRequest @JvmOverloads constructor(

    @PropertyElement(name = "PartNumber")
    val PartNumber: Int,

    @PropertyElement(name = "ETag")
    val ETag: String
)
