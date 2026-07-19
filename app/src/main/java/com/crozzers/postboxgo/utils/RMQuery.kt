/**
 * Simple util for constructing the query URL and headers for the Royal Mail API.
 *
 * Designed to be imported for easy testing
 */
package com.crozzers.postboxgo.utils

import java.net.URL

fun constructRMQuery(postcode: String, lat: Double, lon: Double): Pair<URL, Map<String, String>> {
    // I did ask Royal Mail if I could use their official API and they said:
    // "...you need to be sending 150 items per day to qualify for API..."
    // I am not sending anything, I just want access to the postbox data :(
    val url = URL(
        "https://www.royalmail.com/capi/rml/bf/v1/locations/branchFinder" +
                "?postCode=${postcode.replace(" ", "%20")}" +
                // for some reason setting the searchRadius at 40 yields more postboxes
                // even when they aren't outside that radius
                "&searchRadius=40&count=7" +
                "&officeType=postboxes&type=2&appliedFilters=null" +
                "&latitude=${"%.6f".format(lat)}" +
                "&longitude=${"%.6f".format(lon)}"
    )

    val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:145.0) Gecko/20100101 Firefox/145.0",
        "Accept" to "*/*",
        "Referer" to "https://www.royalmail.com/services-near-you",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-origin"
    )

    return Pair(url, headers)
}
