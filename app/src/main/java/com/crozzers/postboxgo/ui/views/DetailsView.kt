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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.ui.components.ConfirmDialog
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxAgeEstimate
import com.crozzers.postboxgo.utils.humanReadablePostboxName

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
    Text(
        text = humanReadablePostboxName(postbox.name),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Left
    )
    Row {
        Column {
            Text(
                text = "Registered: ${humanReadableDate(postbox.dateRegistered)}",
                fontSize = 12.sp
            )
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
            if (postbox.double != null) {
                var l: String?
                var r: String?
                if (postbox.name.contains("(l)", ignoreCase = true)) {
                    l = postbox.id
                    r = postbox.double
                } else {
                    l = postbox.double
                    r = postbox.id
                }
                Text("ID: $l (L), $r (R)")
            } else {
                Text(text = "ID: ${postbox.id}")
            }
            Text(text = "Type: ${postbox.type ?: "Unknown"}")
            Text(text = "Monarch: ${postbox.monarch.displayName}")
            Text("Age estimate: ${humanReadablePostboxAgeEstimate(postbox.getAgeEstimate())}")
        }
    }
}


@Composable
fun ActionButtons(
    coords: Pair<Float, Float>,
    deleteCallback: (s: Boolean) -> Unit
) {
    var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }

    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

    val context = LocalContext.current
    val openConfirmDeleteDialog = remember { mutableStateOf(false) }

    Spacer(Modifier.height(8.dp))
    val content = @Composable {
        Button(
            {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://maps.google.com/maps/dir//${coords.first},${coords.second}".toUri()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(if (orientation == Configuration.ORIENTATION_PORTRAIT) 1f else 0.5f)
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

    when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            content()
        }

        else -> {
            Row {
                content()
            }
        }
    }
}

