package com.coldfier.aws.retrofit.client.internal.date

import java.text.SimpleDateFormat
import java.util.*

internal enum class DateFormat(private val pattern: String) {
    RFC_2822("EEE, d MMM yyyy HH:mm:ss 'GMT'"),
    ISO_8601_FULL("yyyyMMdd'T'HHmmss'Z'"),
    ISO_8601_SHORT("yyyyMMdd");

    fun format(date: Date): String = SimpleDateFormat(pattern, Locale.US).format(date)
}