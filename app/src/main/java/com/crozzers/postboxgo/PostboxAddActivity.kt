package com.crozzers.postboxgo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostbox(callback: (p: Postbox) -> Unit) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var selectedMonarch by remember { mutableStateOf(Monarch.NONE) }
    var monarchDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Add New Postbox", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Latitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Longitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = monarchDropdownExpanded,
            onExpandedChange = { monarchDropdownExpanded = !monarchDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedMonarch.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(text = "Monarch") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monarchDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = monarchDropdownExpanded,
                onDismissRequest = { monarchDropdownExpanded = false }
            ) {
                Monarch.entries.forEach { monarch ->
                    DropdownMenuItem(
                        text = { Text(monarch.name) },
                        onClick = {
                            selectedMonarch = monarch
                            monarchDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = null
                var coords: Pair<Float, Float>
                try {
                    coords = Pair(latitude.toFloat(), longitude.toFloat())
                } catch (e: NumberFormatException) {
                    errorMessage = "Error: Latitude and Longitude must be numbers."
                    return@Button
                }
                callback(Postbox(coords, selectedMonarch, LocalDateTime.now().toString()))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Postbox")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
