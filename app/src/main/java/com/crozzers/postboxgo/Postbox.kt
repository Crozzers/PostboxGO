package com.crozzers.postboxgo

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

enum class Monarch(val displayName: String) {
    NONE("Unmarked"),
    VICTORIA("Victoria (VR)"),
    EDWARD7("Edward 7th (E VII R)"),
    GEORGE5("George 5th (GR)"),
    EDWARD8("Edward 8th (E VIII R)"),
    GEORGE6("George 6th (G VI R)"),
    ELIZABETH2("Elizabeth 2nd (E II R)"),
    CHARLES3("Charles 3rd (C III R)")
}

/**
 * Data class representing a postbox.
 */
@Serializable
data class Postbox(
    val id: String,
    val coords: Pair<Float, Float>,
    var monarch: Monarch,  // var because we can edit this later
    val dateRegistered: String,
    val name: String,
    val type: String?,
    /**
     * Whether we can verify if the user has actually visited this postbox
     * in person, or just added it from map view
     */
    val verified: Boolean = true
) {
    companion object {
        fun fromDetailedPostboxInfo(
            pb: DetailedPostboxInfo,
            monarch: Monarch = Monarch.NONE,
            verified: Boolean = true
        ): Postbox {
            return Postbox(
                id = "${pb.officeDetails.postcode} ${pb.officeDetails.address1}",
                coords = Pair(
                    pb.locationDetails.latitude,
                    pb.locationDetails.longitude,
                ),
                monarch = monarch,
                dateRegistered = LocalDateTime.now().toString(),
                name = pb.officeDetails.name,
                type = pb.officeDetails.address3,
                verified = verified
            )
        }
    }
}

// stuff we get from post office API

/**
 * Dataclass representing details about a post office/box, returned by royal mail APIs
 */
@Serializable
data class PostOfficeDetails(
    /** Name of the postbox */
    val name: String,
    /** Postbox number. Combine with postcode to get unique ID */
    val address1: String,
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