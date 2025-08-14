package com.fluidpotata.placesapp

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MultipartBody

@Composable
fun ListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var ownedIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Modal state
    var editingPlace by remember { mutableStateOf<Place?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editLat by remember { mutableStateOf("") }
    var editLon by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var modalError by remember { mutableStateOf<String?>(null) }

    val locationClient = LocationServices.getFusedLocationProviderClient(context)

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            selectedImage = bitmap
        }
    }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = locationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                    result?.let {
                        editLat = it.latitude.toString()
                        editLon = it.longitude.toString()
                    } ?: run { modalError = "Unable to fetch location" }
                } catch (e: Exception) {
                    modalError = "Error fetching location: ${e.localizedMessage}"
                }
            }
        } else {
            modalError = "Location permission denied"
        }
    }

    fun loadPlaces() {
        coroutineScope.launch {
            try {
                isLoading = true
                places = ApiClient.api.getPlaces()
                ownedIds = db.placeOwnerDao().getAllOwnedIds()
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
        Text(
            "Places List",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

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
                                    url = "https://labs.anontech.info/cse489/t3/${place.image}",
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

                            if (ownedIds.contains(place.id)) {
                                TextButton(onClick = {
                                    editingPlace = place
                                    editTitle = place.title
                                    editLat = place.lat ?: ""
                                    editLon = place.lon ?: ""
                                    selectedImage = null
                                    modalError = null
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

    // Edit modal
    editingPlace?.let { place ->
        if (showCamera) {
            CameraCaptureScreen(
                onImageCaptured = { bitmap ->
                    selectedImage = bitmap
                    showCamera = false
                },
                onError = { modalError = it.localizedMessage ?: "Camera error"; showCamera = false },
                onCancel = { showCamera = false }
            )
        } else {
            AlertDialog(
                onDismissRequest = { editingPlace = null },
                title = { Text("Edit Place") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editLat,
                            onValueChange = { editLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editLon,
                            onValueChange = { editLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Text("Pick Image")
                            }
                            Button(onClick = { showCamera = true }, modifier = Modifier.weight(1f)) {
                                Text("Take Photo")
                            }
                            Button(onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }, modifier = Modifier.weight(1f)) {
                                Text("Current Location")
                            }
                        }

                        selectedImage?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }
                        if (selectedImage == null && !place.image.isNullOrBlank()) {
                            NetworkImage(
                                url = "https://labs.anontech.info/cse489/t3/${place.image}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }

                        modalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val latDouble = editLat.toDoubleOrNull()
                        val lonDouble = editLon.toDoubleOrNull()
                        if (editTitle.isBlank() || latDouble == null || lonDouble == null) {
                            modalError = "Please fill all fields with valid numbers"
                            return@TextButton
                        }

                        modalError = null
                        isUpdating = true

                        coroutineScope.launch {
                            try {
                                val response: retrofit2.Response<*>
                                if (selectedImage != null) {
                                    val imagePart = bitmapToMultipart(
                                        context,
                                        selectedImage!!,
                                        "image",
                                        "place_${place.id}.jpg"
                                    )
                                    response = ApiClient.api.updatePlaceWithImage(
                                        id = place.id,
                                        title = editTitle,
                                        lat = latDouble,
                                        lon = lonDouble,
                                        image = imagePart
                                    )
                                } else {
                                    response = ApiClient.api.updatePlaceForm(
                                        id = place.id,
                                        title = editTitle,
                                        lat = latDouble,
                                        lon = lonDouble
                                    )
                                }

                                if (response.isSuccessful) {
                                    loadPlaces()
                                    editingPlace = null
                                } else {
                                    modalError = "Failed: ${response.code()} ${response.message()}"
                                }
                            } catch (e: Exception) {
                                modalError = "Error: ${e.localizedMessage}"
                            } finally {
                                isUpdating = false
                            }
                        }
                    }) {
                        Text(if (isUpdating) "Updating..." else "Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPlace = null }) { Text("Cancel") }
                }
            )
        }
    }
}
