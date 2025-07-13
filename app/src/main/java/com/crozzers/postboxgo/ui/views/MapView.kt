package com.crozzers.postboxgo.ui.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crozzers.postboxgo.Postbox
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun PostboxMapView(postboxes: List<Postbox>, onItemClick: (postbox: Postbox) -> Unit) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(0.0, 0.0), 10f
        )
    }
    var latSum = 0.0
    var lonSum = 0.0
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        postboxes.forEach { postbox ->
            val pos = LatLng(
                postbox.coords.first.toDouble(),
                postbox.coords.second.toDouble()
            )
            latSum += postbox.coords.first
            lonSum += postbox.coords.second
            Marker(
                state = MarkerState(position = pos),
                title = postbox.name,
                snippet = "Click for details",
                onInfoWindowClick = {
                    onItemClick(postbox)
                }
            )
        }
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(latSum / postboxes.size, lonSum / postboxes.size), 7.5f
        )
    }
}