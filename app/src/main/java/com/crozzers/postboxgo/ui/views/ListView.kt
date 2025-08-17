package com.crozzers.postboxgo.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadableMonarch
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListView(
    postboxes: MutableMap<String, Postbox>,
    onItemClick: (postbox: Postbox) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val sortOption = LocalContext.current.settings.data.map { preferences ->
        preferences[Setting.HOMEPAGE_SORT_KEY] ?: SortOption.DATE.name
    }.collectAsState(initial = SortOption.DATE.name)

    val sortDirection = LocalContext.current.settings.data.map { preferences ->
        preferences[Setting.HOMEPAGE_SORT_DIRECTION] ?: 1
    }.collectAsState(initial = 1)

    val filteredAndSortedPostboxes = postboxes.values.filter {
        humanReadablePostboxName(it.name).contains(searchQuery, ignoreCase = true) ||
                it.type?.contains(searchQuery, ignoreCase = true) == true ||
                humanReadableMonarch(it.monarch).contains(searchQuery, ignoreCase = true) ||
                humanReadableDate(it.dateRegistered).contains(searchQuery, ignoreCase = true)

    }.sortedWith(
        compareBy {
            when (SortOption.valueOf(sortOption.value)) {
                SortOption.NAME -> humanReadablePostboxName(it.name)
                SortOption.DATE -> LocalDateTime.parse(it.dateRegistered)
                SortOption.TYPE -> it.type
                SortOption.MONARCH -> humanReadableMonarch(it.monarch)
            }
        }
    ).let {
        if (sortDirection.value == 1) {
            it.reversed()
        } else {
            it
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search postboxes") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        searchQuery = ""
                    }) { Icon(Icons.Default.Clear, contentDescription = "Clear search") }
                }
            )
        }
        // TODO: enable grid view in landscape
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(filteredAndSortedPostboxes) { postbox ->
                PostboxCard(postbox, onItemClick)
            }
        }
    }
}

enum class SortOption(val displayName: String) {
    NAME("Name"),
    DATE("Date"),
    TYPE("Type"),
    MONARCH("Monarch"),
}

enum class SortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

@Composable
fun PostboxCard(postbox: Postbox, onClick: (postbox: Postbox) -> Unit) {
    Card(
        Modifier
            .height(130.dp)
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(postbox) }
            .border(5.dp, MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = "Name: ${humanReadablePostboxName(postbox.name)}",
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(text = "Type: ${postbox.type}", color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = "Monarch: ${humanReadableMonarch(postbox.monarch)}",
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "Registered: ${humanReadableDate(postbox.dateRegistered)}",
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

