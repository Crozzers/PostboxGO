package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Monarch
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun AddPostbox(locationClient: FusedLocationProviderClient, callback: (p: Postbox) -> Unit) {
    var selectedPostbox by remember { mutableStateOf<DetailedPostboxInfo?>(null) }
    var postboxDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMonarch by remember { mutableStateOf(Monarch.NONE) }
    var monarchDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val nearbyPostboxes = remember { mutableStateListOf<DetailedPostboxInfo>() }
    // store here to pass to other stuff. Hacky but true
    val context = LocalContext.current
    getLocation(LocalContext.current, locationClient) { location ->
        getNearbyPostboxes(context, location) { postboxes ->
            nearbyPostboxes.clear()
            nearbyPostboxes.addAll(postboxes)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Add New Postbox", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = postboxDropdownExpanded,
            onExpandedChange = { postboxDropdownExpanded = !postboxDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value =
                    if (selectedPostbox == null) "Select a postbox"
                    else selectedPostbox!!.officeDetails.name +
                            " (${selectedPostbox!!.locationDetails.distance} miles away)",
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
                                        " (${postbox.locationDetails.distance} miles away)"
                            )
                        },
                        onClick = {
                            selectedPostbox = postbox
                            postboxDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            selectedMonarch = monarch
                            monarchDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = null
                if (selectedPostbox == null) {
                    errorMessage = "Please select a postbox"
                    return@Button
                }
                callback(
                    Postbox(
                        Uuid.random().toString(),
                        Pair(
                            selectedPostbox!!.locationDetails.latitude,
                            selectedPostbox!!.locationDetails.longitude
                        ),
                        selectedMonarch,
                        LocalDateTime.now().toString(),
                        selectedPostbox!!.officeDetails.name,
                        selectedPostbox!!.officeDetails.address3
                    )
                )
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
fun getLocation(
    context: Context,
    locationClient: FusedLocationProviderClient,
    callback: (l: Location) -> Unit
) {
    locationClient.lastLocation.addOnCompleteListener { task ->
        if (task.isSuccessful && task.result != null) {
            callback(task.result)
        } else if (task.isSuccessful) {
            Toast.makeText(context, "Determining current location...", Toast.LENGTH_SHORT).show()
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
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
        } else {
            Toast.makeText(context, "Failed to get current location", Toast.LENGTH_SHORT).show()
        }
    }
}


