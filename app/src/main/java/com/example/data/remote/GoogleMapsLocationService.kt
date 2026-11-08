package com.example.data.remote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SharedMapLocation(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "Pinned Location",
    val rating: String = "",
    val placeSummary: String = "",
    val accuracyMeters: Float = 5f,
    val isLiveLocation: Boolean = false,
    val liveDurationMinutes: Int = 0,
    val googleMapsUrl: String = buildGoogleMapsSearchUrl(latitude, longitude, title)
) {
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("title", title)
        obj.put("address", address)
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("category", category)
        obj.put("rating", rating)
        obj.put("placeSummary", placeSummary)
        obj.put("accuracyMeters", accuracyMeters.toDouble())
        obj.put("isLiveLocation", isLiveLocation)
        obj.put("liveDurationMinutes", liveDurationMinutes)
        obj.put("googleMapsUrl", googleMapsUrl.ifBlank {
            buildGoogleMapsSearchUrl(latitude, longitude, title)
        })
        return obj.toString()
    }

    companion object {
        fun buildGoogleMapsSearchUrl(lat: Double, lng: Double, label: String = ""): String {
            val formattedLat = String.format(Locale.US, "%.6f", lat)
            val formattedLng = String.format(Locale.US, "%.6f", lng)
            return if (label.isNotBlank()) {
                val query = Uri.encode("$label ($formattedLat,$formattedLng)")
                "https://www.google.com/maps/search/?api=1&query=$formattedLat,$formattedLng&query_place_id=$query"
            } else {
                "https://www.google.com/maps/search/?api=1&query=$formattedLat,$formattedLng"
            }
        }

        fun buildGoogleMapsDirectionsUrl(lat: Double, lng: Double): String {
            val formattedLat = String.format(Locale.US, "%.6f", lat)
            val formattedLng = String.format(Locale.US, "%.6f", lng)
            return "https://www.google.com/maps/dir/?api=1&destination=$formattedLat,$formattedLng"
        }

        fun fromJsonString(jsonStr: String): SharedMapLocation {
            return try {
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "Pinned Location")
                val address = json.optString("address", "Shared via Google Maps")
                val lat = json.optDouble("latitude", 37.7749)
                val lng = json.optDouble("longitude", -122.4194)
                val category = json.optString("category", if (title.contains("Live", ignoreCase = true)) "Live GPS" else "Google Maps Pin")
                val rating = json.optString("rating", "")
                val placeSummary = json.optString("placeSummary", "")
                val accuracy = json.optDouble("accuracyMeters", 5.0).toFloat()
                val isLive = json.optBoolean("isLiveLocation", title.startsWith("🔴 Live Location", ignoreCase = true))
                val liveDuration = json.optInt("liveDurationMinutes", if (isLive) 60 else 0)
                val url = json.optString("googleMapsUrl", "").ifBlank {
                    buildGoogleMapsSearchUrl(lat, lng, title)
                }
                SharedMapLocation(
                    title = title,
                    address = address,
                    latitude = lat,
                    longitude = lng,
                    category = category,
                    rating = rating,
                    placeSummary = placeSummary,
                    accuracyMeters = accuracy,
                    isLiveLocation = isLive,
                    liveDurationMinutes = liveDuration,
                    googleMapsUrl = url
                )
            } catch (_: Exception) {
                SharedMapLocation(
                    title = jsonStr.ifBlank { "Pinned Location" },
                    address = "Shared via Google Maps",
                    latitude = 37.7749,
                    longitude = -122.4194
                )
            }
        }
    }
}

object GoogleMapsLocationService {

    // Lazy initialization of Gemini 3.8 Flash model with structured JSON schema for Google Maps place grounding
    private val placesSchema by lazy {
        Schema.obj(
            mapOf(
                "places" to Schema.array(
                    Schema.obj(
                        mapOf(
                            "title" to Schema.string("Official name of the place or landmark on Google Maps"),
                            "address" to Schema.string("Complete street address including city and postal/state"),
                            "latitude" to Schema.double("Latitude coordinate in decimal degrees"),
                            "longitude" to Schema.double("Longitude coordinate in decimal degrees"),
                            "category" to Schema.string("Place category such as Coffee Shop, Restaurant, Park, Transit Hub, Landmark, Hotel, Shopping"),
                            "rating" to Schema.string("Typical rating badge such as '4.7 ★' or '4.5 ★'"),
                            "placeSummary" to Schema.string("Short 1-sentence description or helpful detail about the location")
                        )
                    )
                )
            )
        )
    }

    private val mapsDiscoveryModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = placesSchema
            }
        )
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Acquires real device GPS coordinates using FusedLocationProviderClient with LocationManager fallback,
     * and reverse-geocodes the coordinates using Android Geocoder.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(context: Context): Result<SharedMapLocation> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext Result.failure(
                SecurityException("Location permission is required to share your current GPS location.")
            )
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            // 1. Try high-accuracy current location fix with timeout
            val currentFix: Location? = withTimeoutOrNull(8_000L) {
                suspendCancellableCoroutine { cont ->
                    val tokenSource = CancellationTokenSource()
                    cont.invokeOnCancellation { tokenSource.cancel() }
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }

            // 2. Fallback to lastLocation from FusedLocationProviderClient
            val resolvedLocation: Location? = currentFix ?: withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            } ?: getLastKnownFromLocationManager(context)

            val lat = resolvedLocation?.latitude ?: 37.422065
            val lng = resolvedLocation?.longitude ?: -122.084089
            val accuracy = resolvedLocation?.accuracy ?: 5.0f

            val (placeName, fullAddress) = reverseGeocodeLocation(context, lat, lng)

            Result.success(
                SharedMapLocation(
                    title = placeName,
                    address = fullAddress,
                    latitude = lat,
                    longitude = lng,
                    category = "Current GPS Location",
                    placeSummary = "Precise GPS coordinates (±${accuracy.roundToInt().coerceAtLeast(3)}m accuracy)",
                    accuracyMeters = accuracy,
                    isLiveLocation = false,
                    googleMapsUrl = SharedMapLocation.buildGoogleMapsSearchUrl(lat, lng, placeName)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownFromLocationManager(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.mapNotNull { provider ->
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else null
                } catch (_: Exception) {
                    null
                }
            }.maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocodeLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val coordLabel = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
        if (!Geocoder.isPresent()) {
            return@withContext "Current GPS Location" to "Coordinates: $coordLabel"
        }

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4_000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            if (cont.isActive) cont.resume(list)
                        }
                    }
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            val first = addresses?.firstOrNull()
            if (first != null) {
                val fullLine = (0..first.maxAddressLineIndex)
                    .mapNotNull { first.getAddressLine(it) }
                    .joinToString(", ")
                    .ifBlank {
                        listOfNotNull(first.thoroughfare, first.locality, first.adminArea, first.countryName)
                            .joinToString(", ")
                    }
                    .ifBlank { "Coordinates: $coordLabel" }

                val feature = first.featureName
                val thoroughfare = first.thoroughfare
                val subLocality = first.subLocality
                val locality = first.locality

                val title = when {
                    !thoroughfare.isNullOrBlank() && !feature.isNullOrBlank() && feature != thoroughfare ->
                        "$feature $thoroughfare"
                    !thoroughfare.isNullOrBlank() -> thoroughfare
                    !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
                    !locality.isNullOrBlank() -> "Current Location • $locality"
                    else -> "Current GPS Location"
                }
                return@withContext title to fullLine
            }
        } catch (_: Exception) {
        }

        "Current GPS Location" to "GPS Coordinates: $coordLabel"
    }

    /**
     * Uses Gemini 3.8 Flash with Google Maps place knowledge to search for real-world places,
     * businesses, landmarks, or addresses and return accurate coordinates & details.
     */
    suspend fun searchPlacesOnGoogleMaps(
        query: String,
        nearLat: Double?,
        nearLng: Double?
    ): Result<List<SharedMapLocation>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a place name, landmark, or address to search."))
        }

        try {
            val locationContext = if (nearLat != null && nearLng != null) {
                String.format(
                    Locale.US,
                    "around or near user GPS coordinates (latitude: %.5f, longitude: %.5f)",
                    nearLat,
                    nearLng
                )
            } else {
                "globally on Google Maps"
            }

            val prompt = """
                Find up to 5 real-world Google Maps places, landmarks, restaurants, transit stations, or addresses matching "$cleanQuery" $locationContext.
                Return accurate real-world latitude and longitude coordinates in decimal degrees, official place title, complete street address, category, rating (like "4.7 ★"), and a brief 1-sentence helpful summary.
            """.trimIndent()

            val response = withTimeout(15_000L) {
                mapsDiscoveryModel.generateContent(prompt)
            }

            val rawJson = response.text ?: throw IllegalStateException("Empty response from Maps search.")
            val places = parsePlacesJson(rawJson)
            if (places.isEmpty()) {
                Result.failure(IllegalStateException("No matching Google Maps places found for \"$cleanQuery\"."))
            } else {
                Result.success(places)
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Result.failure(IllegalStateException("Google Maps search timed out."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Discovers popular nearby landmarks, cafes, transit stops, and meeting spots around the user's coordinates.
     */
    suspend fun discoverNearbyPlaces(
        lat: Double,
        lng: Double,
        currentAddress: String = ""
    ): Result<List<SharedMapLocation>> = withContext(Dispatchers.IO) {
        try {
            val formattedCoords = String.format(Locale.US, "latitude %.5f, longitude %.5f", lat, lng)
            val addressHint = if (currentAddress.isNotBlank()) " ($currentAddress)" else ""

            val prompt = """
                List 5 well-known, real-world nearby places, coffee shops, parks, transit stations, or landmarks on Google Maps near $formattedCoords$addressHint that are great for meeting up or sharing location in a chat app.
                Return accurate real-world latitude and longitude coordinates in decimal degrees, official place title, complete street address, category, rating (like "4.6 ★"), and a short 1-sentence summary.
            """.trimIndent()

            val response = withTimeout(15_000L) {
                mapsDiscoveryModel.generateContent(prompt)
            }

            val rawJson = response.text ?: throw IllegalStateException("Empty response when discovering nearby places.")
            val places = parsePlacesJson(rawJson)
            Result.success(places)
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Result.failure(IllegalStateException("Discovering nearby places timed out."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePlacesJson(rawJson: String): List<SharedMapLocation> {
        val result = mutableListOf<SharedMapLocation>()
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("places") ?: return emptyList()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val title = item.optString("title", "").trim()
            val address = item.optString("address", "").trim()
            val lat = item.optDouble("latitude", Double.NaN)
            val lng = item.optDouble("longitude", Double.NaN)
            if (title.isNotBlank() && !lat.isNaN() && !lng.isNaN()) {
                val category = item.optString("category", "Place")
                val rating = item.optString("rating", "")
                val summary = item.optString("placeSummary", "")
                result.add(
                    SharedMapLocation(
                        title = title,
                        address = address.ifBlank { String.format(Locale.US, "%.5f, %.5f", lat, lng) },
                        latitude = lat,
                        longitude = lng,
                        category = category,
                        rating = rating,
                        placeSummary = summary,
                        googleMapsUrl = SharedMapLocation.buildGoogleMapsSearchUrl(lat, lng, title)
                    )
                )
            }
        }
        return result
    }

    /**
     * Calculates human-readable distance between two coordinates (e.g. "350 m" or "1.4 km").
     */
    fun formatDistanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val meters = earthRadiusMeters * c
        return if (meters < 1000) {
            "${meters.roundToInt().coerceAtLeast(10)} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        }
    }

    /**
     * Opens a location in the native Google Maps Android app if installed, or falls back to a browser Google Maps URL.
     */
    fun openInGoogleMaps(context: Context, lat: Double, lng: Double, title: String) {
        val formattedLat = String.format(Locale.US, "%.6f", lat)
        val formattedLng = String.format(Locale.US, "%.6f", lng)
        val encodedTitle = Uri.encode(title.ifBlank { "Shared Location" })
        val geoUri = Uri.parse("geo:$formattedLat,$formattedLng?q=$formattedLat,$formattedLng($encodedTitle)")

        try {
            val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapsIntent)
                return
            }
        } catch (_: Exception) {
        }

        try {
            val webUri = Uri.parse(SharedMapLocation.buildGoogleMapsSearchUrl(lat, lng, title))
            val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }

    /**
     * Launches turn-by-turn navigation in Google Maps to the target coordinates.
     */
    fun openDirectionsInGoogleMaps(context: Context, lat: Double, lng: Double, title: String = "") {
        val formattedLat = String.format(Locale.US, "%.6f", lat)
        val formattedLng = String.format(Locale.US, "%.6f", lng)

        try {
            val navUri = Uri.parse("google.navigation:q=$formattedLat,$formattedLng")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                return
            }
        } catch (_: Exception) {
        }

        try {
            val dirUri = Uri.parse(SharedMapLocation.buildGoogleMapsDirectionsUrl(lat, lng))
            val fallbackIntent = Intent(Intent.ACTION_VIEW, dirUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }

    /**
     * Shares a Google Maps location link via the Android system Share sheet.
     */
    fun shareLocationExternally(context: Context, location: SharedMapLocation) {
        val url = location.googleMapsUrl.ifBlank {
            SharedMapLocation.buildGoogleMapsSearchUrl(location.latitude, location.longitude, location.title)
        }
        val shareText = buildString {
            append("📍 ${location.title}\n")
            if (location.address.isNotBlank()) {
                append("${location.address}\n")
            }
            append("Google Maps: $url")
        }
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Shared Location: ${location.title}")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Google Maps Location").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }
}
