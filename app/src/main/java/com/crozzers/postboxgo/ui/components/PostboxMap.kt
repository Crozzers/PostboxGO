package com.crozzers.postboxgo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
fun PostboxMap(
    coords: Pair<Float, Float>,
    modifier: Modifier = Modifier,
    zoom: Float = 15f,
    mapUiSettings: MapUiSettings = MapUiSettings()
) {
    val postboxPos = LatLng(
        coords.first.toDouble(),
        coords.second.toDouble()
    )

    // use this rather than rememberCameraPositionState to make sure it
    // moves the map when we reload with new coords
    val cameraPosState = CameraPositionState(
        CameraPosition.fromLatLngZoom(
            postboxPos, zoom
        )
    )
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPosState,
        uiSettings = mapUiSettings
    ) {
        Marker(
            state = MarkerState(position = postboxPos),
            title = "Postbox"
        )
    }
}