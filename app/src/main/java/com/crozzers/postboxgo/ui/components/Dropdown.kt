package com.crozzers.postboxgo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Simple widget for creating a dropdown menu
 *
 * @param label The label for the dropdown menu
 * @param items The items to select from
 * @param selectedItem The currently selected item
 * @param loadingMessage The message to display as a placeholder when the items list is empty
 * @param onExpand A callback for when the user opens the dropdown. Useful for lazy loading the
 *  items list
 * @param nullOption Whether to include an option that allows the user to "select nothing"
 * @param selectionCallback A callback for when the user selects an item. May be null if the
 *  user selects [nullOption]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownMenu(
    label: String,
    items: List<T>,
    selectedItem: T?,
    loadingMessage: String = "Loading...",
    onExpand: ((expanded: Boolean) -> Unit)? = null,
    nullOption: Boolean = false,
    selectionCallback: ((T?) -> Unit)
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            onExpand?.invoke(expanded)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem?.toString() ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (nullOption) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        expanded = false
                        selectionCallback(null)
                    }
                )
            }
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.toString())
                    },
                    onClick = {
                        expanded = false
                        selectionCallback(item)
                    }
                )
            }
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(loadingMessage) },
                    onClick = { },
                    enabled = false
                )
            }
        }
    }
}