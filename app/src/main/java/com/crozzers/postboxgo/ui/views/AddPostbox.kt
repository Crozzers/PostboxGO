package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.LocationDetails
import com.crozzers.postboxgo.Monarch
import com.crozzers.postboxgo.PostOfficeDetails
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.DropdownMenu
import com.crozzers.postboxgo.ui.components.PostboxIcon
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.checkAndRequestLocation
import com.crozzers.postboxgo.utils.getLocation
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.crozzers.postboxgo.utils.isPostboxVerified
import com.crozzers.postboxgo.utils.posToUKPostcode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties

private const val LOG_TAG = "AddPostbox"

@Composable
fun AddPostbox(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    callback: (p: Postbox) -> Unit
) {
    val locationPermissionGranted = (
            ActivityCompat.checkSelfPermission(
                LocalContext.current, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            )
    var tabIndex by remember { mutableIntStateOf(if (locationPermissionGranted) 0 else 1) }
    val tabs = listOf("Nearby", "Select on map", "Inactive postbox")

    var selectedPostbox by remember { mutableStateOf<DetailedPostboxInfo?>(null) }
    var selectedMonarch by remember { mutableStateOf(Monarch.NONE) }
    var verified by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                val locationCheckCallback = checkAndRequestLocation(
                    (
                            "Location permissions are required to locate nearby postboxes."
                                    + " Please enable location access to register a postbox in this way."
                                    + " You can view our privacy policy for details on how this information is used"
                            )
                ) {
                    tabIndex = index
                }
                Tab(
                    text = { Text(title) }, selected = tabIndex == index,
                    onClick = {
                        if (index == 0) {
                            locationCheckCallback()
                        } else {
                            tabIndex = index
                        }
                    },
                )
            }
        }


        fun verifyPostbox(context: Context, p: DetailedPostboxInfo?) {
            // if location permission enabled, run a quick verification check in the
            // background
            if (p != null && ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(
                    LOG_TAG,
                    "Running background verification check on ${p.officeDetails.name}"
                )
                isPostboxVerified(
                    locationClient, Postbox.fromDetailedPostboxInfo(p)
                ) { state ->
                    // location grabs can take time so check if selected postbox is
                    // still the one we're checking
                    if (state && selectedPostbox == p) {
                        verified = true
                    }
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
            val context = LocalContext.current
            when (tabIndex) {
                0 -> AddNearbyPostbox(
                    Modifier.weight(1f),
                    locationClient,
                    saveFile,
                    selectedPostbox,
                    selectedMonarch
                ) { p, m ->
                    Log.d(LOG_TAG, "Nearby postbox ($p) selected with monarch $m")
                    selectedPostbox = p
                    selectedMonarch = m
                    verified = true
                }

                1 -> AddPostboxFromMap(
                    Modifier.weight(1f),
                    saveFile,
                    selectedPostbox,
                    selectedMonarch
                ) { p, m ->
                    Log.d(LOG_TAG, "Postbox ($p) selected from map with monarch $m")
                    selectedPostbox = p
                    selectedMonarch = m
                    verified = false
                    verifyPostbox(context, p)
                }

                2 -> AddInactivePostbox(
                    Modifier.weight(1f),
                    locationClient,
                    selectedPostbox,
                    selectedMonarch
                ) { p, m ->
                    Log.d(LOG_TAG, "Inactive postbox ($p) selected with monarch $m")
                    selectedPostbox = p
                    selectedMonarch = m
                    verified = false
                    verifyPostbox(context, p)
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
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun AddNearbyPostbox(
    modifier: Modifier = Modifier,
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
            Column(modifier, verticalArrangement = Arrangement.Center) {
                if (selectedPostbox != null) {
                    PostboxMap(
                        selectedPostbox,
                        Modifier
                            .fillMaxWidth()
                            .height((screenHeight * 0.35).dp),
                        locationClient = locationClient
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SelectNearbyPostbox(
                    locationClient,
                    selectedPostbox,
                    saveFile,
                ) { p ->
                    callback(p, selectedMonarch)
                }
                Spacer(modifier = Modifier.height(16.dp))
                SelectMonarch(selectedMonarch) { m -> callback(selectedPostbox, m) }
            }
        }

        else -> {
            Row(modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
    modifier: Modifier = Modifier,
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
                selectionCallback = { p ->
                    callback(p, selectedMonarch)
                    // center on selected PB
                    cameraPosState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            // some place in blackpool. Centers the UK nicely on the map
                            LatLng(
                                p.locationDetails.latitude.toDouble(),
                                p.locationDetails.longitude.toDouble()
                            ), 17f
                        )
                    )
                }
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
            Column(modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
                Box(Modifier.fillMaxHeight(0.5f)) {
                    GoogleMap(
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
                Spacer(Modifier.size(8.dp))
                selectionComponent(Modifier)
            }
        }

        else -> {
            Row(
                modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .height((screenHeight * 0.35).dp),
                verticalAlignment = Alignment.CenterVertically
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

@Composable
fun AddInactivePostbox(
    modifier: Modifier = Modifier,
    locationClient: FusedLocationProviderClient,
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

    val context = LocalContext.current

    val cameraPosState by remember { mutableStateOf(CameraPositionState()) }
    if (selectedPostbox != null) {
        cameraPosState.move(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    selectedPostbox.locationDetails.latitude.toDouble(),
                    selectedPostbox.locationDetails.longitude.toDouble()
                ),
                15f
            )
        )
    } else if (locationPermissionGranted) {
        getLocation(locationClient) { location ->
            if (location != null) {
                cameraPosState.move(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 15f
                    )
                )
            }
        }
    }

    val selectionComponent = @Composable { modifier: Modifier ->
        Column(modifier) {
            SelectPostboxType(selectedPostbox?.officeDetails?.address3) {
                var postcode = try {
                    posToUKPostcode(context, cameraPosState.position.target)
                } catch (_: IllegalArgumentException) {
                    null
                }
                postcode = postcode ?: "N/A"

                callback(
                    DetailedPostboxInfo(
                        type = "inactive",
                        officeDetails = PostOfficeDetails(
                            name = "Inactive Postbox ($postcode)",
                            address1 = "",
                            address3 = it,
                            postcode = postcode,
                            specialCharacteristics = "",
                            specialPostboxDescription = "",
                            isPriorityPostbox = false,
                            isSpecialPostbox = false
                        ),
                        locationDetails = LocationDetails(
                            latitude = cameraPosState.position.target.latitude.toFloat(),
                            longitude = cameraPosState.position.target.longitude.toFloat(),
                            distance = 0.0f
                        )
                    ),
                    selectedMonarch
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            SelectMonarch(
                selectedMonarch,
                selectionCallback = { m -> callback(selectedPostbox, m) }
            )
        }
    }

    when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            Column(modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
                Box(Modifier.fillMaxHeight(0.5f)) {
                    GoogleMap(
                        cameraPositionState = cameraPosState,
                        properties = MapProperties(
                            isMyLocationEnabled = locationPermissionGranted
                        )
                    )

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
                Spacer(Modifier.size(8.dp))
                selectionComponent(Modifier)
            }
        }

        else -> {
            Row(
                modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .height((screenHeight * 0.35).dp),
                verticalAlignment = Alignment.CenterVertically
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
                    )

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

@Composable
fun SelectPostbox(
    postboxes: List<DetailedPostboxInfo>,
    selectedPostbox: DetailedPostboxInfo?,
    loadingMessage: String = "Loading...",
    onClick: ((expanded: Boolean) -> Unit)? = null,
    selectionCallback: (p: DetailedPostboxInfo) -> Unit
) {
    DropdownMenu(
        "Postbox",
        postboxes,
        selectedPostbox,
        loadingMessage,
        onClick
    ) {
        if (it != null) {
            selectionCallback(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMonarch(selectedMonarch: Monarch, selectionCallback: (m: Monarch) -> Unit) {
    var monarchDropdownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = monarchDropdownExpanded,
        onExpandedChange = {
            monarchDropdownExpanded = !monarchDropdownExpanded
        },
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
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = monarchDropdownExpanded,
            onDismissRequest = { monarchDropdownExpanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Monarch.entries.forEach { monarch ->
                DropdownMenuItem(
                    text = {
                        Text(monarch.displayName)
                    },
                    onClick = {
                        monarchDropdownExpanded = false
                        selectionCallback(monarch)
                    },
                    leadingIcon = {
                        if (monarch.icon == null) {
                            Box(
                                Modifier
                                    .padding(10.dp)
                                    .size(96.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = monarch.icon),
                                contentDescription = "Royal Cypher of ${monarch.displayName}",
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(96.dp),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPostboxType(
    selectedType: String?,
    onClick: (String) -> Unit
) {
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = typeDropdownExpanded,
        onExpandedChange = {
            typeDropdownExpanded = !typeDropdownExpanded
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedType ?: "Select a postbox type",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Postbox Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = typeDropdownExpanded,
            onDismissRequest = { typeDropdownExpanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            listOf(
                "Pillar", "K Type Pillar", "C Type Pillar", "Lamp Pedastal",
                "Wall Box", "Wall Box C Type", "Parcel", "Bantam N Type", "M Type"
            ).forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(type)
                    },
                    onClick = {
                        typeDropdownExpanded = false
                        onClick(type)
                    },
                    leadingIcon = { PostboxIcon(modifier = Modifier.size(128.dp), type = type) }
                )
            }
        }
    }
}