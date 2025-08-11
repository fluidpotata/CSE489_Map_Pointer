package com.fluidpotata.placesapp

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

data class Place(
    val id: Int,
    val title: String,
    val lat: Double,
    val lon: Double,
    val image: String?
)

interface PlacesApi {
    @GET("api.php")
    suspend fun getPlaces(): List<Place>

    @Multipart
    @POST("api.php")
    suspend fun createPlace(
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Unit>

    @FormUrlEncoded
    @PUT("api.php")
    suspend fun updatePlace(
        @Field("id") id: Int,
        @Field("title") title: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): Response<Unit>
}

object ApiClient {
    val api: PlacesApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.105:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApi::class.java)
    }
}

fun String.toPlainRequestBody(): RequestBody =
    this.toRequestBody("text/plain".toMediaTypeOrNull())

fun bitmapToMultipart(
    context: Context,
    bitmap: Bitmap,
    partName: String,
    filename: String
): MultipartBody.Part {
    val resized = Bitmap.createScaledBitmap(bitmap, 800, 600, true)
    val file = File(context.cacheDir, filename)
    FileOutputStream(file).use {
        resized.compress(Bitmap.CompressFormat.JPEG, 85, it)
    }
    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, file.name, requestFile)
}
