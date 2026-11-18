package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CarCondition
import com.example.data.model.CarType
import com.example.data.model.DubaiGeoCatalog
import com.example.data.model.DubaiZone
import com.example.data.model.PaymentMethod
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.data.model.safeEnum
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.components.Base64CarPhotoPreview
import com.example.ui.components.DubaiLiveMap
import com.example.ui.components.MapMarkerPin
import com.example.ui.components.StatusPill
import com.example.ui.components.dialPhoneNumber
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GarnetSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun CustomerPortalScreen(
    viewModel: CustomerViewModel,
    isArabic: Boolean,
    customerDisplayName: String,
    initialPickupZone: DubaiZone,
    initialDropZone: DubaiZone,
    initialCarType: CarType,
    initialCondition: CarCondition
) {
    val s = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val pricingConfig by viewModel.pricingConfig.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val trackedBooking by viewModel.trackedBooking.collectAsStateWithLifecycle()
    val trackedTruck by viewModel.trackedTruck.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    // Auto-select most recent active booking if none selected yet
    LaunchedEffect(bookingsState) {
        if (bookingsState is UiState.Success) {
            val list = (bookingsState as UiState.Success<List<Booking>>).data
            if (viewModel.selectedTrackingBookingId.value == null) {
                val active = list.firstOrNull {
                    it.status != BookingStatus.DELIVERED.name && it.status != BookingStatus.CANCELLED.name
                } ?: list.firstOrNull()
                if (active != null) {
                    viewModel.selectBookingToTrack(active.id)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CarbonSurface,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CrimsonBright
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(s.tabBook) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(s.tabLiveTrack) },
                icon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(s.tabHistory) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (actionMessage != null) {
            Surface(
                color = GarnetSurface,
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.clearMessage() }
            ) {
                Text(
                    text = actionMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChampagneGold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        when (selectedTab) {
            0 -> CustomerBookingTab(
                isArabic = isArabic,
                customerDisplayName = customerDisplayName,
                initialPickupZone = initialPickupZone,
                initialDropZone = initialDropZone,
                initialCarType = initialCarType,
                initialCondition = initialCondition,
                pricingConfig = pricingConfig,
                isSubmitting = isSubmitting,
                onCreateBooking = { name, phone, pZone, pAddr, dZone, dAddr, cType, cCond, notes, dist, price, payMethod ->
                    viewModel.createBooking(
                        customerName = name,
                        customerPhone = phone,
                        pickupArea = pZone.enName,
                        pickupAddress = pAddr,
                        pickupLat = pZone.lat,
                        pickupLng = pZone.lng,
                        dropArea = dZone.enName,
                        dropAddress = dAddr,
                        dropLat = dZone.lat,
                        dropLng = dZone.lng,
                        carType = cType.name,
                        carCondition = cCond.name,
                        issueNotes = notes,
                        distanceKm = dist,
                        priceAed = price,
                        paymentMethod = payMethod,
                        onBooked = { selectedTab = 1 }
                    )
                }
            )
            1 -> CustomerLiveTrackTab(
                booking = trackedBooking,
                truck = trackedTruck,
                isArabic = isArabic,
                onGoToBook = { selectedTab = 0 }
            )
            2 -> CustomerHistoryTab(
                bookingsState = bookingsState,
                isArabic = isArabic,
                onTrackBooking = {
                    viewModel.selectBookingToTrack(it.id)
                    selectedTab = 1
                },
                onCancelBooking = { viewModel.cancelBooking(it.id) },
                onSubmitRating = { booking, stars, comment ->
                    viewModel.submitRating(booking, stars, comment)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerBookingTab(
    isArabic: Boolean,
    customerDisplayName: String,
    initialPickupZone: DubaiZone,
    initialDropZone: DubaiZone,
    initialCarType: CarType,
    initialCondition: CarCondition,
    pricingConfig: com.example.data.model.PricingConfig,
    isSubmitting: Boolean,
    onCreateBooking: (
        name: String,
        phone: String,
        pickupZone: DubaiZone,
        pickupAddr: String,
        dropZone: DubaiZone,
        dropAddr: String,
        carType: CarType,
        carCondition: CarCondition,
        notes: String,
        distanceKm: Double,
        priceAed: Double,
        paymentMethod: PaymentMethod
    ) -> Unit
) {
    val s = LocalAppStrings.current
    var name by remember(customerDisplayName) { mutableStateOf(customerDisplayName) }
    var phone by remember { mutableStateOf("+971 50 ") }
    var pickupZone by remember { mutableStateOf(initialPickupZone) }
    var pickupAddr by remember { mutableStateOf("") }
    var dropZone by remember { mutableStateOf(initialDropZone) }
    var dropAddr by remember { mutableStateOf("") }
    var carType by remember { mutableStateOf(initialCarType) }
    var carCondition by remember { mutableStateOf(initialCondition) }
    var notes by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CARD) }

    val distKm = remember(pickupZone, dropZone) {
        DubaiGeoCatalog.distanceKmBetween(pickupZone.lat, pickupZone.lng, dropZone.lat, dropZone.lng)
    }
    val priceAed = remember(distKm, carType, carCondition, pricingConfig) {
        DubaiGeoCatalog.calculateRecoveryPrice(distKm, carType, carCondition, pricingConfig)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderCrimson)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(s.bookNowCta, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("UAE Mobile (+971)") },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    DubaiZoneDropdown(s.pickupZoneLabel, pickupZone, isArabic) { pickupZone = it }
                    OutlinedTextField(
                        value = pickupAddr,
                        onValueChange = { pickupAddr = it },
                        label = { Text(s.pickupAddressHint) },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pickup_address_input")
                    )

                    DubaiZoneDropdown(s.dropZoneLabel, dropZone, isArabic) { dropZone = it }
                    OutlinedTextField(
                        value = dropAddr,
                        onValueChange = { dropAddr = it },
                        label = { Text(s.dropAddressHint) },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drop_address_input")
                    )

                    Text(s.carTypeLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarType.entries.forEach { t ->
                            FilterChip(
                                selected = carType == t,
                                onClick = { carType = t },
                                label = { Text(if (isArabic) t.arLabel else t.enLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = ElevatedCarbon
                                )
                            )
                        }
                    }

                    Text(s.carConditionLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarCondition.entries.forEach { c ->
                            FilterChip(
                                selected = carCondition == c,
                                onClick = { carCondition = c },
                                label = { Text(if (isArabic) c.arLabel else c.enLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = ElevatedCarbon
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(s.issueNotesHint) },
                        colors = darkInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(s.paymentMethodTitle, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(PaymentMethod.CARD, s.payCard, Icons.Default.CreditCard),
                            Triple(PaymentMethod.APPLE_PAY, s.payApplePay, Icons.Default.PhoneIphone),
                            Triple(PaymentMethod.CASH, s.payCash, Icons.Default.Payments)
                        ).forEach { (method, label, icon) ->
                            val selected = paymentMethod == method
                            Surface(
                                color = if (selected) CrimsonPrimary else ElevatedCarbon,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) ChampagneGold else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { paymentMethod = method }
                                    .testTag("payment_${method.name.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }

                    Surface(
                        color = GarnetSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${s.estimatedDistance}: $distKm ${s.kmUnit}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text("16ft Hydraulic Flatbed", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            }
                            Text(
                                text = "${priceAed.toInt()} ${s.aedCurrency}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCreateBooking(
                                name, phone, pickupZone, pickupAddr, dropZone, dropAddr,
                                carType, carCondition, notes, distKm, priceAed, paymentMethod
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_booking_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(s.confirmBookBtn, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLiveTrackTab(
    booking: Booking?,
    truck: Truck?,
    isArabic: Boolean,
    onGoToBook: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current

    // 5-second live sync pulse indicator
    var pulseSeconds by remember { mutableIntStateOf(5) }
    LaunchedEffect(booking?.id) {
        while (true) {
            delay(1000L)
            pulseSeconds = if (pulseSeconds <= 1) 5 else pulseSeconds - 1
        }
    }

    if (booking == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(44.dp))
                    Text(s.noActiveBookings, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    Button(
                        onClick = onGoToBook,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text(s.bookNowCta, color = Color.White)
                    }
                }
            }
        }
        return
    }

    val liveLat = truck?.lat ?: booking.truckLat
    val liveLng = truck?.lng ?: booking.truckLng
    val liveSpeed = truck?.speedKmh ?: booking.truckSpeedKmh
    val statusEnum = safeEnum(booking.status, BookingStatus.REQUESTED)

    val liveTruckMarker = Truck(
        id = booking.truckId.ifBlank { "live_truck" },
        truckNumber = booking.truckNumber.ifBlank { "16FT-DISPATCH" },
        pinColorHex = truck?.pinColorHex ?: "#EF5350",
        driverName = booking.driverName,
        status = if (statusEnum == BookingStatus.DELIVERED) TruckStatus.FREE.name else TruckStatus.ON_JOB.name,
        lat = liveLat,
        lng = liveLng,
        speedKmh = liveSpeed
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.liveTrackingHeader, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                StatusPill("${s.liveRefreshBadge} (${pulseSeconds}s)", CrimsonBright)
            }
        }

        // Full Dubai Live Map
        item {
            DubaiLiveMap(
                height = 290.dp,
                trucks = if (booking.driverId.isNotBlank() || truck != null) listOf(liveTruckMarker) else emptyList(),
                pickupPin = MapMarkerPin("pickup", "Pickup", booking.pickupLat, booking.pickupLng, StatusEmerald),
                dropPin = MapMarkerPin("drop", "Drop", booking.dropLat, booking.dropLng, ChampagneGold),
                isArabic = isArabic
            )
        }

        // Status Banner ("Truck Arrived" / "Car Loaded - On The Way" / "Delivered")
        item {
            val statusText = localizedStatus(statusEnum, s)
            val statusColor = when (statusEnum) {
                BookingStatus.TRUCK_ARRIVED -> StatusAmber
                BookingStatus.CAR_LOADED -> StatusBlue
                BookingStatus.DELIVERED -> StatusEmerald
                BookingStatus.CANCELLED -> TextMuted
                else -> CrimsonBright
            }
            Surface(
                color = GarnetSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, statusColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_status_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusText, style = MaterialTheme.typography.headlineMedium, color = statusColor)
                        StatusPill("${booking.priceAed.toInt()} ${s.aedCurrency}", ChampagneGold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryCell(
                            label = s.truckDistanceAway,
                            value = "${String.format("%.1f", booking.distanceToTargetKm)} ${s.kmUnit}"
                        )
                        TelemetryCell(
                            label = s.liveEtaMins,
                            value = "${booking.etaMinutes} ${s.minUnit}"
                        )
                        TelemetryCell(
                            label = s.truckSpeedLabel,
                            value = "${liveSpeed.toInt()} ${s.kmhUnit}"
                        )
                    }
                }
            }
        }

        // Driver Card & Call Button
        item {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_driver_portrait),
                            contentDescription = booking.driverName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ChampagneGold, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = booking.driverName.ifBlank { "Assigning 16ft Driver..." },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = booking.truckNumber.ifBlank { "16ft Flatbed Pending" },
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampagneGold
                            )
                            Text(
                                text = "${booking.pickupArea} → ${booking.dropArea}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    if (booking.driverPhone.isNotBlank()) {
                        Button(
                            onClick = { dialPhoneNumber(context, booking.driverPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("call_driver_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = s.callDriverBtn, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.callDriverBtn, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Uploaded Photo of Car on Truck
        if (booking.carPhotoBase64.isNotBlank()) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isArabic) "صورة سيارتك على السطحة" else "Vehicle Loaded Photo Verification",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(Modifier.height(8.dp))
                        Base64CarPhotoPreview(
                            base64 = booking.carPhotoBase64,
                            contentDescription = "Car loaded on 16ft flatbed"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerHistoryTab(
    bookingsState: UiState<List<Booking>>,
    isArabic: Boolean,
    onTrackBooking: (Booking) -> Unit,
    onCancelBooking: (Booking) -> Unit,
    onSubmitRating: (Booking, Int, String) -> Unit
) {
    val s = LocalAppStrings.current
    var ratingTarget by remember { mutableStateOf<Booking?>(null) }

    when (bookingsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CrimsonPrimary)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(bookingsState.message, color = CrimsonBright)
        }
        is UiState.Success -> {
            val bookings = bookingsState.data
            if (bookings.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(s.noActiveBookings, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bookings, key = { it.id }) { booking ->
                        val st = safeEnum(booking.status, BookingStatus.REQUESTED)
                        Surface(
                            color = CarbonSurface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
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
                                    Text(
                                        text = "${booking.pickupArea} → ${booking.dropArea}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusPill(
                                        localizedStatus(st, s),
                                        if (st == BookingStatus.DELIVERED) StatusEmerald else CrimsonBright
                                    )
                                }
                                Text(
                                    text = "${booking.carType} • ${booking.carCondition} • ${booking.priceAed.toInt()} ${s.aedCurrency} (${booking.paymentMethod})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                HorizontalDivider(color = BorderSubtle)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onTrackBooking(booking) },
                                        border = BorderStroke(1.dp, CrimsonPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(s.tabLiveTrack, color = TextPrimary)
                                    }
                                    if (st == BookingStatus.REQUESTED) {
                                        OutlinedButton(
                                            onClick = { onCancelBooking(booking) },
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                    }
                                    if (st == BookingStatus.DELIVERED && booking.rating == 0L) {
                                        Button(
                                            onClick = { ratingTarget = booking },
                                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(s.rateTripBtn, color = ObsidianBlack)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ratingTarget?.let { target ->
        RatingDialog(
            booking = target,
            onDismiss = { ratingTarget = null },
            onSubmit = { stars, comment ->
                onSubmitRating(target, stars, comment)
                ratingTarget = null
            }
        )
    }
}

@Composable
private fun RatingDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    val s = LocalAppStrings.current
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        title = { Text(s.rateTripBtn, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${booking.pickupArea} → ${booking.dropArea}", color = TextSecondary)
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { value ->
                        IconButton(onClick = { stars = value }) {
                            Icon(
                                imageVector = if (value <= stars) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$value stars",
                                tint = ChampagneGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Share your feedback") },
                    colors = darkInputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(stars, comment) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun TelemetryCell(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

fun localizedStatus(status: BookingStatus, s: AppStrings): String = when (status) {
    BookingStatus.REQUESTED -> s.statusRequested
    BookingStatus.ASSIGNED -> s.statusAssigned
    BookingStatus.EN_ROUTE_PICKUP -> s.statusEnRoute
    BookingStatus.TRUCK_ARRIVED -> s.statusTruckArrived
    BookingStatus.CAR_LOADED -> s.statusCarLoaded
    BookingStatus.DELIVERED -> s.statusDelivered
    BookingStatus.CANCELLED -> s.statusCancelled
}
