package com.crozzers.postboxgo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.crozzers.postboxgo.NavigationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController) {
    TopAppBar(title = {
        Text(
            text = "PostboxGO", maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }, actions = {
        IconButton(onClick = { navController.navigate(NavigationItem.ListView.route) }) {
            Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
        }
        IconButton(onClick = {
            navController.navigate(NavigationItem.AddPostbox.route)
        }) {
            Icon(
                imageVector = Icons.Filled.Add, contentDescription = "Register Postbox"
            )
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(navController: NavController, visible: Boolean, isListView: Boolean) {
    if (!visible) {
        return
    }

    NavigationBar {
        NavigationBarItem(selected = isListView, onClick = {
            navController.navigate(NavigationItem.ListView.route)
        }, icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "List View"
            )
        }, label = { Text("List View") })
        NavigationBarItem(selected = !isListView, onClick = {
            navController.navigate(NavigationItem.MapView.route)
        }, icon = {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Map View"
            )
        }, label = { Text("Map View") })
    }
}