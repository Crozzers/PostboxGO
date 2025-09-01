package com.crozzers.postboxgo.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.net.toUri
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.ui.components.InfoDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority

private var lastFetch: Long? = null
private const val TEN_MINUTES = 10 * 60 * 1000
private const val LOG_TAG = "Location"

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun getLocation(
    locationClient: FusedLocationProviderClient,
    refresh: Boolean = false,
    callback: (l: Location?) -> Unit
) {
    val refreshCurrentLocationFirst = refresh ||
            lastFetch == null || System.currentTimeMillis() - TEN_MINUTES > (lastFetch ?: 0)
    Log.i(LOG_TAG, "Location requested. Refresh current location: $refreshCurrentLocationFirst")
    (
            if (refreshCurrentLocationFirst) locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            else locationClient.lastLocation
            ).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                if (refreshCurrentLocationFirst) {
                    lastFetch = System.currentTimeMillis()
                }
                callback(task.result)
            } else {
                (
                        if (refreshCurrentLocationFirst) locationClient.lastLocation
                        else locationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        )
                        ).addOnCompleteListener { subtask ->
                        if (subtask.isSuccessful && subtask.result != null) {
                            if (!refreshCurrentLocationFirst) {
                                lastFetch = System.currentTimeMillis()
                            }
                            callback(subtask.result)
                        } else {
                            callback(null)
                        }
                    }
            }
        }
}


@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun isPostboxVerified(
    locationClient: FusedLocationProviderClient,
    postbox: Postbox,
    callback: (Boolean) -> Unit
) {
    getLocation(locationClient, true) {
        if (it == null) {
            callback(false)
            return@getLocation
        }
        val results = floatArrayOf(0.0F)
        Location.distanceBetween(
            it.latitude, it.longitude,
            postbox.coords.first.toDouble(), postbox.coords.second.toDouble(), results
        )
        callback(results[0] <= 2500)
    }
}

/**
 * Convenience function that handles checking location permissions, requesting them if
 * necessary and then continuing the flow of the program once granted
 *
 * @param message Message to display to the user if the request is denied, explaining
 *   why location permissions were requested
 * @param callback Callback that happens once location permissions are granted
 *
 * @return A function that can be called in a non-composable context (eg: `Button.onClick`)
 *   that will check, request and action a location request
 */
@Composable
fun checkAndRequestLocation(
    message: String = "This feature requires location permissions. Please grant location permissions and try again",
    callback: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    var showLocationWarningDialog by remember { mutableStateOf(false) }

    // try to get activity from context
    var activity: Context = LocalContext.current
    while (activity is ContextWrapper) {
        if (activity is Activity) {
            break
        }
        activity = activity.baseContext
    }

    /** Whether to show the explanation before or after the permissions request */
    val showRationaleFirst = remember {
        if (activity is Activity) shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        else false
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // permission granted, wahoo
                callback()
            } else {
                // if we showed the explanation first, don't show it again
                showLocationWarningDialog = !showRationaleFirst
            }
        }

    if (showLocationWarningDialog) {
        InfoDialog(
            title = "Location permissions required",
            body = message,
            icon = Icons.Filled.Warning,
            dismissButtonText = "Open Privacy Policy",
        ) { state ->
            // deal with user's action, whether they want to open privacy policy etc
            showLocationWarningDialog = false
            if (state == false) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/Crozzers/PostboxGO/blob/main/privacy-notice.md".toUri()
                    )
                )
            }
            // if we showed the info dialog first, now we ask for permissions
            // see `rememberLauncherForActivityResult` callback for next steps
            if (showRationaleFirst) {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    return {
        // this function is called when the user takes action

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // if we have permission, proceed as planned
            callback()
        } else if (showRationaleFirst) {
            // if we need to show the rationale first, show the info dialog
            // see info dialog's callback for what happens next
            showLocationWarningDialog = true
        } else {
            // if we don't need to show the rationale, ask for permissions straight away
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
