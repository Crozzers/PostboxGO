package com.crozzers.postboxgo.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.setSetting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.ui.theme.ColourSchemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView() {
    val settings = LocalContext.current.settings

    Column(modifier = Modifier.padding(16.dp)) {
        ColourSchemeDropdown(setSetting(settings, Setting.COLOUR_SCHEME))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColourSchemeDropdown(onChange: (s: String) -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedScheme by remember { mutableStateOf("Light") }

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedScheme,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Colour Scheme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            )
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            ColourSchemes.entries.forEach { scheme ->
                DropdownMenuItem(
                    text = { Text(scheme.name) },
                    onClick = {
                        dropdownExpanded = false
                        selectedScheme = scheme.name
                        onChange(scheme.name)
                    }
                )
            }
        }
    }
}