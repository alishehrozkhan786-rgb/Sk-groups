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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CarCondition
import com.example.data.model.CarType
import com.example.data.model.CustomerReview
import com.example.data.model.DubaiGeoCatalog
import com.example.data.model.DubaiZone
import com.example.data.model.PricingConfig
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.i18n.LocalAppStrings
import com.example.ui.components.DubaiLiveMap
import com.example.ui.components.MapMarkerPin
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppAndCallRow
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GarnetSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PublicHomeScreen(
    isArabic: Boolean,
    pricingConfig: PricingConfig,
    publishedReviews: List<CustomerReview>,
    onBookNow: (DubaiZone, DubaiZone, CarType, CarCondition) -> Unit
) {
    val s = LocalAppStrings.current
    var pickupZone by remember { mutableStateOf(DubaiGeoCatalog.zones[0]) }
    var dropZone by remember { mutableStateOf(DubaiGeoCatalog.zones[3]) }
    var selectedCarType by remember { mutableStateOf(CarType.LUXURY) }
    var selectedCondition by remember { mutableStateOf(CarCondition.BREAKDOWN) }

    val estDistance = remember(pickupZone, dropZone) {
        DubaiGeoCatalog.distanceKmBetween(pickupZone.lat, pickupZone.lng, dropZone.lat, dropZone.lng)
    }
    val estPrice = remember(estDistance, selectedCarType, selectedCondition, pricingConfig) {
        DubaiGeoCatalog.calculateRecoveryPrice(estDistance, selectedCarType, selectedCondition, pricingConfig)
    }

    val showcaseTruck = remember {
        listOf(
            Truck(
                id = "showcase_1",
                truckNumber = "DXB-16FT-01",
                modelName = "16ft Hydraulic Flatbed",
                pinColorHex = "#D32F2F",
                driverName = "Farman Fleet",
                status = TruckStatus.FREE.name,
                lat = 25.1580,
                lng = 55.2380,
                speedKmh = 58.0,
                locationName = "Sheikh Zayed Road"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. HERO SECTION
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_dubai_truck),
                                contentDescription = s.heroTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                ObsidianBlack.copy(alpha = 0.72f),
                                                CarbonSurface
                                            )
                                        )
                                    )
                            )
                            Surface(
                                color = CrimsonPrimary,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "16 FEET FLATBED • DUBAI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                            Text(
                                text = s.heroTitle,
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = s.heroSubtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onBookNow(pickupZone, dropZone, selectedCarType, selectedCondition) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("hero_book_now_button")
                            ) {
                                Text(s.bookNowCta, style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
                            Spacer(Modifier.height(10.dp))
                            WhatsAppAndCallRow()
                        }
                    }
                }
            }

            // 2. SERVICES SECTION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = s.servicesTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CarbonSurface,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_flatbed_luxury_car),
                            contentDescription = s.servicesTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceFeatureCard(
                            icon = Icons.Default.CarCrash,
                            title = s.svcAccidentTitle,
                            desc = s.svcAccidentDesc,
                            modifier = Modifier.weight(1f)
                        )
                        ServiceFeatureCard(
                            icon = Icons.Default.Description,
                            title = s.svcNoPapersTitle,
                            desc = s.svcNoPapersDesc,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceFeatureCard(
                            icon = Icons.Default.Build,
                            title = s.svcBreakdownTitle,
                            desc = s.svcBreakdownDesc,
                            modifier = Modifier.weight(1f)
                        )
                        ServiceFeatureCard(
                            icon = Icons.Default.DirectionsCar,
                            title = s.svcTransportTitle,
                            desc = s.svcTransportDesc,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. INTERACTIVE PRICE CALCULATOR
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("price_calculator_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = s.calculatorTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )

                        DubaiZoneDropdown(
                            label = s.pickupZoneLabel,
                            selected = pickupZone,
                            isArabic = isArabic,
                            onSelected = { pickupZone = it }
                        )

                        DubaiZoneDropdown(
                            label = s.dropZoneLabel,
                            selected = dropZone,
                            isArabic = isArabic,
                            onSelected = { dropZone = it }
                        )

                        Text(s.carTypeLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CarType.entries.forEach { type ->
                                val selected = selectedCarType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCarType = type },
                                    label = { Text(if (isArabic) type.arLabel else type.enLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = ElevatedCarbon,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }

                        Text(s.carConditionLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CarCondition.entries.forEach { cond ->
                                val selected = selectedCondition == cond
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCondition = cond },
                                    label = { Text(if (isArabic) cond.arLabel else cond.enLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = ElevatedCarbon,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }

                        Surface(
                            color = GarnetSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(s.estimatedDistance, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text(
                                        text = "$estDistance ${s.kmUnit}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(s.estimatedPrice, style = MaterialTheme.typography.bodyMedium, color = ChampagneGold)
                                    Text(
                                        text = "${estPrice.toInt()} ${s.aedCurrency}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = ChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onBookNow(pickupZone, dropZone, selectedCarType, selectedCondition) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(s.bookNowCta, color = Color.White)
                        }
                    }
                }
            }

            // 4. DUBAI LIVE COVERAGE MAP
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s.dubaiLiveMapTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    DubaiLiveMap(
                        height = 250.dp,
                        trucks = showcaseTruck,
                        pickupPin = MapMarkerPin(
                            id = "pk",
                            label = if (isArabic) pickupZone.arName.substringBefore(" /") else pickupZone.enName.substringBefore(" /"),
                            lat = pickupZone.lat,
                            lng = pickupZone.lng,
                            color = StatusEmerald
                        ),
                        dropPin = MapMarkerPin(
                            id = "dp",
                            label = if (isArabic) dropZone.arName.substringBefore(" /") else dropZone.enName.substringBefore(" /"),
                            lat = dropZone.lat,
                            lng = dropZone.lng,
                            color = ChampagneGold
                        ),
                        isArabic = isArabic
                    )
                }
            }

            // 5. WHY CHOOSE US
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.whyChooseUsTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    listOf(
                        Icons.Default.Verified to s.why1,
                        Icons.Default.Speed to s.why2,
                        Icons.Default.Shield to s.why3,
                        Icons.Default.LocationOn to s.why4
                    ).forEach { (icon, text) ->
                        Surface(
                            color = CarbonSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 6. VERIFIED REVIEWS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.reviewsTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    val displayReviews = if (publishedReviews.isNotEmpty()) publishedReviews else listOf(
                        CustomerReview(
                            id = "r1",
                            customerName = "Rashid Al Maktoum",
                            carType = "LUXURY",
                            rating = 5,
                            comment = "Picked up my Porsche GT3 on Sheikh Zayed Road in 18 mins. Live 5-second map tracking is awesome!"
                        ),
                        CustomerReview(
                            id = "r2",
                            customerName = "Tariq Mansoor",
                            carType = "SUV",
                            rating = 5,
                            comment = "No-papers showroom transfer to Al Quoz handled smoothly on their 16ft hydraulic flatbed."
                        )
                    )
                    displayReviews.take(4).forEach { rev ->
                        Surface(
                            color = CarbonSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rev.customerName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Row {
                                        repeat(rev.rating.toInt().coerceIn(1, 5)) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(rev.comment, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 7. CONTACT FOOTER
            item {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.contactTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            StatusPill("24/7 ACTIVE", StatusEmerald)
                        }
                        Text(s.contactAddress, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = com.example.ui.components.FARMAN_HOTLINE_DISPLAY,
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = BorderSubtle)
                        WhatsAppAndCallRow()
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceFeatureCard(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonPrimary.copy(alpha = 0.18f))
            ) {
                Icon(icon, contentDescription = title, tint = CrimsonBright, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DubaiZoneDropdown(
    label: String,
    selected: DubaiZone,
    isArabic: Boolean,
    onSelected: (DubaiZone) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (isArabic) selected.arName else selected.enName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = ElevatedCarbon,
                unfocusedContainerColor = ElevatedCarbon
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CarbonSurface)
        ) {
            DubaiGeoCatalog.zones.forEach { zone ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isArabic) zone.arName else zone.enName,
                            color = TextPrimary
                        )
                    },
                    onClick = {
                        onSelected(zone)
                        expanded = false
                    }
                )
            }
        }
    }
}
