package com.crozzers.postboxgo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBar(navController: NavController) {
    TopAppBar(title = {
        Text(
            text = "PostboxGO", maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }, actions = {
        IconButton(onClick = {
            navController.navigate("addPostbox")
        }) {
            Icon(
                imageVector = Icons.Filled.Add, contentDescription = "Register Postbox"
            )
        }
    })
}