package com.crozzers.postboxgo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.utils.bitmapDescriptorFromDrawable
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Display a map with markers for each postbox
 *
 * @param postboxes List of postboxes to display
 * @param modifier Modifier to apply to the map
 * @param enableGestures Whether to enable gesture interactions with the map (eg, panning)
 * @param locationClient Optional location client to use to show the user's location
 * @param zoom Zoom level to display map at initially
 * @param centreOnLocation Whether to center on user location, rather than overall postbox markers
 * @param marker Custom marker for each postbox. If null, a postbox icon will be displayed
 * @param onPostboxClick Callback for when a user clicks the postbox marker info window
 */
@Composable
fun PostboxMap(
    postboxes: Collection<Postbox>,
    modifier: Modifier = Modifier,
    enableGestures: Boolean = true,
    locationClient: FusedLocationProviderClient? = null,
    zoom: Float? = null,
    centreOnLocation: Boolean = false,
    marker: (@Composable (postbox: Postbox) -> Unit)? = null,
    onPostboxClick: ((postbox: Postbox) -> Unit)? = null
) {
    val boundsBuilder = LatLngBounds.builder()
    // use this rather than rememberCameraPositionState to make sure it
    // moves the map when we reload with new coords
    val cameraPosState by remember { mutableStateOf(CameraPositionState()) }

    // if we provide a location client and IF permission is enabled, show that
    // on the map and zoom to fit
    if (locationClient != null) {
        if (ActivityCompat.checkSelfPermission(
                LocalContext.current,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                LocalContext.current,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    var builder = boundsBuilder
                    if (centreOnLocation) {
                        // if centering only on location then create fresh bounds excluding
                        // the postbox markers
                        builder = LatLngBounds.builder()
                    }
                    builder.include(
                        LatLng(
                            location.latitude, location.longitude
                        )
                    )
                    cameraPosState.move(
                        CameraUpdateFactory.newLatLngBounds(
                            builder.build(),
                            150
                        )
                    )
                    if (zoom != null) {
                        cameraPosState.move(CameraUpdateFactory.zoomTo(zoom))
                    }
                }
            }
        }
    }

    val mapSettings = MapUiSettings(
        scrollGesturesEnabled = enableGestures,
        scrollGesturesEnabledDuringRotateOrZoom = enableGestures,
        zoomGesturesEnabled = enableGestures,
        tiltGesturesEnabled = enableGestures,
        rotationGesturesEnabled = enableGestures
    )

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPosState,
        uiSettings = mapSettings,
        properties = MapProperties(
            isMyLocationEnabled = locationClient != null && ActivityCompat.checkSelfPermission(
                LocalContext.current, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            // make sure we don't zoom way into the map and make it useless
            maxZoomPreference = 17f,
        )
    ) {
        postboxes.forEach { postbox ->
            val pos = LatLng(
                postbox.coords.first.toDouble(),
                postbox.coords.second.toDouble()
            )
            boundsBuilder.include(pos)
            if (marker != null) {
                marker(postbox)
            } else {
                Marker(
                    state = MarkerState(position = pos),
                    title = postbox.name,
                    onInfoWindowClick = { onPostboxClick?.invoke(postbox) },
                    snippet = if (onPostboxClick != null) "Click for details" else null,
                    icon = bitmapDescriptorFromDrawable(
                        LocalContext.current,
                        getIconFromPostboxType(postbox.type)
                    ),
                    alpha = if (postbox.inactive) 0.5f else 1.0f
                )
            }
        }
        if (postboxes.isEmpty()) {
            // mail rail location
            boundsBuilder.include(LatLng(51.52461615735085, -0.11318969493024554))
        }
        cameraPosState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
        if (zoom != null) {
            cameraPosState.move(CameraUpdateFactory.zoomTo(zoom))
        }
    }
}

@Composable
fun PostboxMap(
    postbox: Postbox,
    modifier: Modifier = Modifier,
    enableGestures: Boolean = true,
    locationClient: FusedLocationProviderClient? = null
) {
    return PostboxMap(listOf(postbox), modifier, enableGestures, locationClient)
}

@Composable
fun PostboxMap(
    postbox: DetailedPostboxInfo,
    modifier: Modifier = Modifier,
    enableGestures: Boolean = true,
    locationClient: FusedLocationProviderClient? = null
) {
    return PostboxMap(
        listOf(Postbox.fromDetailedPostboxInfo(postbox)),
        modifier,
        enableGestures,
        locationClient
    )
}
