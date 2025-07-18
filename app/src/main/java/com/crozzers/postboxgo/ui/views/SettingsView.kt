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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.setSetting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.ui.theme.ColourSchemes
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView() {
    val settings = LocalContext.current.settings

    val selectedColourScheme = settings.data.map { preferences ->
        preferences[Setting.COLOUR_SCHEME] ?: ColourSchemes.Standard.name
    }.collectAsState(initial = ColourSchemes.Standard.name)

    Column(modifier = Modifier.padding(16.dp)) {
        ColourSchemeDropdown(selectedColourScheme, setSetting(settings, Setting.COLOUR_SCHEME))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColourSchemeDropdown(selectedScheme: State<String>, onChange: (s: String) -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedScheme.value,
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
                        onChange(scheme.name)
                    }
                )
            }
        }
    }
}