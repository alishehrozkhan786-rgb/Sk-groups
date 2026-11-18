package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverRecord
import com.example.data.model.JobDispatch
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.data.model.safeEnum
import com.example.i18n.LocalAppStrings
import com.example.location.DriverLocationTracker
import com.example.location.compressImageUriToBase64
import com.example.ui.components.Base64CarPhotoPreview
import com.example.ui.components.DubaiLiveMap
import com.example.ui.components.MapMarkerPin
import com.example.ui.components.StatusPill
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.openGoogleMapsNavigation
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GarnetSurface
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DriverViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun DriverPortalScreen(
    viewModel: DriverViewModel,
    driver: DriverRecord,
    isArabic: Boolean
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current

    val openDispatchesState by viewModel.openDispatchesState.collectAsStateWithLifecycle()
    val driverBookingsState by viewModel.driverBookingsState.collectAsStateWithLifecycle()
    val driverTruck by viewModel.driverTruck.collectAsStateWithLifecycle()
    val lastGps by viewModel.lastGpsReading.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val activeBooking: Booking? = remember(driverBookingsState) {
        if (driverBookingsState is UiState.Success) {
            (driverBookingsState as UiState.Success<List<Booking>>).data.firstOrNull {
                it.status !in listOf(BookingStatus.DELIVERED.name, BookingStatus.CANCELLED.name)
            }
        } else null
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val tracker = remember(context) { DriverLocationTracker(context) }

    // Continuous 5-second live GPS stream whenever permission is granted
    LaunchedEffect(hasLocationPermission, activeBooking?.id, activeBooking?.status) {
        if (hasLocationPermission) {
            tracker.observeLocationUpdates(5000L).collect { reading ->
                viewModel.onLiveGpsTick(reading, activeBooking)
            }
        }
    }

    // Zero-permission Photo Picker for car loaded on 16ft flatbed
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && activeBooking != null) {
            val base64 = compressImageUriToBase64(context, uri)
            viewModel.advanceJobStatus(activeBooking, BookingStatus.CAR_LOADED, base64)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (actionMessage != null) {
            item {
                Surface(
                    color = GarnetSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ChampagneGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearMessage() }
                ) {
                    Text(actionMessage ?: "", color = ChampagneGold, modifier = Modifier.padding(12.dp))
                }
            }
        }

        // Driver Identity & Always-ON GPS Live Telemetry Card
        item {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${driver.name} (${driver.driverCode})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("Truck: ${driver.truckNumber}", style = MaterialTheme.typography.labelMedium, color = ChampagneGold)
                        }
                        val drvStatus = safeEnum(driver.status, TruckStatus.FREE)
                        StatusPill(
                            label = when (drvStatus) {
                                TruckStatus.FREE -> s.statusFree
                                TruckStatus.ON_JOB -> s.statusOnJob
                                TruckStatus.OFFLINE -> s.statusOffline
                            },
                            color = when (drvStatus) {
                                TruckStatus.FREE -> StatusEmerald
                                TruckStatus.ON_JOB -> CrimsonBright
                                TruckStatus.OFFLINE -> StatusOffline
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedCarbon)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (hasLocationPermission) StatusEmerald else CrimsonBright)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.gpsAlwaysOnLabel, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            val gpsText = if (lastGps != null) {
                                "GPS: ${String.format("%.4f", lastGps!!.lat)}, ${String.format("%.4f", lastGps!!.lng)} • ${lastGps!!.speedKmh.toInt()} km/h"
                            } else if (hasLocationPermission) {
                                "Acquiring GPS fix..."
                            } else {
                                s.gpsPermissionNeeded
                            }
                            Text(gpsText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("grant_gps_button")
                            ) {
                                Text(s.grantGpsBtn, color = Color.White)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val lat = lastGps?.lat ?: driverTruck?.lat ?: 25.1851
                                    val lng = lastGps?.lng ?: driverTruck?.lng ?: 55.2662
                                    val spd = lastGps?.speedKmh ?: 52.0
                                    viewModel.manualGpsPing(lat, lng, spd, activeBooking)
                                },
                                border = BorderStroke(1.dp, ChampagneGold)
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.pingGpsNowBtn, color = ChampagneGold)
                            }
                        }
                    }

                    // Availability toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(TruckStatus.FREE to s.statusFree, TruckStatus.OFFLINE to s.statusOffline).forEach { (st, lbl) ->
                            OutlinedButton(
                                onClick = { viewModel.setDriverAvailability(st) },
                                border = BorderStroke(1.dp, if (driver.status == st.name) StatusEmerald else BorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(lbl, color = if (driver.status == st.name) StatusEmerald else TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ACTIVE JOB CONSOLE (Note: strictly NO payment details shown to Driver!)
        if (activeBooking != null) {
            item {
                val st = safeEnum(activeBooking.status, BookingStatus.ASSIGNED)
                val mapTruck = Truck(
                    id = driver.truckId,
                    truckNumber = driver.truckNumber,
                    pinColorHex = "#EF5350",
                    status = TruckStatus.ON_JOB.name,
                    lat = lastGps?.lat ?: activeBooking.truckLat,
                    lng = lastGps?.lng ?: activeBooking.truckLng,
                    speedKmh = lastGps?.speedKmh ?: activeBooking.truckSpeedKmh
                )
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, ChampagneGold.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Dispatch", style = MaterialTheme.typography.headlineMedium, color = ChampagneGold)
                            StatusPill(localizedStatus(st, s), StatusBlue)
                        }

                        DubaiLiveMap(
                            height = 220.dp,
                            trucks = listOf(mapTruck),
                            pickupPin = MapMarkerPin("pk", "Pickup", activeBooking.pickupLat, activeBooking.pickupLng, StatusEmerald),
                            dropPin = MapMarkerPin("dp", "Drop", activeBooking.dropLat, activeBooking.dropLng, ChampagneGold),
                            isArabic = isArabic
                        )

                        Text(
                            text = "Customer: ${activeBooking.customerName}\nPickup: ${activeBooking.pickupArea} — ${activeBooking.pickupAddress}\nDrop: ${activeBooking.dropArea} — ${activeBooking.dropAddress}\nVehicle: ${activeBooking.carType} (${activeBooking.carCondition})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        if (activeBooking.issueNotes.isNotBlank()) {
                            Text("Notes: ${activeBooking.issueNotes}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val navToDrop = st == BookingStatus.CAR_LOADED
                                    openGoogleMapsNavigation(
                                        context,
                                        if (navToDrop) activeBooking.dropLat else activeBooking.pickupLat,
                                        if (navToDrop) activeBooking.dropLng else activeBooking.pickupLng,
                                        if (navToDrop) activeBooking.dropArea else activeBooking.pickupArea
                                    )
                                },
                                border = BorderStroke(1.dp, StatusBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.openMapsNavBtn, color = StatusBlue)
                            }
                            if (activeBooking.customerPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { dialPhoneNumber(context, activeBooking.customerPhone) },
                                    border = BorderStroke(1.dp, StatusEmerald),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.callCustomerBtn, color = StatusEmerald)
                                }
                            }
                        }

                        if (activeBooking.carPhotoBase64.isNotBlank()) {
                            Base64CarPhotoPreview(activeBooking.carPhotoBase64, "Uploaded car on flatbed")
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Step-by-step Driver Action Buttons
                        when (st) {
                            BookingStatus.ASSIGNED -> {
                                Button(
                                    onClick = { viewModel.advanceJobStatus(activeBooking, BookingStatus.EN_ROUTE_PICKUP) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(s.startPickupBtn, color = Color.White)
                                }
                            }
                            BookingStatus.EN_ROUTE_PICKUP -> {
                                Button(
                                    onClick = { viewModel.advanceJobStatus(activeBooking, BookingStatus.TRUCK_ARRIVED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(s.markArrivedBtn, color = Color.Black)
                                }
                            }
                            BookingStatus.TRUCK_ARRIVED -> {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("upload_car_photo_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(s.uploadPhotoAndLoadBtn, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.advanceJobStatus(activeBooking, BookingStatus.CAR_LOADED) },
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark Loaded Without Photo", color = TextSecondary)
                                }
                            }
                            BookingStatus.CAR_LOADED -> {
                                Button(
                                    onClick = { viewModel.advanceJobStatus(activeBooking, BookingStatus.DELIVERED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(s.completeDeliveryBtn, color = Color.Black)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        // NEW JOB REQUESTS QUEUE (No payment details visible)
        item {
            Text(s.newJobRequests, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        when (val st = openDispatchesState) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CrimsonPrimary)
                }
            }
            is UiState.Error -> item {
                Text(st.message, color = CrimsonBright)
            }
            is UiState.Success -> {
                if (st.data.isEmpty()) {
                    item {
                        Surface(
                            color = CarbonSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TextSecondary)
                                Spacer(Modifier.width(12.dp))
                                Text(s.noOpenJobs, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(st.data, key = { it.id }) { job ->
                        DriverJobRequestCard(
                            job = job,
                            onAccept = { viewModel.acceptJob(job) },
                            onReject = { viewModel.rejectJob(job.bookingId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverJobRequestCard(
    job: JobDispatch,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val s = LocalAppStrings.current
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderCrimson),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${job.pickupArea} → ${job.dropArea}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                StatusPill("${job.distanceKm} ${s.kmUnit}", ChampagneGold)
            }
            Text(
                text = "${job.carType} • ${job.carCondition}\nPickup: ${job.pickupAddress}\nDrop: ${job.dropAddress}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (job.issueNotes.isNotBlank()) {
                Text("Notes: ${job.issueNotes}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onReject,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.rejectJobBtn, color = TextSecondary)
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.acceptJobBtn, color = Color.Black)
                }
            }
        }
    }
}
