package com.crozzers.postboxgo.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun humanReadableDate(date: String): String {
    var parsed: LocalDateTime?
    try {
        parsed = LocalDateTime.parse(date)
    } catch (e: DateTimeParseException) {
        return date
    }
    return parsed.format(
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
    )
}

fun humanReadablePostboxName(name: String): String {
    return Regex("""\b([a-z])""").replace(name.lowercase()) { it ->
        it.value.uppercase()
    }
}