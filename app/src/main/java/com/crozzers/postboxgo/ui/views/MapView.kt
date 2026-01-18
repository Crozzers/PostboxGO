package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.checkAndRequestLocation
import com.crozzers.postboxgo.utils.getLocation
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.google.android.gms.location.FusedLocationProviderClient

@Composable
@RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
fun MapView(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile,
    callback: (p: Postbox) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Registered Postboxes", "Undiscovered Postboxes")
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
                                    + " Please enable location access to view nearby unregistered postboxes."
                                    + " You can view our privacy policy for details on how this information is used"
                            )
                ) {
                    tabIndex = index
                }
                Tab(
                    text = { Text(title) }, selected = tabIndex == index,
                    onClick = {
                        if (index == 1) {
                            locationCheckCallback()
                        } else {
                            tabIndex = index
                        }
                    },
                )
            }
        }


        when (tabIndex) {
            0 ->
                PostboxMap(
                    saveFile.getPostboxes().values,
                    Modifier.fillMaxSize(),
                    onPostboxClick = callback,
                    locationClient = locationClient
                )

            1 -> NearbyPostboxesMap(locationClient, saveFile)
        }
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun NearbyPostboxesMap(
    locationClient: FusedLocationProviderClient,
    saveFile: SaveFile
) {
    val nearbyPostboxes = remember { mutableStateListOf<DetailedPostboxInfo>() }
    val hasFetched = remember { mutableStateOf(false) }
    val context = LocalContext.current
    if (nearbyPostboxes.isEmpty() && !hasFetched.value) {
        getLocation(locationClient) { location ->
            if (location == null) {
                Toast.makeText(context, "Failed to determine current location", Toast.LENGTH_SHORT)
                    .show()
            } else {
                getNearbyPostboxes(context, location) { postboxes ->
                    hasFetched.value = true
                    if (postboxes == null) {
                        Toast.makeText(
                            context,
                            "Failed to get nearby postboxes",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        nearbyPostboxes.clear()
                        nearbyPostboxes.addAll(postboxes.filter { pb ->
                            val id = "${pb.officeDetails.postcode} ${pb.officeDetails.address1}"
                            saveFile.getPostbox(id) == null
                        })
                    }
                }
            }
        }
    }
    PostboxMap(
        postboxes = nearbyPostboxes.map { Postbox.fromDetailedPostboxInfo(it) },
        modifier = Modifier.fillMaxSize(),
        locationClient = locationClient,
        zoom = 15f
    )
}