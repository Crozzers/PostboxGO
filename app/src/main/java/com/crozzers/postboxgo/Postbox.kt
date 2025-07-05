package com.crozzers.postboxgo

import kotlinx.serialization.Serializable

enum class Monarch {
    NONE,
    VICTORIA,
    EDWARD7,
    GEORGE5,
    EDWARD8,
    GEORGE6,
    ELIZABETH2,
    CHARLES3
}

@Serializable
data class Postbox(
    val coords: Pair<Float, Float>,
    val monarch: Monarch
)