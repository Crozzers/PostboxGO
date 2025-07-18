package com.crozzers.postboxgo.ui.views

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.PostboxMap

@Composable
fun DetailsView(postbox: Postbox, saveFile: SaveFile, deleteCallback: () -> Unit) {
    var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }

    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

    when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                PostboxDetails(postbox)
                PostboxMap(postbox, Modifier.fillMaxHeight(0.75f))
                ActionButtons(
                    postbox.coords
                ) { state ->
                    if (state) {
                        saveFile.removePostbox(postbox)
                        deleteCallback()
                    }
                }
            }
        }

        else -> {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .verticalScroll(rememberScrollState())
                ) {
                    PostboxDetails(postbox)
                    ActionButtons(
                        postbox.coords
                    ) { state ->
                        if (state) {
                            saveFile.removePostbox(postbox)
                            deleteCallback()
                        }
                    }
                }
                Column(
                    Modifier.fillMaxSize()
                ) {
                    PostboxMap(postbox, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun PostboxDetails(postbox: Postbox) {
    Text(text = postbox.name, fontSize = 24.sp)
    Text(text = "Registered: ${postbox.dateRegistered}", fontSize = 12.sp)
    Text(text = "Type: ${postbox.type ?: "Unknown"}")
    Text(text = "Monarch: ${postbox.monarch}")
    Text(text = "Location: ${postbox.coords.first}, ${postbox.coords.second}")
}


@Composable
fun ActionButtons(coords: Pair<Float, Float>, deleteCallback: (s: Boolean) -> Unit) {
    val context = LocalContext.current
    val openConfirmDeleteDialog = remember { mutableStateOf(false) }

    Spacer(Modifier.height(16.dp))
    Button({
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://maps.google.com/maps/dir//${coords.first},${coords.second}".toUri()
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row() {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete Postbox",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = "Delete Postbox", color = MaterialTheme.colorScheme.primary)
        }
    }
    when {
        openConfirmDeleteDialog.value -> {
            ConfirmDeleteDialog(callback = { state ->
                openConfirmDeleteDialog.value = false
                deleteCallback(state)
            })
        }
    }
}

@Composable
fun ConfirmDeleteDialog(callback: (state: Boolean) -> Unit) {
    // TODO: fix in light/dark mode
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