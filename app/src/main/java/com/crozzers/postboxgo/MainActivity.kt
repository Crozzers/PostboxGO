package com.crozzers.postboxgo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crozzers.postboxgo.ui.theme.PostboxGOTheme

class App : Application() {
    lateinit var saveFile: SaveFile
        private set

    override fun onCreate() {
        super.onCreate()
        saveFile = SaveFile(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PostboxGOTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MenuBar(navController)
                    }) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavigationItem.Home.route
                    ) {
                        composable(NavigationItem.Home.route) {
                            PostboxListScreen(
                                (applicationContext as App).saveFile.getPostboxes(),
                                Modifier.padding(innerPadding)
                            )
                        }
                        composable(NavigationItem.AddPostbox.route) {
                            AddPostbox({p ->
                                (applicationContext as App).saveFile.addPostbox(p);
                                navController.navigate(NavigationItem.Home.route)
                            })
                        }
                    }
                }
            }
        }
    }
}

enum class Screen {
    HOME,
    ADDPOSTBOX
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object AddPostbox : NavigationItem(Screen.ADDPOSTBOX.name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBar(navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = "PostboxGo",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = {
                navController.navigate("addPostbox")
            }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Register Postbox"
                )
            }
        }
    )
}