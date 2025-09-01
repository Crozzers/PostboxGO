package com.crozzers.postboxgo.ui.views

import android.Manifest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import com.google.android.gms.location.FusedLocationProviderClient
import kotlin.uuid.ExperimentalUuidApi

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun EditPostbox(
    locationClient: FusedLocationProviderClient,
    postbox: Postbox,
    callback: (p: Postbox) -> Unit
) {
    var selectedMonarch by remember { mutableStateOf(postbox.monarch) }

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
        var title = humanReadablePostboxName(postbox.name)
        if (!postbox.verified) {
            title += " (unverified)"
        }
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                PostboxDetailsBrief(postbox)
                PostboxMap(
                    postbox,
                    Modifier
                        .fillMaxWidth()
                        .height((screenHeight * 0.35).dp),
                    locationClient = locationClient
                )
                Spacer(modifier = Modifier.height(16.dp))
                SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
            }

            else -> {
                Row(Modifier.padding(8.dp)) {
                    Column(Modifier.fillMaxWidth(0.5f)) {
                        PostboxDetailsBrief(postbox)
                        SelectMonarch(selectedMonarch) { m -> selectedMonarch = m }
                    }
                    Spacer(Modifier.padding(8.dp))
                    PostboxMap(
                        postbox,
                        Modifier
                            .fillMaxWidth()
                            .height((screenHeight * 0.4).dp),
                        locationClient = locationClient
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                postbox.monarch = selectedMonarch
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