package com.crozzers.postboxgo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.crozzers.postboxgo.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showEdit =
        navBackStackEntry?.destination?.route?.startsWith(Routes.ViewPostbox.route) == true

    val title = when (navBackStackEntry?.destination?.route?.split("/")[0]) {
        Routes.AddPostbox.route -> Routes.AddPostbox.displayName
        Routes.EditPostbox.route -> Routes.EditPostbox.displayName
        Routes.Settings.route -> Routes.Settings.displayName
        else -> "PostboxGO"
    }

    TopAppBar(
        title = {
            Text(
                text = title, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }, actions = {
            if (showEdit) {
                val postboxId = navBackStackEntry!!.arguments?.getString("id")
                if (postboxId != null) {
                    IconButton(onClick = {
                        navController.navigate("${Routes.EditPostbox.route}/$postboxId")
                    }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                    }
                }
            }
            // settings first because it's harder to reach and lesser used
            IconButton(onClick = {
                navController.navigate(Routes.Settings.route)
            }) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
            // homepage next as it's less used than adding
            IconButton(onClick = { navController.navigate(Routes.ListView.route) }) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
            }
            // most used, whack it in thumb's reach in the top right
            IconButton(onClick = {
                navController.navigate(Routes.AddPostbox.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Add, contentDescription = "Register Postbox"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(navController: NavController, visible: Boolean, isListView: Boolean) {
    if (!visible) {
        return
    }

    NavigationBar {
        NavigationBarItem(selected = isListView, onClick = {
            navController.navigate(Routes.ListView.route)
        }, icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "List View"
            )
        }, label = { Text("List View") })
        NavigationBarItem(selected = !isListView, onClick = {
            navController.navigate(Routes.MapView.route)
        }, icon = {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Map View"
            )
        }, label = { Text("Map View") }
        )
    }
}