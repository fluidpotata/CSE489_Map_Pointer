package com.fluidpotata.placesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fluidpotata.placesapp.ui.theme.PlacesAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlacesAppTheme {
                MyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent { route ->
                scope.launch {
                    navController.navigate(route) {
                        popUpTo(ScreenRoutes.Map) { inclusive = false }
                        launchSingleTop = true
                    }
                    drawerState.close()
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            topBar = {
                TopAppBar(
                    title = { Text("Map Mania") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ScreenRoutes.Map,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ScreenRoutes.Map) {
                    MapScreen(navController)
                }
                composable(ScreenRoutes.Form) {
                    FormScreen(navController, null)
                }
                composable(
                    "edit/{placeId}",
                    arguments = listOf(navArgument("placeId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val placeId = backStackEntry.arguments?.getInt("placeId")
                    FormScreen(navController, placeId)
                }
                composable(ScreenRoutes.List) {
                    ListScreen(navController)
                }

                composable(
                    "imageViewer/{imageUrl}/{title}",
                    arguments = listOf(
                        navArgument("imageUrl") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val decodedImagePath = java.net.URLDecoder.decode(
                        backStackEntry.arguments?.getString("imageUrl") ?: "",
                        "UTF-8"
                    )
                    val decodedTitle = java.net.URLDecoder.decode(
                        backStackEntry.arguments?.getString("title") ?: "",
                        "UTF-8"
                    )
                    ImageViewerScreen(decodedImagePath, decodedTitle)
                }


            }
        }
    }
}

@Composable
fun DrawerContent(onItemClicked: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Text("Navigation", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerItem("Home", onClick = { onItemClicked(ScreenRoutes.Map) })
        DrawerItem("Form", onClick = { onItemClicked(ScreenRoutes.Form) })
        DrawerItem("List", onClick = { onItemClicked(ScreenRoutes.List) })
    }
}

@Composable
fun DrawerItem(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = label)
    }
}


