package com.example.ui.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.GoogleMapsLocationService
import com.example.data.remote.SharedMapLocation
import com.example.ui.theme.Emerald500
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private enum class LocationShareMode {
    CURRENT_PIN,
    LIVE_LOCATION,
    GOOGLE_MAPS_SEARCH
}

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onSendLocation: (title: String, address: String, latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(LocationShareMode.CURRENT_PIN) }

    var placeTitle by remember { mutableStateOf("Current GPS Location") }
    var address by remember { mutableStateOf("Tap 'Use Live GPS' or search on Google Maps") }
    var lat by remember { mutableDoubleStateOf(37.7882) }
    var lng by remember { mutableDoubleStateOf(-122.4014) }
    var accuracyMeters by remember { mutableFloatStateOf(5.0f) }

    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatusText by remember { mutableStateOf("Ready to lock GPS or search Google Maps") }

    // Live Location options
    var liveDurationMinutes by remember { mutableIntStateOf(60) } // 15, 60, 480

    // Google Maps Search / AI Grounding state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchingMaps by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var discoveredPlaces by remember { mutableStateOf<List<SharedMapLocation>>(emptyList()) }

    val fetchGpsLocation: () -> Unit = {
        scope.launch {
            isFetchingGps = true
            gpsStatusText = "Acquiring high-accuracy GPS fix..."
            val result = GoogleMapsLocationService.fetchCurrentDeviceLocation(context)
            isFetchingGps = false
            result.onSuccess { loc ->
                placeTitle = loc.title
                address = loc.address
                lat = loc.latitude
                lng = loc.longitude
                accuracyMeters = loc.accuracyMeters
                gpsStatusText = "GPS Locked • ±${loc.accuracyMeters.roundToInt().coerceAtLeast(3)}m accuracy"
            }.onFailure { err ->
                gpsStatusText = err.localizedMessage ?: "Using fallback GPS coordinates"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            Toast.makeText(
                context,
                "Location permission denied. You can still search places on Google Maps.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Automatically acquire real device GPS location on dialog launch if permission is already granted
    LaunchedEffect(Unit) {
        if (GoogleMapsLocationService.hasLocationPermission(context)) {
            fetchGpsLocation()
        }
    }

    val performMapsSearch: (String) -> Unit = { queryToRun ->
        focusManager.clearFocus()
        scope.launch {
            isSearchingMaps = true
            searchError = null
            val result = GoogleMapsLocationService.searchPlacesOnGoogleMaps(
                query = queryToRun,
                nearLat = lat,
                nearLng = lng
            )
            isSearchingMaps = false
            result.onSuccess { places ->
                discoveredPlaces = places
            }.onFailure { err ->
                searchError = err.localizedMessage ?: "Could not search Google Maps right now."
            }
        }
    }

    val performNearbyDiscovery: () -> Unit = {
        focusManager.clearFocus()
        scope.launch {
            isSearchingMaps = true
            searchError = null
            val result = GoogleMapsLocationService.discoverNearbyPlaces(
                lat = lat,
                lng = lng,
                currentAddress = address
            )
            isSearchingMaps = false
            result.onSuccess { places ->
                discoveredPlaces = places
            }.onFailure { err ->
                searchError = err.localizedMessage ?: "Could not load nearby places from Google Maps."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Google Maps Share Location",
                                tint = Emerald500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Maps Location",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Share GPS pin, live location, or Maps place",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Google Maps Preview Canvas
                GoogleMapsInteractivePreview(
                    title = placeTitle,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = accuracyMeters,
                    isLiveMode = selectedMode == LocationShareMode.LIVE_LOCATION,
                    isFetchingGps = isFetchingGps,
                    onAdjustCoordinates = { newLat, newLng ->
                        lat = newLat
                        lng = newLng
                        scope.launch {
                            val (newTitle, newAddr) = GoogleMapsLocationService.reverseGeocodeLocation(
                                context,
                                newLat,
                                newLng
                            )
                            placeTitle = newTitle
                            address = newAddr
                        }
                    },
                    onOpenGoogleMaps = {
                        GoogleMapsLocationService.openInGoogleMaps(context, lat, lng, placeTitle)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == LocationShareMode.CURRENT_PIN,
                        onClick = { selectedMode = LocationShareMode.CURRENT_PIN },
                        label = { Text("GPS Pin", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald500.copy(alpha = 0.18f),
                            selectedLabelColor = Emerald500,
                            selectedLeadingIconColor = Emerald500
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedMode == LocationShareMode.LIVE_LOCATION,
                        onClick = { selectedMode = LocationShareMode.LIVE_LOCATION },
                        label = { Text("Live", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ShareLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.16f),
                            selectedLabelColor = Color(0xFFE53935),
                            selectedLeadingIconColor = Color(0xFFE53935)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedMode == LocationShareMode.GOOGLE_MAPS_SEARCH,
                        onClick = {
                            selectedMode = LocationShareMode.GOOGLE_MAPS_SEARCH
                            if (discoveredPlaces.isEmpty() && !isSearchingMaps) {
                                performNearbyDiscovery()
                            }
                        },
                        label = { Text("Places", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A73E8).copy(alpha = 0.18f),
                            selectedLabelColor = Color(0xFF1A73E8),
                            selectedLeadingIconColor = Color(0xFF1A73E8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("google_maps_places_tab")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick GPS Lock + Open in Google Maps Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (GoogleMapsLocationService.hasLocationPermission(context)) {
                                fetchGpsLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("use_live_gps_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Emerald500
                            )
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFetchingGps) "Locating..." else "Use Live GPS",
                            color = Emerald500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            GoogleMapsLocationService.openInGoogleMaps(context, lat, lng, placeTitle)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_in_google_maps_preview_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Google Maps",
                            color = Color(0xFF1A73E8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode-Specific Section: Live Location Duration
                AnimatedVisibility(visible = selectedMode == LocationShareMode.LIVE_LOCATION) {
                    Surface(
                        color = Color(0xFFE53935).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = "Live Location",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share Real-Time Live Location",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE53935)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Participants in this chat will see your live Google Maps position for the selected duration:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    15 to "15 Minutes",
                                    60 to "1 Hour",
                                    480 to "8 Hours"
                                ).forEach { (mins, label) ->
                                    val isSelected = liveDurationMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { liveDurationMinutes = mins },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE53935),
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Mode-Specific Section: Google Maps Place Search & Nearby Discovery (Gemini 3.8 Flash)
                AnimatedVisibility(visible = selectedMode == LocationShareMode.GOOGLE_MAPS_SEARCH) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search Google Maps places, cafes, landmarks...",
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search Maps",
                                        tint = Color(0xFF1A73E8)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            performMapsSearch(searchQuery)
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1A73E8)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("google_maps_search_input")
                            )

                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        performMapsSearch(searchQuery)
                                    } else {
                                        performNearbyDiscovery()
                                    }
                                },
                                enabled = !isSearchingMaps,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                                modifier = Modifier.testTag("google_maps_search_button")
                            ) {
                                if (isSearchingMaps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Find", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick category search chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { performNearbyDiscovery() },
                                label = { Text("✨ Nearby Spots", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = {
                                    searchQuery = "Coffee shops & cafes"
                                    performMapsSearch("Coffee shops & cafes")
                                },
                                label = { Text("☕ Cafes", fontSize = 11.sp) }
                            )
                            AssistChip(
                                onClick = {
                                    searchQuery = "Restaurants & dining"
                                    performMapsSearch("Restaurants & dining")
                                },
                                label = { Text("🍽️ Dining", fontSize = 11.sp) }
                            )
                        }

                        if (searchError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = searchError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (discoveredPlaces.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Google Maps Grounded Results (Tap to select or share)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A73E8)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                discoveredPlaces.forEach { place ->
                                    val isSelectedPlace = placeTitle == place.title &&
                                             kotlin.math.abs(lat - place.latitude) < 0.0001
                                    val distanceStr = GoogleMapsLocationService.formatDistanceBetween(
                                        lat, lng, place.latitude, place.longitude
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelectedPlace) {
                                            Emerald500.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelectedPlace) Emerald500 else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                placeTitle = place.title
                                                address = buildString {
                                                    append(place.address)
                                                    if (place.rating.isNotBlank()) append(" • ${place.rating}")
                                                }
                                                lat = place.latitude
                                                lng = place.longitude
                                                gpsStatusText = "Selected from Google Maps: ${place.category}"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1A73E8).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1A73E8),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = place.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (place.rating.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = place.rating,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF59E0B)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = place.address,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${place.category} • $distanceStr",
                                                        fontSize = 10.sp,
                                                        color = Emerald500,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    onSendLocation(
                                                        place.title,
                                                        place.address,
                                                        place.latitude,
                                                        place.longitude
                                                    )
                                                    onDismiss()
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Share ${place.title}",
                                                    tint = Emerald500,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Editable Location Title & Street Address
                OutlinedTextField(
                    value = placeTitle,
                    onValueChange = { placeTitle = it },
                    label = { Text("Location Name / Place Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address / Details") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = gpsStatusText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalTitle = if (selectedMode == LocationShareMode.LIVE_LOCATION) {
                                val durationLabel = when (liveDurationMinutes) {
                                    15 -> "15m"
                                    60 -> "1h"
                                    else -> "8h"
                                }
                                val cleanBase = placeTitle.removePrefix("🔴 Live Location").trim()
                                    .ifBlank { "Current GPS" }
                                "🔴 Live Location ($durationLabel) • $cleanBase"
                            } else {
                                placeTitle.ifBlank { "Pinned Google Maps Location" }
                            }
                            val finalAddress = address.ifBlank {
                                String.format(Locale.US, "GPS: %.5f, %.5f", lat, lng)
                            }
                            onSendLocation(finalTitle, finalAddress, lat, lng)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == LocationShareMode.LIVE_LOCATION) {
                                Color(0xFFE53935)
                            } else {
                                Emerald500
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_location_button")
                    ) {
                        Icon(
                            imageVector = if (selectedMode == LocationShareMode.LIVE_LOCATION) {
                                Icons.Default.ShareLocation
                            } else {
                                Icons.Default.LocationOn
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedMode == LocationShareMode.LIVE_LOCATION) {
                                "Share Live Location"
                            } else {
                                "Send Google Maps Location"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMapsInteractivePreview(
    title: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    isLiveMode: Boolean,
    isFetchingGps: Boolean,
    onAdjustCoordinates: (Double, Double) -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "maps_pulse")
    val pulseRadiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val pinColor = if (isLiveMode) Color(0xFFE53935) else Color(0xFFEA4335) // Google Maps Red
    val accentBlue = Color(0xFF1A73E8) // Google Maps Blue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .pointerInput(latitude, longitude) {
                detectTapGestures { tapOffset ->
                    // Allow tapping on map canvas to nudge pin slightly around current area
                    if (size.width > 0 && size.height > 0) {
                        val normX = (tapOffset.x / size.width) - 0.5f
                        val normY = 0.5f - (tapOffset.y / size.height)
                        val adjustedLat = latitude + (normY * 0.004)
                        val adjustedLng = longitude + (normX * 0.004)
                        onAdjustCoordinates(adjustedLat, adjustedLng)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Map Terrain Base
            drawRect(color = Color(0xFFE8EEF5))

            // Green park zones
            drawRect(
                color = Color(0xFFC8E6C9).copy(alpha = 0.7f),
                topLeft = Offset(w * 0.05f, h * 0.1f),
                size = Size(w * 0.28f, h * 0.42f)
            )
            drawRect(
                color = Color(0xFFC8E6C9).copy(alpha = 0.65f),
                topLeft = Offset(w * 0.68f, h * 0.52f),
                size = Size(w * 0.26f, h * 0.38f)
            )

            // Water channel feature
            val waterPath = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.9f, w, h * 0.72f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(waterPath, color = Color(0xFFAECBFA).copy(alpha = 0.65f))

            // Secondary local streets
            val streetColor = Color.White
            for (i in 1..4) {
                val y = h * (i / 5f)
                drawLine(
                    color = streetColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 5f
                )
            }
            for (i in 1..5) {
                val x = w * (i / 6f)
                drawLine(
                    color = streetColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 5f
                )
            }

            // Primary arterial highway (Google Maps yellow/amber)
            drawLine(
                color = Color(0xFFFDE293),
                start = Offset(0f, h * 0.32f),
                end = Offset(w, h * 0.62f),
                strokeWidth = 11f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.48f, 0f),
                end = Offset(w * 0.52f, h),
                strokeWidth = 9f,
                cap = StrokeCap.Round
            )

            // Center Pin / GPS Pulse
            val center = Offset(w * 0.5f, h * 0.48f)
            drawCircle(
                color = (if (isLiveMode) pinColor else accentBlue).copy(alpha = pulseAlpha),
                radius = 34f * pulseRadiusMultiplier,
                center = center
            )
            drawCircle(
                color = (if (isLiveMode) pinColor else accentBlue).copy(alpha = 0.2f),
                radius = 20f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 11f,
                center = center
            )
            drawCircle(
                color = if (isLiveMode) pinColor else accentBlue,
                radius = 7.5f,
                center = center
            )
        }

        // Top-Left Google Maps Pill Badge
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Google Maps",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isLiveMode) "Google Maps • LIVE" else "Google Maps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiveMode) Color(0xFFE53935) else Color(0xFF1F2937)
                )
            }
        }

        // Top-Right Open in Maps Action Pill
        Surface(
            color = Color(0xFF1A73E8),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable { onOpenGoogleMaps() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launch Maps",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open in Google Maps",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Center Floating Pin Icon above the GPS dot
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Selected Location Pin",
            tint = pinColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 26.dp)
                .size(34.dp)
        )

        // Bottom Coordinate & Accuracy Bar
        Surface(
            color = Color(0xFF111827).copy(alpha = 0.82f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Pinned Location" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.5f°, %.5f° • ±%dm • Tap map to adjust",
                            latitude,
                            longitude,
                            accuracyMeters.roundToInt().coerceAtLeast(3)
                        ),
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (isFetchingGps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Emerald500
                    )
                }
            }
        }
    }
}
