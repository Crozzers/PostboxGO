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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.settings
import com.crozzers.postboxgo.ui.components.PostboxIcon
import com.crozzers.postboxgo.utils.humanReadableDate
import com.crozzers.postboxgo.utils.humanReadablePostboxName
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListView(
    postboxes: MutableMap<String, Postbox>,
    onItemClick: (postbox: Postbox) -> Unit
) {
    if (postboxes.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No postboxes registered yet.", textAlign = TextAlign.Center)
            Text(
                "Click the plus icon on the navigation bar to register a new postbox.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        var searchQuery by remember { mutableStateOf("") }

        val exactMatches = mutableListOf<Postbox>()
        val fuzzyMatches = mutableListOf<Postbox>()

        if (searchQuery.isEmpty()) {
            // don't bother doing some matching algorithm if there's no search query. Just show all the postboxes
            exactMatches.addAll(postboxes.values)
        } else {
            for (postbox in postboxes.values) {
                val searchValues = mutableListOf(
                    humanReadablePostboxName(postbox.name),
                    postbox.id,
                    postbox.monarch.displayName,
                    humanReadableDate(postbox.dateRegistered)
                )
                if (postbox.double != null) {
                    searchValues.add(postbox.double)
                }
                if (postbox.type != null) {
                    searchValues.add(postbox.type!!)
                }

                val searchNoSpaces = searchQuery.replace(" ", "")
                var exactMatch = false
                var fuzzyMatch = false
                for (value in searchValues) {
                    val valueNoSpaces = value.replace(" ", "")
                    if (valueNoSpaces.equals(searchNoSpaces, ignoreCase = true)) {
                        // if we find an exact match exit immediately. This is the best match we can find
                        exactMatches.add(postbox)
                        exactMatch = true
                        break
                    }
                    // if not an exact match, check for fuzzy but keep looking for a better match just in case
                    fuzzyMatch =
                        fuzzyMatch || valueNoSpaces.contains(searchNoSpaces, ignoreCase = true)
                }
                // only add to fuzzy match list if exact match not found
                if (!exactMatch && fuzzyMatch) {
                    fuzzyMatches.add(postbox)
                }
            }
        }

        val sortOption = LocalContext.current.settings.data.map { preferences ->
            preferences[Setting.HOMEPAGE_SORT_KEY] ?: SortOption.DATE.name
        }.collectAsState(initial = SortOption.DATE.name)

        val sortDirection = LocalContext.current.settings.data.map { preferences ->
            preferences[Setting.HOMEPAGE_SORT_DIRECTION] ?: SortDirection.DESCENDING.name
        }.collectAsState(initial = SortDirection.DESCENDING.name)

        val comparator = compareBy<Postbox> {
            when (SortOption.valueOf(sortOption.value)) {
                SortOption.NAME -> humanReadablePostboxName(it.name)
                SortOption.ID -> it.id
                SortOption.DATE -> LocalDateTime.parse(it.dateRegistered)
                SortOption.TYPE -> it.type
                SortOption.MONARCH -> it.monarch.displayName
            }
        }

        exactMatches.sortWith(comparator)
        fuzzyMatches.sortWith(comparator)
        if (sortDirection.value == SortDirection.DESCENDING.name) {
            exactMatches.reverse()
            fuzzyMatches.reverse()
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

            if (exactMatches.isEmpty() && fuzzyMatches.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No postboxes found matching your query.", textAlign = TextAlign.Center)
                    Text(
                        "Click the plus icon on the navigation bar to register a new postbox.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp), modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // ensure that exact matches are floated to the top
                    items(exactMatches) { postbox ->
                        PostboxCard(postbox, onItemClick)
                    }
                    items(fuzzyMatches) { postbox ->
                        PostboxCard(postbox, onItemClick)
                    }
                }
            }
        }
    }
}

enum class SortOption(val displayName: String) {
    NAME("Name"),
    ID("ID"),
    DATE("Date"),
    TYPE("Type"),
    MONARCH("Monarch");

    override fun toString(): String {
        return displayName
    }
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
        Row {
            PostboxIcon(Modifier.fillMaxWidth(0.2f), type = postbox.type, postbox.inactive)
            Column(Modifier.padding(start = 0.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
                val id = if ("[a-z0-9-]{32,36}".toRegex().matches(postbox.id)) {
                    // don't show UUIDs in the homepage. They are long and ugly.
                    // V1 savefiles and inactive postboxes use UUIDs
                    ""
                } else if (postbox.double != null) {
                    // if it's a double then show both IDs, and try to show LHS first
                    if (postbox.name.contains("(L)", ignoreCase = true)) {
                        " (${postbox.id}, ${postbox.double})"
                    } else {
                        " (${postbox.double}, ${postbox.id})"
                    }
                } else {
                    // postbox is not double, or inactive so just show it normally
                    " (${postbox.id})"
                }
                Row {
                    Text(
                        text = "${postbox}$id",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2f)
                    )
                    if (!postbox.verified) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Unverified postbox",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                }
                Text(
                    text = postbox.monarch.displayName,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Registered: ${humanReadableDate(postbox.dateRegistered)}",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


