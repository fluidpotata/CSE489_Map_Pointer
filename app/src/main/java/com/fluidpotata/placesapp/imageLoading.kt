package com.fluidpotata.placesapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.net.URL

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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


