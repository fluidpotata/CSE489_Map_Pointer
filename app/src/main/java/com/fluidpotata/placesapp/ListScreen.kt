package com.fluidpotata.placesapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun ListScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadPlaces() {
        coroutineScope.launch {
            try {
                isLoading = true
                places = ApiClient.api.getPlaces()
                errorMsg = null
            } catch (e: Exception) {
                Log.e("ListScreen", "Error fetching places", e)
                errorMsg = "Failed to load places"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPlaces() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Places List", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { navController.navigate(ScreenRoutes.Form) }) {
                Text("Add")
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(places) { place ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!place.image.isNullOrBlank()) {
                                NetworkImage(
                                    url = "http://192.168.1.105:5000/${place.image}",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(place.title, style = MaterialTheme.typography.bodyLarge)
                                Text("Lat: ${place.lat}, Lon: ${place.lon}")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                navController.navigate("edit/${place.id}")
                            }) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }
    }
}
