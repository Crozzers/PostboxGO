package com.crozzers.postboxgo.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.crozzers.postboxgo.R
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val LOG_TAG = "update_check"

private val JsonParser: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}

private var cachedUpdateCheck: URL? = null

fun checkForUpdate(
    context: Context,
    track: ReleaseTrack = ReleaseTrack.STABLE,
    callback: (URL) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        if (cachedUpdateCheck != null) {
            callback(cachedUpdateCheck!!)
            return@launch
        }

        val url = URL("https://api.github.com/repos/Crozzers/PostboxGO/tags")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            try {
                connect()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "failed to connect to github API: ${e.message}")
                return@launch
            }

            if (responseCode != 200) {
                Log.e(
                    LOG_TAG,
                    "failed to fetch latest tags - code $responseCode - message: $responseMessage"
                )
                return@launch
            }

            val versionRegex = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?""")
            val currentVersion =
                versionRegex.matchEntire(
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    ).versionName.toString()
                )
            if (currentVersion == null) {
                val version =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                Log.e(LOG_TAG, "failed to parse current version number: $version")
                return@launch
            }

            val releaseLabels = ReleaseTrack.entries.reversed().map { it.label }
            val latestTags = JsonParser.decodeFromString<List<GHTag>>(
                inputStream.bufferedReader().readText()
            ).map { versionRegex.matchEntire(it.name) }
                .filter { it != null }
                .sortedWith { a, b ->
                    // parse the MAJOR.MINOR.PATCH numbers
                    for (p in 1..3) {
                        val partA = a!!.groupValues[p].toInt()
                        val partB = b!!.groupValues[p].toInt()
                        if (partA != partB) {
                            return@sortedWith if (partA > partB) -1 else 1
                        }
                    }
                    // parse release labels by priority
                    val aLabel = releaseLabels.indexOf(a!!.groupValues[4])
                    val bLabel = releaseLabels.indexOf(b!!.groupValues[4])
                    if (aLabel != bLabel) {
                        return@sortedWith if (aLabel > bLabel) -1 else 1
                    }
                    // parse release numbers
                    val aLabelVer = a.groupValues[5].toInt()
                    val bLabelVer = b.groupValues[5].toInt()
                    if (aLabelVer != bLabelVer) {
                        return@sortedWith if (aLabelVer > bLabelVer) -1 else 1
                    }
                    0
                }
                .filter {
                    // filter by releases with a similar priority. We don't want users on stable
                    // versions upgrading to pre-release versions, but we can have users on alpha versions
                    // upgrading to beta or RC versions
                    it!!.groupValues[4] == track.label ||
                            releaseLabels.indexOf(it.groupValues[4]) >= releaseLabels.indexOf(track.label)
                }
            if (latestTags.isEmpty() || latestTags.first()!!.value == currentVersion.value) {
                Log.i(LOG_TAG, "no new tags found")
                return@launch
            }
            Log.i(
                LOG_TAG,
                "newer version found: ${latestTags[0]} - current: ${currentVersion.value}"
            )
            cachedUpdateCheck =
                URL("https://github.com/Crozzers/PostboxGO/releases/tag/${latestTags[0]!!.value}")
            callback(cachedUpdateCheck!!)
        }
    }
}

fun isManuallyInstalled(context: Context): Boolean {
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    val manual = !listOf("com.android.vending", "com.google.android.feedback").contains(installer)
    Log.i(LOG_TAG, "Manually installed: $manual, installer: $installer")
    return manual
}

@Composable
fun UpdateCheck() {
    val settings = LocalContext.current.settings
    val checkForUpdates = settings.data.map { preferences ->
        preferences[Setting.CHECK_FOR_UPDATES] ?: true
    }.collectAsState(initial = true)
    val selectedReleaseTrack = settings.data.map { preferences ->
        preferences[Setting.RELEASE_TRACK] ?: ReleaseTrack.STABLE.name
    }.collectAsState(initial = ReleaseTrack.STABLE.name)
    val currentVersion = LocalContext.current.packageManager.getPackageInfo(
        LocalContext.current.packageName,
        0
    ).versionName
    var showDialog by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<URL?>(null) }
    if (checkForUpdates.value) {
        checkForUpdate(LocalContext.current, ReleaseTrack.valueOf(selectedReleaseTrack.value)) {
            showDialog = true
            latest = it
        }
    }
    if (showDialog) {
        val context = LocalContext.current

        val openUpdatesIntent = Intent(
            Intent.ACTION_VIEW,
            latest!!.toString().toUri()
        )
        val builder = NotificationCompat.Builder(LocalContext.current, "PBG_APP_UPDATES")
            .setSmallIcon(R.drawable.pbg_notification)
            .setColor(Color.RED)
            .setContentTitle("App update available")
            .setContentText(
                "Version ${
                    latest.toString().split('/').last()
                } is available. Click to download"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    LocalContext.current,
                    0,
                    openUpdatesIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(LocalContext.current)) {
            if (ActivityCompat.checkSelfPermission(
                    LocalContext.current,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(123, builder.build())
            }
        }
    }
}

@Serializable
private data class GHTag(
    val name: String
)

enum class ReleaseTrack(val displayName: String, val label: String) {
    STABLE("Stable", ""),
    ALPHA("Alpha", "alpha"),
    BETA("Beta", "beta"),
    RC("Release Candidate", "rc");

    override fun toString(): String {
        return displayName
    }
}