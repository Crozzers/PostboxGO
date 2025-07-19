package com.crozzers.postboxgo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    title: String = "Are you sure?",
    body: String = "This action cannot be undone",
    callback: (state: Boolean) -> Unit
) {
    // TODO: fix in light/dark mode
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Are you sure"
            )
        },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = {
                    callback(true)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    callback(false)
                }
            ) {
                Text("Dismiss")
            }
        },
        onDismissRequest = {
            callback(false)
        },
    )
}