package com.crozzers.postboxgo.ui.views

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import com.crozzers.postboxgo.DetailedPostboxInfo
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.checkAndRequestLocation
import com.crozzers.postboxgo.utils.getLocation
import com.crozzers.postboxgo.utils.getNearbyPostboxes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GroundOverlay
import com.google.maps.android.compose.GroundOverlayPosition
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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
                    if (postboxes != null) {
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
        zoom = 15f,
        centreOnLocation = true,
        marker = { p -> NearbyPostboxMarker(p) }
    )
}


@Composable
fun NearbyPostboxMarker(postbox: Postbox) {
    val pos = LatLng(
        postbox.coords.first.toDouble(),
        postbox.coords.second.toDouble()
    )
    // lon/lat is returned as 14dp, which is beyond centimetre precision, so it's essentially
    // random, but reproducible for each individual postbox.
    // take the last 3 digits to calculate the offset (0f - 1f)
    var x = pos.latitude.toBigDecimal().toPlainString().takeLast(3).replace('.', '0')
        .toFloat() / 1000
    var y = pos.longitude.toBigDecimal().toPlainString().takeLast(3).replace('.', '0')
        .toFloat() / 1000

    // offset 0, 0 is top left, so flip the y to make maths easier
    y = 1f - y
    val clamped = clampPositionWithinCircleOffset(x, y)
    x = clamped.first
    // un-flip the y now that maths is done
    y = 1f - clamped.second

    GroundOverlay(
        position = GroundOverlayPosition.create(pos, 250f),
        image = BitmapDescriptorFactory.fromBitmap(RedCircle()),
        transparency = 0.5f,
        anchor = Offset(x, y)
    )
}

@Composable
fun RedCircle(): Bitmap {
    val bitmap = createBitmap(196, 196)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = MaterialTheme.colorScheme.surface.toArgb()
    }
    canvas.drawCircle(98f, 98f, 98f, paint)
    return bitmap
}

/**
 * Ensures some coordinates are within a circle inside an offset grid for a map overlay
 */
fun clampPositionWithinCircleOffset(x: Float, y: Float): Pair<Float, Float> {
    // circle center coords within the offset grid
    val cx = 0.5f
    val cy = 0.5f
    val radius = 0.45f  // actual rad is 0.5f but shrink a little to give us margin

    val m = if (x == cx) {
        // avoid zero div error. m is 1 if Y is above circle center, -1 if not
        if (y < cy) {
            -1f
        } else {
            1f
        }
    } else if (y == cy) {
        // if y is at circle center then gradient is always 0
        0f
    } else {
        // else gradient relative to circle center (0.5, 0.5)
        (cy - y) / (cx - x)
    }

    // calculate point on circles circumference using parametric eq
    val a = atan(m)
    val flippedA = (a + Math.PI).toFloat()
    val edgeX1 = cx + (radius * cos(a))
    val edgeY1 = cy + (radius * sin(a))
    // also calculate point on other side of circle by flipping angle 180deg (equivalent)
    val edgeX2 = cx + (radius * cos(flippedA))
    val edgeY2 = cy + (radius * sin(flippedA))

    // ensure coords between circle borders
    return Pair(
        x.coerceIn(
            min(edgeX1, edgeX2),
            max(edgeX1, edgeX2)
        ), y.coerceIn(
            min(edgeY1, edgeY2),
            max(edgeY1, edgeY2)
        )
    )
}