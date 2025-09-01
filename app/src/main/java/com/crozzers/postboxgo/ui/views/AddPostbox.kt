package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Monarch
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.InfoDialog
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.getLocation
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import kotlin.uuid.ExperimentalUuidApi

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostbox(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    callback: (p: Postbox) -> Unit
) {
    val locationPermissionGranted = (ActivityCompat.checkSelfPermission(
        LocalContext.current, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
            )
    var tabIndex by remember { mutableIntStateOf(if (locationPermissionGranted) 0 else 1) }
    val tabs = listOf("Nearby", "Select on map")

    var selectedPostbox by remember { mutableStateOf<DetailedPostboxInfo?>(null) }
    var selectedMonarch by remember { mutableStateOf(Monarch.NONE) }
    var verified by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showWarning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    Column {
        TabRow(
            selectedTabIndex = tabIndex, contentColor = MaterialTheme.colorScheme.onPrimary,
            indicator = @Composable { tabPositions: List<TabPosition> ->
                if (tabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) }, selected = tabIndex == index,
                    onClick = {
                        if (index == 0 && !locationPermissionGranted) {
                            showWarning = true
                            return@Tab
                        }
                        tabIndex = index
                    },
                )
            }
        }
    }
    if (showWarning) {
        InfoDialog(
            title = "Location permission required",
            body = (
                    "This app requires location permissions to locate nearby postboxes."
                            + " Please enable location access to register a postbox in this way."
                            + " You can view our privacy policy for details on how this information is used"
                    ),
            dismissButtonText = "Open Privacy Policy",
            confirmButtonText = "Ok"
        ) { state ->
            showWarning = false
            if (state == false) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/Crozzers/PostboxGO/blob/main/privacy-notice.md".toUri()
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (tabIndex) {
            0 -> AddNearbyPostbox(
                locationClient,
                saveFile,
                selectedPostbox,
                selectedMonarch
            ) { p, m ->
                selectedPostbox = p
                selectedMonarch = m
                verified = true
            }

            1 -> AddPostboxFromMap(
                saveFile,
                selectedPostbox,
                selectedMonarch
            ) { p, m ->
                selectedPostbox = p
                selectedMonarch = m
                verified = false
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                errorMessage = null
                if (selectedPostbox == null) {
                    errorMessage = "Please select a postbox"
                    return@Button
                }
                selectedPostbox?.let {
                    callback(
                        Postbox.fromDetailedPostboxInfo(
                            it, selectedMonarch,
                            verified = verified
                        )
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
fun AddNearbyPostbox(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    selectedPostbox: DetailedPostboxInfo?,
    selectedMonarch: Monarch,
    callback: (p: DetailedPostboxInfo?, m: Monarch) -> Unit
) {
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


    when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            SelectNearbyPostbox(
                locationClient,
                selectedPostbox,
                saveFile,
            ) { p ->
                callback(p, selectedMonarch)
            }

            if (selectedPostbox != null) {
                Spacer(modifier = Modifier.height(16.dp))
                PostboxMap(
                    selectedPostbox,
                    Modifier
                        .fillMaxWidth()
                        .height((screenHeight * 0.35).dp),
                    locationClient = locationClient
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SelectMonarch(selectedMonarch) { m -> callback(selectedPostbox, m) }
        }

        else -> {
            Row(
                Modifier
                    .padding(8.dp)
                    .height((screenHeight * 0.35).dp)
            ) {
                Column(Modifier.fillMaxWidth(0.5f)) {
                    SelectNearbyPostbox(locationClient, selectedPostbox, saveFile) { p ->
                        callback(p, selectedMonarch)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectMonarch(selectedMonarch) { m -> callback(selectedPostbox, m) }
                }

                Spacer(Modifier.padding(8.dp))

                if (selectedPostbox != null) {
                    PostboxMap(
                        selectedPostbox, Modifier
                            .fillMaxSize(),
                        locationClient = locationClient
                    )
                }
            }
        }
    }

}

@Composable
fun AddPostboxFromMap(
    saveFile: SaveFile,
    selectedPostbox: DetailedPostboxInfo?,
    selectedMonarch: Monarch,
    callback: (p: DetailedPostboxInfo?, m: Monarch) -> Unit
) {
    var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }
    val configuration = LocalConfiguration.current
    var screenHeight by remember { mutableIntStateOf(configuration.screenHeightDp) }

    val locationPermissionGranted = (ActivityCompat.checkSelfPermission(
        LocalContext.current, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED)

    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect {
                orientation = it
                screenHeight = configuration.screenHeightDp
            }
    }

    val postboxes = remember { mutableStateListOf<DetailedPostboxInfo>() }
    val context = LocalContext.current

    val cameraPosState by remember { mutableStateOf(CameraPositionState()) }
    val cameraUpdateCallback = {
        if (selectedPostbox != null) {
            cameraPosState.move(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        selectedPostbox.locationDetails.latitude.toDouble(),
                        selectedPostbox.locationDetails.longitude.toDouble()
                    ), 15f
                )
            )
        } else {
            cameraPosState.move(
                CameraUpdateFactory.newLatLngZoom(
                    // some place in blackpool. Centers the UK nicely on the map
                    LatLng(53.8176083047311, -3.044017188695928), 6f
                )
            )
        }
    }

    val selectionComponent = @Composable { modifier: Modifier ->
        Column(modifier) {
            SelectPostbox(
                postboxes,
                selectedPostbox,
                onClick = { expanded ->
                    // load in our postboxes
                    val pos = cameraPosState.position.target
                    getNearbyPostboxes(context, pos) { p ->
                        postboxes.clear()
                        postboxes.addAll(p.filter { pb ->
                            val id = "${pb.officeDetails.postcode} ${pb.officeDetails.address1}"
                            saveFile.getPostbox(id) == null
                        })
                    }
                },
                selectionCallback = { p -> callback(p, selectedMonarch) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SelectMonarch(
                selectedMonarch,
                selectionCallback = { m -> callback(selectedPostbox, m) }
            )
        }
    }

    when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            Column(Modifier.padding(4.dp)) {
                Box(Modifier.fillMaxHeight(0.5f)) {
                    GoogleMap(
//                modifier = modifier,
                        cameraPositionState = cameraPosState,
                        properties = MapProperties(
                            isMyLocationEnabled = locationPermissionGranted
                        )
                    ) {
                        cameraUpdateCallback()
                    }

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .offset(y = ((-25).dp))
                    )
                }
                selectionComponent(Modifier)
            }
        }

        else -> {
            Row(
                Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .height((screenHeight * 0.35).dp)
            ) {
                selectionComponent(Modifier.fillMaxWidth(0.5f))
                Spacer(Modifier.size(4.dp))
                Box(Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPosState,
                        properties = MapProperties(
                            isMyLocationEnabled = locationPermissionGranted
                        )
                    ) {
                        cameraUpdateCallback()
                    }

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .offset(y = ((-25).dp))
                    )
                }
            }
        }
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun SelectNearbyPostbox(
    locationClient: FusedLocationProviderClient,
    selectedPostbox: DetailedPostboxInfo?,
    saveFile: SaveFile,
    selectionCallback: (p: DetailedPostboxInfo) -> Unit
) {
    val nearbyPostboxes = remember { mutableStateListOf<DetailedPostboxInfo>() }
    // store here to pass to other stuff. Hacky but true
    val context = LocalContext.current
    getLocation(locationClient) { location ->
        if (location == null) {
            Toast.makeText(context, "Failed to determine current location", Toast.LENGTH_SHORT)
                .show()
        } else {
            getNearbyPostboxes(context, location) { postboxes ->
                nearbyPostboxes.clear()
                nearbyPostboxes.addAll(postboxes.filter { pb ->
                    val id = "${pb.officeDetails.postcode} ${pb.officeDetails.address1}"
                    saveFile.getPostbox(id) == null
                })
            }
        }
    }

    SelectPostbox(
        nearbyPostboxes,
        selectedPostbox,
        "Getting location...",
        selectionCallback = selectionCallback
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPostbox(
    postboxes: List<DetailedPostboxInfo>,
    selectedPostbox: DetailedPostboxInfo?,
    loadingMessage: String = "Loading...",
    onClick: ((expanded: Boolean) -> Unit)? = null,
    selectionCallback: (p: DetailedPostboxInfo) -> Unit
) {
    var postboxDropdownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = postboxDropdownExpanded,
        onExpandedChange = {
            postboxDropdownExpanded = !postboxDropdownExpanded
            onClick?.invoke(postboxDropdownExpanded)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value =
                if (selectedPostbox == null) "Select a postbox"
                else humanReadablePostboxName(selectedPostbox.officeDetails.name) +
                        " (${selectedPostbox.officeDetails.postcode} ${selectedPostbox.officeDetails.address1})" +
                        " (${selectedPostbox.locationDetails.distance} miles away)",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Postbox") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = postboxDropdownExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = postboxDropdownExpanded,
            onDismissRequest = { postboxDropdownExpanded = false }
        ) {
            postboxes.forEach { postbox ->
                DropdownMenuItem(
                    text = {
                        Text(
                            humanReadablePostboxName(postbox.officeDetails.name) +
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
            if (postboxes.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(loadingMessage) },
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
            value = selectedMonarch.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Monarch") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monarchDropdownExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    text = { Text(monarch.displayName) },
                    onClick = {
                        monarchDropdownExpanded = false
                        selectionCallback(monarch)
                    }
                )
            }
        }
    }
}


