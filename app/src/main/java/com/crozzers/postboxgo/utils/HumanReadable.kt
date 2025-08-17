package com.crozzers.postboxgo.utils

import com.crozzers.postboxgo.Monarch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun humanReadableMonarch(monarch: Monarch): String {
    return when (monarch) {
        Monarch.NONE -> "Unmarked"
        Monarch.VICTORIA -> "Victoria (VR)"
        Monarch.EDWARD7 -> "Edward 7th (E VII R)"
        Monarch.GEORGE5 -> "George 5th (GR)"
        Monarch.EDWARD8 -> "Edward 8th (E VIII R)"
        Monarch.GEORGE6 -> "George 6th (G VI R)"
        Monarch.ELIZABETH2 -> "Elizabeth 2nd (E II R)"
        Monarch.CHARLES3 -> "Charles 3rd (C III R)"
    }
}

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