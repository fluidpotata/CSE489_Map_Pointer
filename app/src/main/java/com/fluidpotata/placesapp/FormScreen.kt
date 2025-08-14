package com.fluidpotata.placesapp

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import com.fluidpotata.placesapp.toPlainRequestBody


@Composable
fun FormScreen(navController: NavController, placeId: Int? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val db = remember { AppDatabase.getDatabase(context) }

    var title by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            selectedImage = bitmap
            existingImageUrl = null
        }
    }

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
                        lat = it.latitude.toString()
                        lon = it.longitude.toString()
                    } ?: run {
                        errorMsg = "Unable to fetch location"
                    }
                } catch (e: Exception) {
                    errorMsg = "Error fetching location: ${e.localizedMessage}"
                }
            }
        } else {
            errorMsg = "Location permission denied"
        }
    }

    // Load existing place data if editing
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
                    existingImageUrl = it.image?.let { imgPath ->
                        "https://labs.anontech.info/cse489/t3/$imgPath"
                    }
                }
            } catch (e: Exception) {
                errorMsg = "Failed to load place data: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    if (showCamera) {
        CameraCaptureScreen(
            onImageCaptured = { bitmap ->
                selectedImage = bitmap
                existingImageUrl = null
                showCamera = false
            },
            onError = { error ->
                errorMsg = "Camera error: ${error.localizedMessage}"
                showCamera = false
            },
            onCancel = { showCamera = false }
        )
    } else {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text("Pick Image")
                }
                Button(onClick = { showCamera = true }, modifier = Modifier.weight(1f)) {
                    Text("Take Photo")
                }
            }

            Button(onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) {
                Text("Use Current Location")
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
                            if (placeId == null) {
                                // CREATE NEW PLACE (POST)
                                val response = ApiClient.api.createPlace(
                                    title.toPlainRequestBody(),
                                    latDouble.toString().toPlainRequestBody(),
                                    lonDouble.toString().toPlainRequestBody(),
                                    selectedImage?.let { bitmapToMultipart(context, it, "image", "$title.jpg") }
                                )
                                if (response.isSuccessful) {
                                    val newId = response.body()?.id
                                    if (newId != null) {
                                        db.placeOwnerDao().insertPlace(PlaceOwner(newId))
                                    }
                                    navController.navigate(ScreenRoutes.List) {
                                        popUpTo(ScreenRoutes.List) { inclusive = true }
                                    }
                                } else {
                                    errorMsg = "Failed to save place: ${response.code()} ${response.message()}"
                                }
                            } else {
                                // UPDATE EXISTING PLACE (PUT)
                                val response = if (selectedImage != null) {
                                    // Multipart with image
                                    val imagePart = bitmapToMultipart(context, selectedImage!!, "image", "place_$placeId.jpg")
                                    ApiClient.api.updatePlaceWithImage(
                                        id = placeId,
                                        title = title,
                                        lat = latDouble,
                                        lon = lonDouble,
                                        image = imagePart
                                    )
                                } else {

                                    ApiClient.api.updatePlaceForm(
                                        id = placeId,
                                        title = title,
                                        lat = latDouble,
                                        lon = lonDouble
                                    )
                                }

                                if (response.isSuccessful) {
                                    navController.navigate(ScreenRoutes.List) {
                                        popUpTo(ScreenRoutes.List) { inclusive = true }
                                    }
                                } else {
                                    errorMsg = "Failed to update place: ${response.code()} ${response.message()}"
                                }
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
}

@Composable
fun CameraCaptureScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    Column {
        AndroidView({ previewView }, modifier = Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onCancel) { Text("Cancel") }
            Button(onClick = {
                val file = File(context.cacheDir, "captured.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) = onError(exc)
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            onImageCaptured(bitmap)
                        }
                    }
                )
            }) { Text("Capture") }
        }
    }
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
