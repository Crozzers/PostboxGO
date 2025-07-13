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

/**
 * Data class representing a postbox.
 */
@Serializable
data class Postbox(
    val id: String,
    val coords: Pair<Float, Float>,
    val monarch: Monarch,
    val dateRegistered: String,
    val name: String,
    val type: String?,
)

// stuff we get from post office API

/**
 * Dataclass representing details about a post office/box, returned by royal mail APIs
 */
@Serializable
data class PostOfficeDetails(
    /** Name of the postbox */
    val name: String,
    /** Usually the postbox type */
    val address3: String,
    /** First half of the postcode */
    val postcode: String,
    val specialCharacteristics: String,
    val specialPostboxDescription: String,
    val isPriorityPostbox: Boolean,
    val isSpecialPostbox: Boolean
)

/** Details about where a postbox is and how far away it is */
@Serializable
data class LocationDetails(
    val latitude: Float,
    val longitude: Float,
    val distance: Float
)

@Serializable
data class DetailedPostboxInfo(
    val type: String,
    val officeDetails: PostOfficeDetails,
    val locationDetails: LocationDetails
)