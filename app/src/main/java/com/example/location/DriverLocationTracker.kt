package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Base64
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayOutputStream

data class DriverGpsReading(
    val lat: Double,
    val lng: Double,
    val speedKmh: Double,
    val bearingDeg: Double
)

class DriverLocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMs: Long = 5000L): Flow<DriverGpsReading> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location = result.lastLocation ?: return
                val speedKmh = (loc.speed * 3.6).toDouble().coerceIn(0.0, 180.0)
                trySend(
                    DriverGpsReading(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        speedKmh = speedKmh,
                        bearingDeg = loc.bearing.toDouble().coerceIn(0.0, 360.0)
                    )
                )
            }
        }

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                trySend(
                    DriverGpsReading(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        speedKmh = (loc.speed * 3.6).toDouble().coerceIn(0.0, 180.0),
                        bearingDeg = loc.bearing.toDouble().coerceIn(0.0, 360.0)
                    )
                )
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}

fun compressImageUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val maxDim = 480
        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
        val w = (original.width * ratio).toInt().coerceAtLeast(1)
        val h = (original.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 65, out)
        val bytes = out.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}
