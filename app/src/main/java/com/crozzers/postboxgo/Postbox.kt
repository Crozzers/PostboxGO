package com.crozzers.postboxgo

import com.crozzers.postboxgo.utils.humanReadablePostboxName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Monarch(val displayName: String) {
    NONE("Unmarked"),
    VICTORIA("Victoria (VR)"),
    EDWARD7("Edward 7th (E VII R)"),
    GEORGE5("George 5th (GR)"),
    EDWARD8("Edward 8th (E VIII R)"),
    GEORGE6("George 6th (G VI R)"),
    ELIZABETH2("Elizabeth 2nd (E II R)"),
    CHARLES3("Charles 3rd (C III R)");

    override fun toString(): String {
        return displayName
    }
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
    var type: String?,
    /**
     * Whether we can verify if the user has actually visited this postbox
     * in person, or just added it from map view
     */
    var verified: Boolean = true,
    /**
     * Whether the postbox is currently in service
     */
    val inactive: Boolean = false,
    /**
     * If the postbox is a double postbox, this will be the ID of the other half
     */
    val double: String? = null
) {
    override fun toString(): String {
        var name = name
        if (double != null) {
            name = name.replace(Regex("""( \([LR]\))"""), "")
        }
        return humanReadablePostboxName(name)
    }

    companion object {
        /**
         * Create a Postbox data class instance from the [DetailedPostboxInfo] returned by Royal
         * Mail APIs.
         *
         * @param pb The postbox info
         * @param monarch The monarch to mark the postbox as
         * @param verified Whether the user has physically visited the postbox
         */
        @OptIn(ExperimentalUuidApi::class)
        fun fromDetailedPostboxInfo(
            pb: DetailedPostboxInfo,
            monarch: Monarch = Monarch.NONE,
            verified: Boolean = true
        ): Postbox {
            var name: String = pb.officeDetails.name
            var doubleId: String? = null
            if (pb.double != null) {
                name = pb.officeDetails.name.replace(Regex("""( \([LR]\))"""), "")
                doubleId =
                    "${pb.double!!.officeDetails.postcode} ${pb.double!!.officeDetails.address1}"
            }
            return Postbox(
                id =
                    if (pb.type.lowercase() == "inactive") Uuid.random().toString()
                    else "${pb.officeDetails.postcode} ${pb.officeDetails.address1}",
                coords = Pair(
                    pb.locationDetails.latitude,
                    pb.locationDetails.longitude,
                ),
                monarch = monarch,
                dateRegistered = LocalDateTime.now().toString(),
                name = name,
                type = pb.officeDetails.address3,
                verified = verified,
                inactive = pb.type.lowercase() == "inactive",
                double = doubleId
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
    val locationDetails: LocationDetails,
    /**
     * This is not returned by RM APIs, but we can use this space to dump details about
     * double postboxes into
     */
    var double: DetailedPostboxInfo? = null
) {
    fun isDouble(): Boolean {
        val name = officeDetails.name.lowercase()
        val type = officeDetails.address3.lowercase()
        return (type.contains("c type") || (type.contains("type c") && !type.contains("wall box")))
                && (name.contains("(l)") || name.contains("(r)"))
    }

    override fun toString(): String {
        var name = humanReadablePostboxName(officeDetails.name)
        if (isDouble()) {
            name = name.replace(Regex("""( \([LR]\))"""), "")
        }
        return name +
                " (${officeDetails.postcode} ${officeDetails.address1})" +
                " (${locationDetails.distance} miles away)"
    }
}