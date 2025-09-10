package com.crozzers.postboxgo.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val LOG_TAG = "NearbyPostboxes"

private val JsonParser: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}

private val cacheFileMutex = Mutex()

fun getNearbyPostboxes(
    context: Context,
    location: LatLng,
    callback: (p: List<DetailedPostboxInfo>) -> Unit
) {
    var postcode: String?
    try {
        postcode = posToUKPostcode(context, location)
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, "Failed to determine postcode", Toast.LENGTH_SHORT).show()
        return
    }
    if (postcode == null) {
        Log.e(LOG_TAG, "Failed to determine postcode")
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        var postboxData: MutableList<DetailedPostboxInfo>? =
            getPostboxesFromCache(context, postcode)?.toMutableList()
        if (postboxData != null) {
            callback(postboxData)
            return@launch
        }

        // I did ask Royal Mail if I could use their official API and they said:
        // "...you need to be sending 150 items per day to qualify for API..."
        // I am not sending anything, I just want access to the postbox data :(
        val url = URL(
            "https://www.royalmail.com/capi/rml/bf/v1/locations/branchFinder" +
                    // for some reason setting the searchRadius at 40 yields more postboxes
                    // even when they aren't outside that radius
                    "?postCode=${postcode}&searchRadius=40&count=10" +
                    "&officeType=postboxes&type=2&appliedFilters=null"
        )

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0"
            )
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Referer", "https://www.royalmail.com/services-near-you")

            connect()

            if (responseCode != 200) {
                Log.e(
                    LOG_TAG,
                    "Failed to fetch nearby postboxes for postcode $postcode: $responseMessage"
                )
                return@launch
            }
            val doubles = mutableListOf<DetailedPostboxInfo>()
            postboxData = JsonParser.decodeFromString<List<DetailedPostboxInfo>>(
                inputStream.bufferedReader().readText()
            ).filter { pb ->
                if (pb.type != "PB") {
                    return@filter false
                }
                // if it's a single then add it straight away
                if (!pb.isDouble()) {
                    return@filter true
                }
                // for doubles we need to find the other half
                // nested iteration but this list is going to be 50 items max
                for (double in doubles) {
                    val locationResult = FloatArray(1)
                    Location.distanceBetween(
                        pb.locationDetails.latitude.toDouble(),
                        pb.locationDetails.longitude.toDouble(),
                        double.locationDetails.latitude.toDouble(),
                        double.locationDetails.longitude.toDouble(),
                        locationResult
                    )
                    if (
                        locationResult[0] < 100 &&
                        double.officeDetails.postcode == pb.officeDetails.postcode
                        && double.officeDetails.name.lowercase().replace(
                            Regex("""\([lr]\)"""),
                            ""
                        ) == pb.officeDetails.name.lowercase().replace(Regex("""\([lr]\)"""), "")
                    ) {
                        doubles.remove(double)
                        pb.double = double
                        return@filter true
                    }
                }
                doubles += pb
                return@filter false

            }.toMutableList()
            // add the remaining double postboxes that we weren't able to match up and sort
            // by distance from user
            postboxData.addAll(doubles)
            postboxData.sortBy { it.locationDetails.distance }
        }
        if (postboxData?.isNotEmpty() == true) {
            callback(postboxData)
            cachePostboxData(context, postcode, postboxData)
        }
        return@launch
    }
}

fun posToUKPostcode(context: Context, pos: LatLng): String? {
    val geocoder = Geocoder(context, Locale.getDefault())

    val addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1)
    if (addresses.isNullOrEmpty()) {
        return null
    }
    val country = addresses[0].countryCode
    val postcode = addresses[0].postalCode
    if (country != "GB" && country != "GBR") {
        // Royal mail's API is only valid in UK and NI. I checked ALL British overseas territories
        // as listed here: https://en.wikipedia.org/wiki/British_Overseas_Territories#Current_overseas_territories.
        // fun fact: most of them don't use postcodes at all (at least not UK style ones)
        // I did also check the Republic of Ireland since they do still have some UK postboxes
        // (but painted green), but Royal Mail does not work there.
        Log.e(LOG_TAG, "Postcode is not in the UK: $postcode - $country")
        throw IllegalArgumentException("Postcode is not in the UK: ${addresses[0].postalCode}")
    }
    return postcode
}

fun getNearbyPostboxes(
    context: Context,
    location: Location,
    callback: (p: List<DetailedPostboxInfo>) -> Unit
) {
    return getNearbyPostboxes(context, LatLng(location.latitude, location.longitude), callback)
}

@Serializable
data class CachedPostboxDetails(
    /** Epoch time in seconds */
    val lastFetch: Long,
    val postboxes: List<DetailedPostboxInfo>
)

private const val CACHE_FILE = "nearby_postboxes_cache.json"
private const val CACHE_EXPIRATION_TIME: Long = 60 * 60 * 24 * 30  // 30 days


suspend fun getPostboxesFromCache(context: Context, postcode: String): List<DetailedPostboxInfo>? {
    var cachedData: Map<String, CachedPostboxDetails>

    cacheFileMutex.withLock {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) {
            return null
        }
        try {
            cachedData = Json.decodeFromString<Map<String, CachedPostboxDetails>>(file.readText())
        } catch (e: SerializationException) {
            Log.w(LOG_TAG, "Failed to parse existing cache data", e)
            return null
        }
    }

    for (data in cachedData) {
        if (data.key != postcode) {
            continue
        }
        if (data.value.lastFetch + CACHE_EXPIRATION_TIME < (System.currentTimeMillis() / 1000)) {
            Log.i(LOG_TAG, "Cached data expired for $postcode")
            return null
        }
        Log.i(LOG_TAG, "Using cached data for $postcode")
        return data.value.postboxes
    }
    Log.i(LOG_TAG, "No cache entry found for $postcode")

    return null
}

/**
 * Cache the postbox data for the given postcode.
 * This function also removes any stale entries at the same time
 */
suspend fun cachePostboxData(
    context: Context,
    postcode: String,
    postboxData: List<DetailedPostboxInfo>
) {
    cacheFileMutex.withLock {
        val file = File(context.filesDir, CACHE_FILE)
        val existingData = mutableMapOf<String, CachedPostboxDetails>()
        if (file.exists()) {
            try {
                existingData += Json.decodeFromString<Map<String, CachedPostboxDetails>>(file.readText())
            } catch (e: SerializationException) {
                Log.w(LOG_TAG, "Failed to parse existing cache data", e)
            }
        }
        existingData[postcode] =
            CachedPostboxDetails((System.currentTimeMillis() / 1000), postboxData)

        file.writeText(JsonParser.encodeToString(existingData))
    }
    Log.i(LOG_TAG, "Cached new entry for $postcode")
}

suspend fun clearPostboxData(context: Context): Boolean {
    cacheFileMutex.withLock {
        val file = File(context.filesDir, CACHE_FILE)
        if (file.exists()) {
            return file.delete();
        }
    }
    return false;
}

suspend fun removeStaleCachedPostboxData(context: Context) {
    cacheFileMutex.withLock {
        val file = File(context.filesDir, CACHE_FILE)
        val existingData = mutableMapOf<String, CachedPostboxDetails>()
        if (file.exists()) {
            try {
                existingData += Json.decodeFromString<Map<String, CachedPostboxDetails>>(file.readText())
            } catch (e: SerializationException) {
                Log.w(LOG_TAG, "Failed to parse existing cache data", e)
            }
        }
        if (existingData.isEmpty()) {
            return
        }
        // filter out any expired entries
        val currentTime = (System.currentTimeMillis() / 1000)
        val prevSize = existingData.size
        existingData.keys.removeAll {
            existingData[it]!!.lastFetch + CACHE_EXPIRATION_TIME < currentTime
        }
        file.writeText(JsonParser.encodeToString(existingData))
        Log.i(LOG_TAG, "Removed ${prevSize - existingData.size} stale cache entries")
    }
}