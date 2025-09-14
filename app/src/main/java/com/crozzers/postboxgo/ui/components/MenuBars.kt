package com.crozzers.postboxgo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowHeightSizeClass
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

    val compact =
        currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT

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
        },
        // just shrink it slightly for smaller screens, as it's mostly useless real estate
        expandedHeight = if (compact) TopAppBarDefaults.TopAppBarExpandedHeight - 12.dp else TopAppBarDefaults.TopAppBarExpandedHeight
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(navController: NavController) {
    var currentRoute by remember {
        mutableStateOf(
            navController.currentDestination?.route ?: Routes.ListView.route
        )
    }
    navController.addOnDestinationChangedListener { _, destination, _ ->
        currentRoute = destination.route ?: Routes.ListView.route
    }

    NavigationBar {
        for (route in Routes.entries) {
            if (route == Routes.ViewPostbox || route == Routes.EditPostbox) {
                continue
            }
            NavigationBarItem(
                route.route == currentRoute,
                {
                    navController.navigate(route.route)
                },
                {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.displayName
                    )
                },
                label = { Text(route.displayName) }
            )
        }
    }
}

@Composable
fun NavRail(navController: NavController) {
    val navEntry = navController.currentBackStackEntryAsState()
    var currentRoute by remember {
        mutableStateOf(
            navEntry.value?.destination?.route ?: Routes.ListView.route
        )
    }
    navController.addOnDestinationChangedListener { _, destination, _ ->
        currentRoute = destination.route ?: Routes.ListView.route
    }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        for (route in Routes.entries) {
            if (route == Routes.ViewPostbox || route == Routes.EditPostbox) {
                continue
            }

            NavigationRailItem(
                route.route == currentRoute,
                {
                    navController.navigate(route.route)
                },
                {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.displayName
                    )
                },
                label = { Text(route.displayName) }
            )
        }
    }
}