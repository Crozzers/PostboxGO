package com.crozzers.postboxgo.ui.views

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.core.net.toUri
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.setSetting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.ui.theme.ColourSchemes
import com.crozzers.postboxgo.utils.clearPostboxData
import kotlinx.coroutines.flow.map

const val LOG_TAG = "SettingsView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(saveFile: SaveFile) {
    val settings = LocalContext.current.settings

    val selectedColourScheme = settings.data.map { preferences ->
        preferences[Setting.COLOUR_SCHEME] ?: ColourSchemes.Standard.name
    }.collectAsState(initial = ColourSchemes.Standard.name)

    Column(modifier = Modifier.padding(16.dp)) {
        ColourSchemeDropdown(selectedColourScheme, setSetting(settings, Setting.COLOUR_SCHEME))
        Spacer(modifier = Modifier.padding(16.dp))
        SaveFileManagement(saveFile)
        Spacer(modifier = Modifier.padding(16.dp))
        ClearPBCacheButton(modifier = Modifier.padding(16.dp))
        Spacer(modifier = Modifier.padding(16.dp))
        HorizontalDivider(Modifier)
        Spacer(modifier = Modifier.padding(8.dp))
        VersionInfo()
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

@Composable
fun SaveFileManagement(saveFile: SaveFile) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) {
            Log.i(LOG_TAG, "Savefile import cancelled")
        } else {
            saveFile.import(uri)
        }
    }

    Text("Save file options:", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth()) {
        Button(onClick = {
            launcher.launch("application/json")
        }) {
            Text("Import and overwrite")
        }
        Spacer(Modifier.padding(16.dp))
        Button(onClick = { saveFile.export() }) {
            Text("Export")
        }

    }
}

@Composable
fun ClearPBCacheButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(onClick = {
        clearPostboxData(context)
        Toast.makeText(context, "Postbox cache cleared", Toast.LENGTH_SHORT).show()
    }) { Text("Clear nearby postbox cache") }
}

@Composable
fun VersionInfo() {
    val context = LocalContext.current
    val packageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)
    Button(onClick = {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/Crozzers/PostboxGO".toUri()
            )
        )
    }) {
        Text("View source code")
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text("App Version: ${packageInfo.versionName}")
}