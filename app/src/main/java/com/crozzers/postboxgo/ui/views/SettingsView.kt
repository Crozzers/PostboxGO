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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.crozzers.postboxgo.R
import com.crozzers.postboxgo.SaveFile
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.setSetting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.ui.components.DropdownMenu
import com.crozzers.postboxgo.ui.theme.ColourSchemes
import com.crozzers.postboxgo.utils.ReleaseTrack
import com.crozzers.postboxgo.utils.clearPostboxData
import com.crozzers.postboxgo.utils.isManuallyInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val LOG_TAG = "SettingsView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(saveFile: SaveFile) {
    val settings = LocalContext.current.settings

    val selectedColourScheme = settings.data.map { preferences ->
        preferences[Setting.COLOUR_SCHEME] ?: ColourSchemes.Standard.name
    }.collectAsState(initial = ColourSchemes.Standard.name)

    val selectedSortOption = settings.data.map { preferences ->
        preferences[Setting.HOMEPAGE_SORT_KEY] ?: SortOption.DATE.name
    }.collectAsState(initial = SortOption.DATE.name)

    val selectedSortDirection = settings.data.map { preferences ->
        preferences[Setting.HOMEPAGE_SORT_DIRECTION] ?: SortDirection.DESCENDING.name
    }.collectAsState(initial = SortDirection.DESCENDING.name)

    val checkForUpdates = settings.data.map { preferences ->
        preferences[Setting.CHECK_FOR_UPDATES] ?: true
    }.collectAsState(initial = true)

    val selectedReleaseTrack = settings.data.map { preferences ->
        preferences[Setting.RELEASE_TRACK] ?: ReleaseTrack.STABLE.name
    }.collectAsState(initial = ReleaseTrack.STABLE.name)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ColourSchemeDropdown(selectedColourScheme, setSetting(settings, Setting.COLOUR_SCHEME))
        Spacer(modifier = Modifier.padding(16.dp))
        HorizontalDivider(Modifier)
        Spacer(modifier = Modifier.padding(8.dp))
        HomepageSortOption(selectedSortOption, selectedSortDirection) { s, d ->
            setSetting(settings, Setting.HOMEPAGE_SORT_KEY)(s)
            setSetting(settings, Setting.HOMEPAGE_SORT_DIRECTION)(d)
        }
        Spacer(modifier = Modifier.padding(16.dp))
        HorizontalDivider(Modifier)
        Spacer(modifier = Modifier.padding(8.dp))
        SaveFileManagement(saveFile)
        Spacer(modifier = Modifier.padding(16.dp))
        ClearPBCacheButton()
        if (isManuallyInstalled(LocalContext.current)) {
            Spacer(modifier = Modifier.padding(16.dp))
            HorizontalDivider(Modifier)
            Spacer(modifier = Modifier.padding(8.dp))
            UpdateManagement(checkForUpdates, selectedReleaseTrack) { c, s ->
                setSetting(settings, Setting.CHECK_FOR_UPDATES)(c)
                setSetting(settings, Setting.RELEASE_TRACK)(s)
            }
        }
        Spacer(modifier = Modifier.padding(16.dp))
        HorizontalDivider(Modifier)
        Spacer(modifier = Modifier.padding(8.dp))
        Spacer(modifier = Modifier.padding(8.dp))
        VersionInfo()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColourSchemeDropdown(selectedScheme: State<String>, onChange: (s: String) -> Unit) {
    DropdownMenu(
        "Colour Scheme",
        ColourSchemes.entries.map { s -> s.name },
        selectedScheme.value
    ) {
        if (it != null) {
            onChange(it)
        }
    }
}

@Composable
fun HomepageSortOption(
    selectedSortOption: State<String>,
    selectedSortDirection: State<String>,
    onChange: (s: String, d: String) -> Unit
) {
    Column {
        Text("Homepage sort options:", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.padding(4.dp))

        DropdownMenu(
            "Sort Key",
            SortOption.entries,
            SortOption.valueOf(selectedSortOption.value)
        ) {
            if (it != null) {
                onChange(it.name, selectedSortDirection.value)
            }
        }

        Spacer(Modifier.padding(4.dp))

        Text("Sort direction:", style = MaterialTheme.typography.titleSmall)
        SortDirection.entries.forEach { direction ->
            Row(
                Modifier
                    .selectable(
                        selected = (direction.name == selectedSortDirection.value),
                        onClick = {
                            onChange(selectedSortOption.value, direction.name)
                        }, role = Role.RadioButton
                    )
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                RadioButton(
                    selected = (direction.name == selectedSortDirection.value),
                    onClick = null, // recommended by google for accessibility reasons
                    // https://developer.android.com/develop/ui/compose/components/radio-button#key-points
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onPrimary)
                )
                Text(
                    text = direction.displayName,
                    modifier = Modifier.padding(start = 16.dp)
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

    Text("Save file options:", style = MaterialTheme.typography.titleLarge)
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
fun ClearPBCacheButton() {
    val context = LocalContext.current
    Button(onClick = {
        CoroutineScope(Dispatchers.IO).launch {
            clearPostboxData(context)
        }
        Toast.makeText(context, "Postbox cache cleared", Toast.LENGTH_SHORT).show()
    }) { Text("Clear nearby postbox cache") }
}

@Composable
fun UpdateManagement(
    checkForUpdates: State<Boolean>, selectedReleaseTrack: State<String>,
    onChange: (Boolean, String) -> Unit
) {
    Text("Update options:", style = MaterialTheme.typography.titleLarge)
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checkForUpdates.value, onValueChange = {
                onChange(it, selectedReleaseTrack.value)
            }, role = Role.Checkbox)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checkForUpdates.value, onCheckedChange = null)
        Text(
            "Check for app updates on startup",
            modifier = Modifier.padding(start = 16.dp)
        )
    }

    DropdownMenu(
        "Release track",
        ReleaseTrack.entries,
        ReleaseTrack.valueOf(selectedReleaseTrack.value)
    ) {
        if (it != null) {
            onChange(checkForUpdates.value, it.name)
        }
    }
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
                "https://github.com/Crozzers/PostboxGO/blob/main/docs/usage.md".toUri()
            )
        )
    }) {
        Text("View usage instructions")
        Icon(
            painter = painterResource(id = R.drawable.open_in_new_window),
            contentDescription = "View usage instructions in new window",
            modifier = Modifier.padding(start = 2.dp)
        )
    }
    Button(onClick = {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/Crozzers/PostboxGO".toUri()
            )
        )
    }) {
        Text("View source code")
        Icon(
            painter = painterResource(id = R.drawable.open_in_new_window),
            contentDescription = "View source code in new window",
            modifier = Modifier.padding(start = 2.dp)
        )
    }
    Button(onClick = {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/Crozzers/PostboxGO/blob/main/privacy-notice.md".toUri()
            )
        )
    }) {
        Text("View privacy policy")
        Icon(
            painter = painterResource(id = R.drawable.open_in_new_window),
            contentDescription = "View privacy policy in new window",
            modifier = Modifier.padding(start = 2.dp)
        )
    }
    Button(onClick = {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/Crozzers/PostboxGO/issues/new".toUri()
            )
        )
    }) {
        Text("Report issue via Github")
        Icon(
            painter = painterResource(id = R.drawable.open_in_new_window),
            contentDescription = "Report an issue via Github",
            modifier = Modifier.padding(start = 2.dp)
        )
    }
    Button(onClick = {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "mailto:captaincrozzers@gmail.com".toUri()
            )
        )
    }) {
        Icon(
            Icons.Outlined.Email,
            contentDescription = "Report issue via email",
            modifier = Modifier.padding(end = 2.dp)
        )
        Text("Report issue via Email")
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text("App Version: ${packageInfo.versionName}")
}