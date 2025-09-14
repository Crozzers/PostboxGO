package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.InfoDialog
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.checkAndRequestLocation
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import com.crozzers.postboxgo.utils.isPostboxVerified
import com.google.android.gms.location.FusedLocationProviderClient
import kotlin.uuid.ExperimentalUuidApi

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun EditPostbox(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    postbox: Postbox,
    callback: (p: Postbox) -> Unit
) {
    var selectedMonarch by remember { mutableStateOf(postbox.monarch) }
    var selectedType by remember { mutableStateOf<String?>(postbox.type) }

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
        Text(
            humanReadablePostboxName(postbox.name), style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                if (!postbox.verified) {
                    Row(Modifier.fillMaxWidth()) {
                        VerifyPostbox(locationClient, postbox) {
                            postbox.verified = it
                            saveFile.save()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                PostboxDetailsBrief(postbox)
                PostboxMap(
                    postbox,
                    Modifier
                        .fillMaxWidth()
                        .height((screenHeight * 0.35).dp),
                    locationClient = locationClient
                )
                if (postbox.inactive) {
                    Spacer(Modifier.height(16.dp))
                    SelectPostboxType(selectedType) {
                        selectedType = it
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
                Spacer(modifier = Modifier.height(16.dp))
            }

            else -> {
                Row(
                    Modifier
                        .padding(8.dp)
                        .weight(1f)
                ) {
                    Column(Modifier.fillMaxWidth(0.5f)) {
                        if (!postbox.verified) {
                            VerifyPostbox(locationClient, postbox) {
                                postbox.verified = it
                                saveFile.save()
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        PostboxDetailsBrief(postbox)
                        if (postbox.inactive) {
                            SelectPostboxType(selectedType) {
                                selectedType = it
                            }
                        }
                        SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
                    }
                    Spacer(Modifier.padding(8.dp))
                    PostboxMap(
                        postbox,
                        Modifier.fillMaxSize(),
                        locationClient = locationClient
                    )
                }
            }
        }

        Button(
            onClick = {
                postbox.monarch = selectedMonarch
                postbox.type = selectedType
                callback(postbox)
            }
        ) {
            Text("Save postbox")
        }
    }
}


@Composable
fun PostboxDetailsBrief(postbox: Postbox) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text("Registered: ${humanReadableDate(postbox.dateRegistered)}")
        Text("ID: ${postbox.id}")
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun VerifyPostbox(
    locationClient: FusedLocationProviderClient,
    postbox: Postbox,
    callback: (Boolean) -> Unit
) {
    var text by remember { mutableStateOf("Verify Postbox") }
    var enabled by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val locationPermissionCallback = checkAndRequestLocation(
        "Location permissions are required to verify that you are near to a postbox. Please grant location permissions and try again"
    ) {
        // do a quick check here because kotlin doesn't know that the permission is already granted
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            text = "Getting current location..."

            isPostboxVerified(locationClient, postbox) {
                if (it) {
                    callback(true)
                    enabled = false
                    text = "Postbox verified!"
                } else {
                    text = "Verify Postbox"
                    showDialog = true
                }
            }
        }
    }

    Button(
        onClick = {
            showDialog = false
            locationPermissionCallback()
        },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(disabledContentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        if (enabled) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Unverified postbox",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Verified postbox",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.width(2.dp))
        Text(text)
    }

    if (showDialog) {
        InfoDialog(
            title = "Verification Failed",
            body = "You must be less than 2.5km away from the postbox to verify it",
            icon = Icons.Filled.Warning,
            dismissButtonText = null
        )
    }
}