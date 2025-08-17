package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Monarch
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlin.uuid.ExperimentalUuidApi

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun AddPostbox(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    callback: (p: Postbox) -> Unit
) {
    var selectedPostbox by remember { mutableStateOf<DetailedPostboxInfo?>(null) }
    var selectedMonarch by remember { mutableStateOf(Monarch.NONE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }
    val configuration = LocalConfiguration.current
    var screenHeight by remember { mutableIntStateOf(configuration.screenHeightDp) }

    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect {
                orientation = it
                screenHeight = configuration.screenHeightDp
            }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Add New Postbox", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                SelectPostbox(
                    locationClient,
                    selectedPostbox,
                    saveFile
                ) { p ->
                    selectedPostbox = p
                }

                if (selectedPostbox != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PostboxMap(
                        selectedPostbox!!,
                        Modifier
                            .fillMaxWidth()
                            .height((screenHeight * 0.35).dp),
                        locationClient = locationClient
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
            }

            else -> {
                Row(Modifier.padding(8.dp)) {
                    Column(Modifier.fillMaxWidth(0.5f)) {
                        SelectPostbox(locationClient, selectedPostbox, saveFile) { p ->
                            selectedPostbox = p
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
                    }

                    Spacer(Modifier.padding(8.dp))

                    if (selectedPostbox != null) {
                        PostboxMap(
                            selectedPostbox!!, Modifier
                                .fillMaxWidth()
                                .height((screenHeight * 0.4).dp),
                            locationClient = locationClient
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                errorMessage = null
                if (selectedPostbox == null) {
                    errorMessage = "Please select a postbox"
                    return@Button
                }
                selectedPostbox?.let {
                    callback(
                        Postbox.fromDetailedPostboxInfo(it, selectedMonarch)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Postbox")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun SelectPostbox(
    locationClient: FusedLocationProviderClient,
    selectedPostbox: DetailedPostboxInfo?,
    saveFile: SaveFile,
    selectionCallback: (p: DetailedPostboxInfo) -> Unit
) {
    var postboxDropdownExpanded by remember { mutableStateOf(false) }

    val nearbyPostboxes = remember { mutableStateListOf<DetailedPostboxInfo>() }
    // store here to pass to other stuff. Hacky but true
    val context = LocalContext.current
    getLocation(LocalContext.current, locationClient) { location ->
        getNearbyPostboxes(context, location) { postboxes ->
            nearbyPostboxes.clear()
            nearbyPostboxes.addAll(postboxes.filter { pb ->
                val id = "${pb.officeDetails.postcode} ${pb.officeDetails.address1}"
                saveFile.getPostbox(id) == null
            })
        }
    }

    ExposedDropdownMenuBox(
        expanded = postboxDropdownExpanded,
        onExpandedChange = { postboxDropdownExpanded = !postboxDropdownExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value =
                if (selectedPostbox == null) "Select a postbox"
                else selectedPostbox.officeDetails.name +
                        " (${selectedPostbox.officeDetails.postcode} ${selectedPostbox.officeDetails.address1})" +
                        " (${selectedPostbox.locationDetails.distance} miles away)",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Postbox") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = postboxDropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            )
        )
        ExposedDropdownMenu(
            expanded = postboxDropdownExpanded,
            onDismissRequest = { postboxDropdownExpanded = false }
        ) {
            nearbyPostboxes.forEach { postbox ->
                DropdownMenuItem(
                    text = {
                        Text(
                            postbox.officeDetails.name +
                                    " (${postbox.officeDetails.postcode} ${postbox.officeDetails.address1})" +
                                    " (${postbox.locationDetails.distance} miles away)"
                        )
                    },
                    onClick = {
                        postboxDropdownExpanded = false
                        selectionCallback(postbox)
                    }
                )
            }
            if (nearbyPostboxes.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Getting location...") },
                    onClick = { },
                    enabled = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMonarch(selectedMonarch: Monarch, selectionCallback: (m: Monarch) -> Unit) {
    var monarchDropdownExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = monarchDropdownExpanded,
        onExpandedChange = { monarchDropdownExpanded = !monarchDropdownExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedMonarch.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Monarch") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monarchDropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            )
        )
        ExposedDropdownMenu(
            expanded = monarchDropdownExpanded,
            onDismissRequest = { monarchDropdownExpanded = false }
        ) {
            Monarch.entries.forEach { monarch ->
                DropdownMenuItem(
                    text = { Text(monarch.name) },
                    onClick = {
                        monarchDropdownExpanded = false
                        selectionCallback(monarch)
                    }
                )
            }
        }
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun getLocation(
    context: Context,
    locationClient: FusedLocationProviderClient,
    callback: (l: Location) -> Unit
) {
    locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                callback(task.result)
            } else {
                Toast.makeText(context, "Determining last known location...", Toast.LENGTH_SHORT)
                    .show()
                locationClient.lastLocation
                    .addOnCompleteListener { subtask ->
                        if (subtask.isSuccessful && subtask.result != null) {
                            callback(subtask.result)
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to get current location",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
}


