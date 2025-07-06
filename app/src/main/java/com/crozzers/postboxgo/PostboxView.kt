package com.crozzers.postboxgo

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

@Composable
fun PostboxListScreen(postboxes: List<Postbox>, modifier: Modifier) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(postboxes) { postbox ->
            PostboxCard(postbox)
        }
    }
}

@Composable
fun PostboxCard(postbox: Postbox) {
    Card(
        Modifier
            .height(100.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(text = "Name: ${postbox.name}")
            Text(text = "Type: ${postbox.type}")
            Text(text = "Monarch: ${postbox.monarch}")
            Text(text = "Registered: ${postbox.dateRegistered}")
        }
    }
}