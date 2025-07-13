package com.crozzers.postboxgo

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun ViewPostbox(postbox: Postbox, saveFile: SaveFile, deleteCallback: () -> Unit) {
    val postboxPos = LatLng(
        postbox.coords.first.toDouble(),
        postbox.coords.second.toDouble()
    )
    val cameraPosState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            postboxPos, 15f
        )
    }
    val context = LocalContext.current
    val openConfirmDeleteDialog = remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = postbox.name, fontSize = 24.sp)
        Text(text = "Registered: ${postbox.dateRegistered}", fontSize = 12.sp, color = Color.Gray)
        Text(text = "Type: ${postbox.type ?: "Unknown"}")
        Text(text = "Monarch: ${postbox.monarch}")
        Text(text = "Location: ${postbox.coords.first}, ${postbox.coords.second}")
        GoogleMap(
            modifier = Modifier
                .height(500.dp),
            cameraPositionState = cameraPosState,
        ) {
            Marker(
                state = MarkerState(position = postboxPos),
                title = "Postbox"
            )
        }
        Spacer(Modifier.height(16.dp))
        Button({
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://maps.google.com/maps/dir//${postbox.coords.first},${postbox.coords.second}".toUri()
                )
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Row() {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Get Directions")
                Text(text = "Get Directions")
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            {
                openConfirmDeleteDialog.value = true
            },
            modifier = Modifier
                .fillMaxWidth(),
            border = BorderStroke(1.dp, Color.Red),
        ) {
            Row() {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Postbox",
                    tint = Color.Red
                )
                Text(text = "Delete Postbox", color = Color.Red)
            }
        }
        when {
            openConfirmDeleteDialog.value -> {
                ConfirmDelete(callback = { state ->
                    openConfirmDeleteDialog.value = false
                    if (state) {
                        saveFile.removePostbox(postbox)
                        deleteCallback()
                    }

                })
            }
        }
    }
}

@Composable
fun ConfirmDelete(callback: (state: Boolean) -> Unit) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Are you sure"
            )
        },
        title = { Text("Are you sure?") },
        text = { Text("This action cannot be undone") },
        confirmButton = {
            TextButton(
                onClick = {
                    callback(true)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    callback(false)
                }
            ) {
                Text("Dismiss")
            }
        },
        onDismissRequest = {
            callback(false)
        },
    )
}