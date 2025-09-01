package com.crozzers.postboxgo.utils

import android.Manifest
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
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
