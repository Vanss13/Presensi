package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

object LocationService {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<GpsCoordinate> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val fresh: Location? = try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            } catch (_: Exception) {
                null
            }
            val resolved = fresh ?: client.lastLocation.await()
            if (resolved != null) {
                Result.success(
                    GpsCoordinate(
                        latitude = resolved.latitude,
                        longitude = resolved.longitude,
                        accuracyMeters = resolved.accuracy
                    )
                )
            } else {
                Result.failure(IllegalStateException("Sinyal GPS belum tersedia. Pastikan lokasi perangkat aktif."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Long {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).roundToLong().coerceAtLeast(0L)
    }
}
