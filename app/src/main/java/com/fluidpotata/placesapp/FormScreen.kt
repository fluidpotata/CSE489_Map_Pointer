package com.fluidpotata.placesapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun NetworkImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadingError by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        loadingError = false
        bitmap = null
        try {
            val bmp = withContext(Dispatchers.IO) {
                val input = URL(url).openStream()
                BitmapFactory.decodeStream(input)
            }
            bitmap = bmp
        } catch (e: Exception) {
            loadingError = true
            e.printStackTrace()
        }
    }

    when {
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        loadingError -> Box(
            modifier = modifier.background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Text("Failed to load image", color = Color.White)
        }
        else -> Box(
            modifier = modifier.background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = Color.White)
        }
    }
}

@Composable
fun FormScreen(navController: NavController, placeId: Int? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            selectedImage = bitmap
            existingImageUrl = null
        }
    }

    LaunchedEffect(placeId) {
        placeId?.let {
            try {
                isLoading = true
                val places = ApiClient.api.getPlaces()
                val place = places.find { p -> p.id == it }
                place?.let {
                    title = it.title
                    lat = it.lat.toString()
                    lon = it.lon.toString()
                    existingImageUrl = it.image?.let { imgPath -> "http://192.168.1.105:5000/$imgPath" }
                }
            } catch (e: Exception) {
                errorMsg = "Failed to load place data: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lat,
            onValueChange = { lat = it },
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lon,
            onValueChange = { lon = it },
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Pick Image")
        }

        when {
            selectedImage != null -> {
                Image(
                    bitmap = selectedImage!!.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            existingImageUrl != null -> {
                NetworkImage(
                    url = existingImageUrl!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        if (errorMsg != null) {
            Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (title.isBlank() || lat.isBlank() || lon.isBlank()) {
                    errorMsg = "Please fill all fields"
                    return@Button
                }

                val latDouble = lat.toDoubleOrNull()
                val lonDouble = lon.toDoubleOrNull()
                if (latDouble == null || lonDouble == null) {
                    errorMsg = "Latitude and Longitude must be valid numbers"
                    return@Button
                }

                errorMsg = null
                isLoading = true

                coroutineScope.launch {
                    try {
                        val response = if (placeId == null) {
                            // Create with image (if any)
                            ApiClient.api.createPlace(
                                title.toPlainRequestBody(),
                                latDouble.toString().toPlainRequestBody(),
                                lonDouble.toString().toPlainRequestBody(),
                                selectedImage?.let { bitmapToMultipart(context, it, "image", "upload.jpg") }
                            )
                        } else {
                            // Update without image (PUT x-www-form-urlencoded)
                            ApiClient.api.updatePlace(
                                placeId,
                                title,
                                latDouble,
                                lonDouble
                            )
                        }
                        if (response.isSuccessful) {
                            navController.navigate(ScreenRoutes.List) {
                                popUpTo(ScreenRoutes.List) { inclusive = true }
                            }
                        } else {
                            errorMsg = "Failed to save place: ${response.code()} ${response.message()}"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Error saving place: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Saving..." else "Save Place")
        }
    }
}
