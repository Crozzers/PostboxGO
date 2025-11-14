package com.crozzers.postboxgo.utils

fun parseRomanNumeral(numeral: String): Int {
    var total = 0
    var prev: Int? = null
    for (char in numeral) {
        val number = when (char) {
            'I' -> 1
            'V' -> 5
            'X' -> 10
            else -> null
        }
        if (number == null) {
            continue
        }
        if (prev == null) {
            total += number
        } else if (prev == number) {
            // EG: II
            total += number
        } else if (prev < number) {
            // EG: IV
            total = number - total
        } else {  // prev > number
            // EG: VI
            total += number
        }
        prev = number
    }
    return total
}