package com.crozzers.postboxgo.ui.views

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.ConfirmDialog
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxAgeEstimate

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
                Spacer(Modifier.size(8.dp))
                PostboxMap(postbox, Modifier.fillMaxHeight(0.7f))
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
    Row {
        Column {
            if (!postbox.verified) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = buildAnnotatedString {
                            appendInlineContent("warning", "[icon]")
                            append("This postbox is unverified. Verify that you've visited it in person in the edit screen when nearby")
                        },
                        inlineContent = mapOf(
                            Pair(
                                "warning", InlineTextContent(
                                    Placeholder(
                                        20.sp, 20.sp,
                                        PlaceholderVerticalAlign.Center
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Unverified postbox",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            )
                        )
                    )
                }
            }
            val idString = if (postbox.double != null) {
                var l: String?
                var r: String?
                if (postbox.name.contains("(l)", ignoreCase = true)) {
                    l = postbox.id
                    r = postbox.double
                } else {
                    l = postbox.double
                    r = postbox.id
                }
                "$l (L), $r (R)"
            } else {
                postbox.id
            }
            DetailRow("ID", idString)
            DetailRow("Registered", humanReadableDate(postbox.dateRegistered))
            DetailRow("Type", postbox.type ?: "Unknown")
            DetailRow("Age Estimate", humanReadablePostboxAgeEstimate(postbox.getAgeEstimate()))
            DetailRow("Monarch", postbox.monarch.displayName, postbox.monarch.icon)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .weight(0.4f)
                .padding(8.dp)
        ) {

            Text(
                label,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .drawBehind {
                    drawLine(
                        Color.DarkGray,
                        Offset(-16f, -(8.dp.toPx())),
                        Offset(-16f, size.height + 8.dp.toPx()),
                        strokeWidth = 2f
                    )
                },
            horizontalArrangement = Arrangement.Start
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = label,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(48.dp),
                    tint = Color.Unspecified,
                )
            }
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ActionButtons(
    coords: Pair<Float, Float>,
    deleteCallback: (s: Boolean) -> Unit
) {
    val context = LocalContext.current
    val openConfirmDeleteDialog = remember { mutableStateOf(false) }

    Spacer(Modifier.height(8.dp))
    Row {
        Button(
            {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://maps.google.com/maps/dir//${coords.first},${coords.second}".toUri()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Row {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Get Directions")
                Text(text = "Get Directions")
            }
        }
        Spacer(
            Modifier
                .width(4.dp)
                .height(16.dp)
        )
        OutlinedButton(
            {
                openConfirmDeleteDialog.value = true
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Row {
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
                ConfirmDialog(callback = { state ->
                    openConfirmDeleteDialog.value = false
                    deleteCallback(state)
                })
            }
        }
    }
}

