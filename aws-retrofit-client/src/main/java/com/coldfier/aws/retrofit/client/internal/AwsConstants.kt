package com.coldfier.aws.retrofit.client.internal

internal object AwsConstants {

    const val X_AMZ_KEY_START = "x-amz-"

    const val TRUE_CONTENT_TYPE_HEADER = "Content-Type"

    const val CONTENT_MD5_HEADER = "Content-MD5"

    const val DATE_HEADER = "Date"
    const val X_AMZ_DATE_HEADER = "X-Amz-Date"

    const val X_AMZ_CONTENT_SHA256_HEADER = "X-Amz-Content-SHA256"

    const val HOST_HEADER = "Host"

    const val AUTH_HEADER_KEY = "Authorization"
    const val AUTH_V2_HEADER_VALUE_FORMAT = "AWS %s:%s"
    const val AUTH_V4_ALGORITHM = "AWS4-HMAC-SHA256"
    const val AUTH_V4_HEADER_VALUE_FORMAT =
        "$AUTH_V4_ALGORITHM Credential=%s, SignedHeaders=%s, Signature=%s"

    const val DEFAULT_AWS_REGION = "us-east-1"
    const val AWS_SERVICE_S3 = "s3"

    const val AWS_V4_TERMINATOR = "aws4_request"

    const val CREDENTIAL_VALUE_FORMAT = "%s/%s/%s/%s/$AWS_V4_TERMINATOR"

    const val SCOPE_VALUE_FORMAT = "%s/%s/%s/$AWS_V4_TERMINATOR"

    const val SECRET_VALUE_FORMAT = "AWS4%s"
}