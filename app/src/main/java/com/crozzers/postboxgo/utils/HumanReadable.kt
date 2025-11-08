package com.crozzers.postboxgo.utils

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun humanReadableDate(date: LocalDateTime): String {
    return date.format(
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
    )
}

fun humanReadableDate(date: String): String {
    return try {
        humanReadableDate(LocalDateTime.parse(date))
    } catch (_: DateTimeParseException) {
        date
    }
}

fun humanReadableDate(date: Long): String {
    return humanReadableDate(LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC))
}

fun humanReadablePostboxName(name: String): String {
    return Regex("""\b([a-z])|\(([a-z0-9 ]+)\)""").replace(name.lowercase()) { it ->
        it.value.uppercase()
    }
}

fun humanReadablePostboxType(type: String): String {
    return humanReadablePostboxName(type).replace(Regex(" Postbox$"), "")
}

fun humanReadablePostboxAgeEstimate(estimate: Pair<Int, Int?>?): String {
    if (estimate == null) {
        return "Unknown"
    }
    val now = LocalDateTime.now().year
    val lower = estimate.first
    val higher = estimate.second

    if (lower == higher) {
        return "${xYears(now - lower)} old (${lower})"
    }
    if (higher == null) {
        return "Up to ${xYears(now - lower)} old (${lower} - Present)"
    }
    return "${now - higher} - ${now - lower} years old (${lower} - ${higher})"
}

/**
 * Simple function to handle pluralisation of "years".
 * EG: "0 years", "1 year", "2 years", etc
 */
private fun xYears(years: Int): String {
    return if (years == 1) "1 year" else "$years years"
}