package com.fluidpotata.placesapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch


@Composable
fun MapScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                places = ApiClient.api.getPlaces()
            } catch (e: Exception) {
                Log.e("MapScreen", "Error fetching places", e)
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(23.6850, 90.3563),
            8f
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            places.forEach { place ->
                Marker(
                    state = MarkerState(
                        position = LatLng(place.lat, place.lon)
                    ),
                    title = place.title,
                    onClick = {
                        if (!place.image.isNullOrBlank()) {
                            selectedImageUrl =
                                "http://192.168.1.105:5000/${place.image}"
                        }
                        false
                    }
                )
            }
        }

        if (selectedImageUrl != null) {
            AlertDialog(
                onDismissRequest = { selectedImageUrl = null },
                confirmButton = {
                    TextButton(onClick = { selectedImageUrl = null }) {
                        Text("Close")
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NetworkImage(
                            url = selectedImageUrl!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            )
        }
    }
}
