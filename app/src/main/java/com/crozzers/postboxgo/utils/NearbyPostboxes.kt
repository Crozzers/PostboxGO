package com.crozzers.postboxgo.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import com.crozzers.postboxgo.DetailedPostboxInfo
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
    location: Location,
    callback: (p: List<DetailedPostboxInfo>) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())

    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
    if (addresses.isNullOrEmpty()) {
        Log.e(LOG_TAG, "Failed to determine postcode")
        Toast.makeText(context, "Failed to determine postcode", Toast.LENGTH_SHORT).show()
        return
    }
    val postcode = addresses[0].postalCode

    CoroutineScope(Dispatchers.IO).launch {
        var postboxData: List<DetailedPostboxInfo>? = getPostboxesFromCache(context, postcode)
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
            postboxData = JsonParser.decodeFromString<List<DetailedPostboxInfo>>(
                inputStream.bufferedReader().readText()
            ).filter { postbox -> postbox.type == "PB" }
        }
        if (postboxData != null) {
            callback(postboxData)
            cachePostboxData(context, postcode, postboxData)
        }
        return@launch
    }
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
suspend fun cachePostboxData(context: Context, postcode: String, postboxData: List<DetailedPostboxInfo>) {
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
        existingData[postcode] = CachedPostboxDetails((System.currentTimeMillis() / 1000), postboxData)

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