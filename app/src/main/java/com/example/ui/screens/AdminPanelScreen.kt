package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.PricingConfig
import com.example.data.model.RoutePoint
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.data.model.UserProfile
import com.example.data.model.safeEnum
import com.example.i18n.LocalAppStrings
import com.example.ui.components.DubaiLiveMap
import com.example.ui.components.StatusPill
import com.example.ui.components.parseHexColor
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
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    isArabic: Boolean
) {
    val s = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val trucksState by viewModel.trucksState.collectAsStateWithLifecycle()
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val pricingConfig by viewModel.pricingConfig.collectAsStateWithLifecycle()
    val selectedTruckId by viewModel.selectedTruckId.collectAsStateWithLifecycle()
    val selectedTruckRoute by viewModel.selectedTruckRoute.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val trucks = (trucksState as? UiState.Success)?.data ?: emptyList()
    val drivers = (driversState as? UiState.Success)?.data ?: emptyList()
    val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()
    val users = (usersState as? UiState.Success)?.data ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 780.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CarbonSurface,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ChampagneGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(s.adminFleetMapTab, maxLines = 1) },
                icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(s.adminJobsTab, maxLines = 1) },
                icon = { Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(s.adminFleetDriversTab, maxLines = 1) },
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(s.adminPricingUsersTab, maxLines = 1) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (actionMessage != null) {
            Surface(
                color = GarnetSurface,
                border = BorderStroke(1.dp, ChampagneGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.clearMessage() }
            ) {
                Text(actionMessage ?: "", color = ChampagneGold, modifier = Modifier.padding(12.dp))
            }
        }

        when (selectedTab) {
            0 -> AdminFleetMapTab(
                trucks = trucks,
                selectedTruckId = selectedTruckId,
                routeHistory = selectedTruckRoute,
                isArabic = isArabic,
                onSelectTruck = { viewModel.selectTruck(it.id) }
            )
            1 -> AdminJobsAndPaymentsTab(
                bookings = bookings,
                drivers = drivers,
                trucks = trucks,
                onAssignDriver = { b, d, t -> viewModel.assignDriverToBooking(b, d, t) },
                onMarkPaid = { viewModel.markPaid(it.id) }
            )
            2 -> AdminFleetAndDriversTab(
                trucks = trucks,
                drivers = drivers,
                onAddTruck = { num, model, color -> viewModel.addTruck(num, model, color) },
                onAddDriver = { code, pass, name, email, phone, truck ->
                    viewModel.addDriver(code, pass, name, email, phone, truck)
                },
                onToggleApprove = { drv, approved -> viewModel.setDriverApproved(drv, approved) }
            )
            3 -> AdminPricingAndUsersTab(
                pricingConfig = pricingConfig,
                users = users,
                onSavePricing = { b, pk, acc, np, lux -> viewModel.savePricing(b, pk, acc, np, lux) },
                onToggleBlock = { u, blk -> viewModel.setUserBlocked(u, blk) }
            )
        }
    }
}

@Composable
private fun AdminFleetMapTab(
    trucks: List<Truck>,
    selectedTruckId: String?,
    routeHistory: List<RoutePoint>,
    isArabic: Boolean,
    onSelectTruck: (Truck) -> Unit
) {
    val selectedTruck = trucks.firstOrNull { it.id == selectedTruckId } ?: trucks.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DubaiLiveMap(
                height = 310.dp,
                trucks = trucks,
                routeHistory = routeHistory,
                selectedTruckId = selectedTruck?.id,
                isArabic = isArabic,
                onTruckClick = onSelectTruck
            )
        }

        item {
            Text("All Fleet Trucks (${trucks.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(trucks, key = { it.id }) { truck ->
            val pinColor = parseHexColor(truck.pinColorHex, CrimsonBright)
            val isSelected = truck.id == selectedTruck?.id
            Surface(
                color = if (isSelected) GarnetSurface else CarbonSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isSelected) ChampagneGold else BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTruck(truck) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(pinColor)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${truck.truckNumber} • ${truck.driverName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("${truck.locationName} • ${truck.speedKmh.toInt()} km/h", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    StatusPill(
                        label = truck.status,
                        color = when (truck.status) {
                            TruckStatus.FREE.name -> StatusEmerald
                            TruckStatus.ON_JOB.name -> CrimsonBright
                            else -> StatusOffline
                        }
                    )
                }
            }
        }

        if (selectedTruck != null) {
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Trip History: ${selectedTruck.truckNumber} (${routeHistory.size} GPS points)",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                        }
                        if (routeHistory.isEmpty()) {
                            Text("No GPS breadcrumbs recorded for this truck yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        } else {
                            routeHistory.takeLast(8).reversed().forEach { pt ->
                                Text(
                                    text = "• ${pt.locationName} (${String.format("%.4f", pt.lat)}, ${String.format("%.4f", pt.lng)}) — ${pt.speedKmh.toInt()} km/h",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminJobsAndPaymentsTab(
    bookings: List<Booking>,
    drivers: List<DriverRecord>,
    trucks: List<Truck>,
    onAssignDriver: (Booking, DriverRecord, Truck?) -> Unit,
    onMarkPaid: (Booking) -> Unit
) {
    val s = LocalAppStrings.current
    var assignTarget by remember { mutableStateOf<Booking?>(null) }

    val totalRevenue = bookings.filter { it.status != BookingStatus.CANCELLED.name }.sumOf { it.priceAed }
    val paidRevenue = bookings.filter { it.paymentStatus == PaymentStatus.PAID.name }.sumOf { it.priceAed }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = GarnetSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(s.totalEarningsLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${totalRevenue.toInt()} ${s.aedCurrency}", style = MaterialTheme.typography.headlineLarge, color = ChampagneGold, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Collected (Paid)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${paidRevenue.toInt()} ${s.aedCurrency}", style = MaterialTheme.typography.titleLarge, color = StatusEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(bookings, key = { it.id }) { booking ->
            val st = safeEnum(booking.status, BookingStatus.REQUESTED)
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${booking.customerName} • ${booking.priceAed.toInt()} AED", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusPill(booking.status, if (st == BookingStatus.DELIVERED) StatusEmerald else CrimsonBright)
                    }
                    Text("${booking.pickupArea} → ${booking.dropArea} (${booking.carType} / ${booking.carCondition})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Payment: ${booking.paymentMethod} (${booking.paymentStatus}) | Driver: ${booking.driverName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.labelMedium, color = ChampagneGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (st == BookingStatus.REQUESTED) {
                            Button(
                                onClick = { assignTarget = booking },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(s.assignDriverBtn, color = Color.White)
                            }
                        }
                        if (booking.paymentStatus != PaymentStatus.PAID.name) {
                            OutlinedButton(
                                onClick = { onMarkPaid(booking) },
                                border = BorderStroke(1.dp, StatusEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mark Paid", color = StatusEmerald)
                            }
                        }
                    }
                }
            }
        }
    }

    assignTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { assignTarget = null },
            containerColor = CarbonSurface,
            title = { Text("Assign Driver to ${target.pickupArea}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (drivers.isEmpty()) {
                        Text("Add an approved driver in Fleet tab first.", color = TextSecondary)
                    } else {
                        drivers.filter { it.approved }.forEach { drv ->
                            val trk = trucks.firstOrNull { it.id == drv.truckId }
                            Surface(
                                color = ElevatedCarbon,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAssignDriver(target, drv, trk)
                                        assignTarget = null
                                    }
                            ) {
                                Text(
                                    text = "${drv.name} (${drv.driverCode}) • ${drv.truckNumber}",
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assignTarget = null }) { Text("Close", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun AdminFleetAndDriversTab(
    trucks: List<Truck>,
    drivers: List<DriverRecord>,
    onAddTruck: (String, String, String) -> Unit,
    onAddDriver: (String, String, String, String, String, Truck?) -> Unit,
    onToggleApprove: (DriverRecord, Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    var showAddTruck by remember { mutableStateOf(false) }
    var showAddDriver by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showAddTruck = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.addTruckBtn, maxLines = 1)
                }
                Button(
                    onClick = { showAddDriver = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.addDriverBtn, color = ObsidianBlack, maxLines = 1)
                }
            }
        }

        item {
            Text("Drivers (${drivers.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(drivers, key = { it.id }) { drv ->
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${drv.name} (${drv.driverCode})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${drv.email} • ${drv.truckNumber}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Pass: ${drv.passcode}", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                    }
                    OutlinedButton(
                        onClick = { onToggleApprove(drv, !drv.approved) },
                        border = BorderStroke(1.dp, if (drv.approved) StatusEmerald else CrimsonBright)
                    ) {
                        Text(if (drv.approved) "Approved" else s.approveLabel, color = if (drv.approved) StatusEmerald else CrimsonBright)
                    }
                }
            }
        }
    }

    if (showAddTruck) {
        var plate by remember { mutableStateOf("DXB-16FT-02") }
        var model by remember { mutableStateOf("Isuzu 16ft Hydraulic Flatbed") }
        var selectedColor by remember { mutableStateOf("#D4AF37") }
        val colors = listOf("#D32F2F", "#D4AF37", "#42A5F5", "#66BB6A")

        AlertDialog(
            onDismissRequest = { showAddTruck = false },
            containerColor = CarbonSurface,
            title = { Text(s.addTruckBtn, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Truck Plate") }, colors = darkInputColors())
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, colors = darkInputColors())
                    Text("Map Pin Color", color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(2.dp, if (selectedColor == hex) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTruck(plate, model, selectedColor)
                        showAddTruck = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save Truck") }
            },
            dismissButton = { TextButton(onClick = { showAddTruck = false }) { Text("Cancel") } }
        )
    }

    if (showAddDriver) {
        var code by remember { mutableStateOf("DRV-101") }
        var pass by remember { mutableStateOf("16FT2026") }
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+97150") }
        var pickedTruck by remember { mutableStateOf(trucks.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showAddDriver = false },
            containerColor = CarbonSurface,
            title = { Text(s.addDriverBtn, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Driver ID") }, colors = darkInputColors())
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Driver Password") }, colors = darkInputColors())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Driver Full Name") }, colors = darkInputColors())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Driver Google Email") }, colors = darkInputColors())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Driver Phone") }, colors = darkInputColors())
                    if (trucks.isNotEmpty()) {
                        Text("Assigned Truck: ${pickedTruck?.truckNumber ?: "None"}", color = ChampagneGold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            trucks.take(3).forEach { t ->
                                OutlinedButton(onClick = { pickedTruck = t }) {
                                    Text(t.truckNumber, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddDriver(code, pass, name, email, phone, pickedTruck)
                        showAddDriver = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Create Driver") }
            },
            dismissButton = { TextButton(onClick = { showAddDriver = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminPricingAndUsersTab(
    pricingConfig: PricingConfig,
    users: List<UserProfile>,
    onSavePricing: (Double, Double, Double, Double, Double) -> Unit,
    onToggleBlock: (UserProfile, Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    var baseFee by remember(pricingConfig) { mutableStateOf(pricingConfig.baseFeeAed.toString()) }
    var perKm by remember(pricingConfig) { mutableStateOf(pricingConfig.perKmAed.toString()) }
    var accident by remember(pricingConfig) { mutableStateOf(pricingConfig.accidentSurchargeAed.toString()) }
    var noPapers by remember(pricingConfig) { mutableStateOf(pricingConfig.noPapersSurchargeAed.toString()) }
    var luxuryMult by remember(pricingConfig) { mutableStateOf(pricingConfig.luxuryMultiplier.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dubai 16ft Price Matrix (AED)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    OutlinedTextField(value = baseFee, onValueChange = { baseFee = it }, label = { Text("Base Flatbed Fee (AED)") }, colors = darkInputColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = perKm, onValueChange = { perKm = it }, label = { Text("Rate Per Km (AED)") }, colors = darkInputColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = accident, onValueChange = { accident = it }, label = { Text("Accident Recovery Surcharge (AED)") }, colors = darkInputColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noPapers, onValueChange = { noPapers = it }, label = { Text("No-Papers Handling Fee (AED)") }, colors = darkInputColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = luxuryMult, onValueChange = { luxuryMult = it }, label = { Text("Luxury Car Multiplier (e.g. 1.35)") }, colors = darkInputColors(), modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onSavePricing(
                                baseFee.toDoubleOrNull() ?: 150.0,
                                perKm.toDoubleOrNull() ?: 7.0,
                                accident.toDoubleOrNull() ?: 90.0,
                                noPapers.toDoubleOrNull() ?: 120.0,
                                luxuryMult.toDoubleOrNull() ?: 1.35
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(s.savePricingBtn, color = Color.White)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Registered Users (${users.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        }

        items(users, key = { it.id }) { u ->
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${u.email} • ${u.role}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = { onToggleBlock(u, !u.blocked) },
                        border = BorderStroke(1.dp, if (u.blocked) StatusEmerald else CrimsonBright)
                    ) {
                        Text(if (u.blocked) s.unblockLabel else s.blockLabel, color = if (u.blocked) StatusEmerald else CrimsonBright)
                    }
                }
            }
        }
    }
}
