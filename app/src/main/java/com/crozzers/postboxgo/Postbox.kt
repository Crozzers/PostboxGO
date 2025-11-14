package com.crozzers.postboxgo

import com.crozzers.postboxgo.ui.components.getIconFromPostboxType
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Monarch(val displayName: String, val icon: Int?) {
    NONE("Unmarked", null),
    VICTORIA("Victoria (VR)", R.drawable.cypher_victoria),
    EDWARD7("Edward 7th (E VII R)", R.drawable.cypher_edward7),
    GEORGE5("George 5th (GR)", R.drawable.cypher_george5),
    EDWARD8("Edward 8th (E VIII R)", R.drawable.cypher_edward8),
    GEORGE6("George 6th (G VI R)", R.drawable.cypher_george6),
    ELIZABETH2("Elizabeth 2nd (E II R)", R.drawable.cypher_elizabeth2),
    SCOTTISH_CROWN("Crown Of Scotland (No Royal Cypher)", R.drawable.cypher_crown_of_scotland),
    CHARLES3("Charles 3rd (C III R)", R.drawable.cypher_charles3);

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
    /**
     * Estimates an age for the postbox based on known information about the monarch and type
     *
     * @return null if unknown, or a pair of numbers indicating the earliest possible install date and the
     *   latest possible install date. The latter can be null if that monarch/type is still being installed today
     */
    fun getAgeEstimate(): Pair<Int, Int?>? {
        val icon = getIconFromPostboxType(type)
        // todo: include details from https://www.postalmuseum.org/blog/evolution-of-the-post-box/

        // cross off any postbox types with an easily defined lifespan
        // these have a set start and end that fit neatly within a monarch's lifespan
        if (icon == R.drawable.k_type_pillar) {
            // k type pillar introduced in 1980 and withdrawn after 2001
            return Pair(1980, 2001)
        }

        // figure out year postbox was introduced based on type
        val typeLowerBound = when (icon) {
            // double wide introduced in 1899: https://lbsg.org/photographs/
            R.drawable.type_c -> 1899
            // bantam introduced in 1999
            R.drawable.bantam_n_type -> 1999
            // lamp box design introduced in 1896: https://en.wikipedia.org/wiki/Lamp_box
            R.drawable.lamp_post, R.drawable.m_type -> 1896
            // wall boxes introduced in 1857 (scroll through the A-Z in https://lbsg.org/about-boxes/)
            R.drawable.wall_box_b_type, R.drawable.wall_box_c_type -> 1857
            // https://www.newarkadvertiser.co.uk/news/new-parcel-postboxes-unveiled-9079527/
            R.drawable.parcel -> 2019
            else -> null
        }

        val monarchBounds = when (monarch) {
            // first standardised pillar boxes bearing Victoria's cipher appear to have started in 1866
            // https://www.londonshoes.blog/2019/01/22/londons-historically-significant-pillar-post-boxes/
            Monarch.VICTORIA -> Pair(1866, 1901)
            Monarch.EDWARD7 -> Pair(1901, 1910)
            Monarch.GEORGE5 -> Pair(1910, 1936)
            Monarch.EDWARD8 -> Pair(1936, 1936)
            Monarch.GEORGE6 -> Pair(1936, 1952)
            Monarch.ELIZABETH2 -> Pair(1952, 2024)
            // https://en.wikipedia.org/wiki/Pillar_Box_War
            Monarch.SCOTTISH_CROWN -> Pair(1954, null)
            // Elizabeth 2nd died in 2022 but the first Charles 3rd postbox was only unveiled in July 2024
            // https://www.bbc.co.uk/news/articles/cjr4wxd277qo
            Monarch.CHARLES3 -> Pair(2024, null)
            Monarch.NONE -> null
        }

        if (typeLowerBound == null) {
            return monarchBounds
        } else if (monarchBounds == null) {
            return Pair(typeLowerBound, null)
        }
        // now we monarch bounds are defined AND we have a type lower bound
        if (monarchBounds.second != null && typeLowerBound > monarchBounds.second!!) {
            // if the type bound falls outside the monarch's reign then something's wrong
            // either I've got the type bounds wrong or the user's mis-identified the monarch
            // default to monarch bounds because that's the number a user would be expecting
            return monarchBounds
        }
        return Pair(max(typeLowerBound, monarchBounds.first), monarchBounds.second)
    }

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