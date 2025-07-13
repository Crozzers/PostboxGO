package com.crozzers.postboxgo.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crozzers.postboxgo.Postbox
import com.crozzers.postboxgo.SaveFile

@Composable
fun ListView(
    postboxes: List<Postbox>,
    onItemClick: (postbox: Postbox) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(postboxes) { postbox ->
            PostboxCard(postbox, onItemClick)
        }
    }
}

@Composable
fun PostboxCard(postbox: Postbox, onClick: (postbox: Postbox) -> Unit) {
    Card(
        Modifier
            .height(120.dp)
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(postbox) }
    ) {
        Column {
            Text(text = "Name: ${postbox.name}")
            Text(text = "Type: ${postbox.type}")
            Text(text = "Monarch: ${postbox.monarch}")
            Text(text = "Registered: ${postbox.dateRegistered}")
        }
    }
}

