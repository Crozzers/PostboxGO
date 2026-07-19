package com.crozzers.postboxgo

import com.crozzers.postboxgo.utils.constructRMQuery
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test

private val JsonParser: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}

/**
 * Test that RM API query works
 */
class RoyalMailAPITest {
    @Test
    fun apiQueryWorks() {
        val (url, headers) = constructRMQuery(
            "WC1X 0DL",
            51.524352,
            -0.113727
        )

        var builder = Request.Builder().url(url)
        for ((key, value) in headers.entries) {
            builder = builder.header(key, value)
        }
        val request = builder.build()

        val client = OkHttpClient.Builder().build()

        val response = client.newCall(request).execute()

        if (response.code != 200) {
            throw Error("${response.code} - ${response.message}")
        }

        val postboxData = JsonParser.decodeFromString<List<DetailedPostboxInfo>>(response.body.string())

        assert(postboxData[0].type == "PB")
    }
}