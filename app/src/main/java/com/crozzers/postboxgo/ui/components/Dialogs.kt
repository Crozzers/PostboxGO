package com.crozzers.postboxgo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Simplified version of [InfoDialog] specialised for yes/no questions
 *
 * @param title The title of the dialog
 * @param body The main text for the dialog
 * @param callback A function that will receive the result of the dialog, either yes (`true`) or no
 *   (`false`)
 */
@Composable
fun ConfirmDialog(
    title: String = "Are you sure?",
    body: String = "This action cannot be undone",
    callback: (state: Boolean) -> Unit
) {
    InfoDialog(
        title, body, Icons.Filled.Warning, "Dismiss", "Confirm"
    ) {
        // do `== true` because a `!= null` type guard doesn't work
            state ->
        callback(state == true)
    }
}

/**
 * A slightly simpler interface for material3's [AlertDialog]
 *
 * @param title The title for the dialog
 * @param body The main text for the dialog
 * @param icon The icon to display
 * @param dismissButtonText The text for the "dismiss" button on the left hand side
 * @param confirmButtonText The text for the "confirm" button on the right hand side
 * @param callback A function that will receive the result of the dialog, either confirm (`true`),
 *   dismiss (`false`) or `null`, which means the user closed the dialog without clicking either
 */
@Composable
fun InfoDialog(
    title: String = "Info",
    body: String = "",
    icon: ImageVector = Icons.Filled.Info,
    dismissButtonText: String? = "Dismiss",
    confirmButtonText: String = "Ok",
    callback: ((state: Boolean?) -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(true) }
    var state: Boolean? by remember { mutableStateOf(null) }

    // we use this contrived approach here to ensure that whatever happens in `callback` does not
    // block the dialog from closing immediately after the user clicks something
    if (showDialog) {
        AlertDialog(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = title
                )
            },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        state = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = if (dismissButtonText != null) ({
                TextButton(
                    onClick = {
                        showDialog = false
                        state = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(dismissButtonText)
                }
            }) else null,
            onDismissRequest = {
                showDialog = false
            },
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
            iconContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }

    LaunchedEffect(showDialog) {
        if (!showDialog) {
            callback?.invoke(state)
        }
    }
}