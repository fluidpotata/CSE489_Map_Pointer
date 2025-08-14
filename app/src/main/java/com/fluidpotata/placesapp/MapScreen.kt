package com.fluidpotata.placesapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip




@Composable
fun MapScreen(navController: NavController) {
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                places = ApiClient.api.getPlaces()
            } catch (e: Exception) {
                Log.e("MapScreen", "Error loading places", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val bangladeshLatLng = LatLng(23.6850, 90.3563)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(bangladeshLatLng, 6f)
        }

        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { selectedPlace = null }
        ) {
            places.forEach { place ->
                if (!place.title.isNullOrBlank() && !place.lat.isNullOrBlank() && place.lat != "0.0"
                    && !place.lon.isNullOrBlank() && place.lon != "0.0") {
                    val lat = place.latAsDouble()
                    val lon = place.lonAsDouble()
                    if (lat != null && lon != null) {
                        Marker(
                            state = MarkerState(LatLng(lat, lon)),
                            title = place.title,
                            onClick = {
                                selectedPlace = place
                                true
                            }
                        )
                    }
                }
            }
        }

        selectedPlace?.let { place ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NetworkImage(
                        url = "https://labs.anontech.info/cse489/t3/${place.image}",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val encodedPath = java.net.URLEncoder.encode(place.image ?: "", "UTF-8")
                                val encodedTitle = java.net.URLEncoder.encode(place.title ?: "", "UTF-8")
                                navController.navigate("imageViewer/$encodedPath/$encodedTitle")
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        place.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }


}
