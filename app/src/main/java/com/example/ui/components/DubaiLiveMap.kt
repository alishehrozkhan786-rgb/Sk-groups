package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DubaiGeoCatalog
import com.example.data.model.RoutePoint
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

data class MapMarkerPin(
    val id: String,
    val label: String,
    val lat: Double,
    val lng: Double,
    val color: Color,
    val subtitle: String = ""
)

private const val MIN_LAT = 24.95
private const val MAX_LAT = 25.33
private const val MIN_LNG = 54.98
private const val MAX_LNG = 55.44

@Composable
fun DubaiLiveMap(
    modifier: Modifier = Modifier,
    height: Dp = 280.dp,
    trucks: List<Truck> = emptyList(),
    pickupPin: MapMarkerPin? = null,
    dropPin: MapMarkerPin? = null,
    routeHistory: List<RoutePoint> = emptyList(),
    selectedTruckId: String? = null,
    isArabic: Boolean = false,
    showDistricts: Boolean = true,
    onTruckClick: ((Truck) -> Unit)? = null
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Smoothly animate primary truck coordinates
    val firstTruck = trucks.firstOrNull()
    val animatedTruckLat by animateFloatAsState(
        targetValue = (firstTruck?.lat ?: 25.1375).toFloat(),
        animationSpec = spring(stiffness = 120f),
        label = "truckLat"
    )
    val animatedTruckLng by animateFloatAsState(
        targetValue = (firstTruck?.lng ?: 55.2255).toFloat(),
        animationSpec = spring(stiffness = 120f),
        label = "truckLng"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianBlack)
            .border(1.dp, BorderCrimson, RoundedCornerShape(20.dp))
            .testTag("dubai_live_map")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trucks) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 3.0f)
                        val maxPan = (zoom - 1f) * 280f
                        panOffset = Offset(
                            x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                            y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                        )
                    }
                }
        ) {
            fun geoToScreen(lat: Double, lng: Double): Offset {
                val nx = ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)).toFloat()
                val ny = (1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)).toFloat()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseX = nx * size.width
                val baseY = ny * size.height
                return Offset(
                    x = cx + (baseX - cx) * zoom + panOffset.x,
                    y = cy + (baseY - cy) * zoom + panOffset.y
                )
            }

            // 1. Dark Arabian Gulf Water + Mainland Background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF070C14), Color(0xFF0E0E13))
                )
            )

            // Draw stylized Dubai coastline
            val coastPoints = listOf(
                24.96 to 54.99,
                25.06 to 55.11,
                25.11 to 55.14,
                25.14 to 55.19,
                25.21 to 55.24,
                25.27 to 55.29,
                25.31 to 55.34,
                25.33 to 55.44,
                24.95 to 55.44,
                24.95 to 54.99
            )
            val landPath = Path().apply {
                coastPoints.forEachIndexed { idx, (lat, lng) ->
                    val pt = geoToScreen(lat, lng)
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            drawPath(landPath, color = Color(0xFF131319))
            drawPath(
                landPath,
                color = Color(0xFF282834),
                style = Stroke(width = 1.5f)
            )

            // Palm Jumeirah silhouette
            val palmCenter = geoToScreen(25.1124, 55.1390)
            drawCircle(
                color = Color(0xFF1E1E28),
                radius = 16f * zoom,
                center = palmCenter
            )
            drawCircle(
                color = Color(0xFF3A2426),
                radius = 16f * zoom,
                center = palmCenter,
                style = Stroke(width = 1.5f)
            )

            // 2. Subtle Luxury Grid Lines
            val gridStep = 48f
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0xFF1A1A22), Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
                x += gridStep
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0xFF1A1A22), Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                y += gridStep
            }

            // 3. Major Dubai Highway Arteries (Sheikh Zayed Rd E11, Al Khail Rd E44)
            val szrCoords = listOf(
                24.9857 to 55.0273,
                25.0805 to 55.1403,
                25.1181 to 55.2006,
                25.1851 to 55.2662,
                25.1972 to 55.2744,
                25.2532 to 55.3188
            )
            drawHighwayPath(szrCoords.map { geoToScreen(it.first, it.second) }, Color(0xFF5D181C), 3.2f)

            val alKhailCoords = listOf(
                25.0472 to 55.2371,
                25.1375 to 55.2255,
                25.1851 to 55.2900,
                25.2532 to 55.3657
            )
            drawHighwayPath(alKhailCoords.map { geoToScreen(it.first, it.second) }, Color(0xFF31313D), 2.4f)

            // 4. Dubai District Nodes & Labels
            if (showDistricts) {
                DubaiGeoCatalog.zones.forEach { zone ->
                    val pt = geoToScreen(zone.lat, zone.lng)
                    if (pt.x in 12f..(size.width - 12f) && pt.y in 12f..(size.height - 12f)) {
                        drawCircle(Color(0xFF3E3E4C), radius = 3.5f, center = pt)
                        val shortName = (if (isArabic) zone.arName else zone.enName)
                            .substringBefore(" /")
                        drawMapLabel(
                            textMeasurer = textMeasurer,
                            text = shortName,
                            topLeft = Offset(pt.x + 6f, pt.y - 8f),
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // 5. Route History Breadcrumb Trail (Admin truck inspection)
            if (routeHistory.size >= 2) {
                val trailPath = Path().apply {
                    routeHistory.forEachIndexed { index, rp ->
                        val p = geoToScreen(rp.lat, rp.lng)
                        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                }
                drawPath(
                    path = trailPath,
                    color = ChampagneGold,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                routeHistory.forEach { rp ->
                    drawCircle(ChampagneGold, radius = 3.5f, center = geoToScreen(rp.lat, rp.lng))
                }
            }

            // 6. Active Pickup -> Dropoff Dashed Route
            if (pickupPin != null && dropPin != null) {
                val pStart = geoToScreen(pickupPin.lat, pickupPin.lng)
                val pEnd = geoToScreen(dropPin.lat, dropPin.lng)
                drawLine(
                    color = CrimsonBright.copy(alpha = 0.85f),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            }

            // 7. Pickup & Dropoff Pins
            pickupPin?.let { pin ->
                val p = geoToScreen(pin.lat, pin.lng)
                drawCircle(pin.color.copy(alpha = 0.25f), radius = 18f, center = p)
                drawCircle(pin.color, radius = 8f, center = p)
                drawCircle(Color.White, radius = 3.2f, center = p)
                drawMapLabel(textMeasurer, pin.label, Offset(p.x + 10f, p.y - 10f), TextPrimary, 10.sp, bold = true)
            }
            dropPin?.let { pin ->
                val p = geoToScreen(pin.lat, pin.lng)
                drawCircle(pin.color.copy(alpha = 0.25f), radius = 18f, center = p)
                drawCircle(pin.color, radius = 8f, center = p)
                drawCircle(ObsidianBlack, radius = 3.2f, center = p)
                drawMapLabel(textMeasurer, pin.label, Offset(p.x + 10f, p.y - 10f), ChampagneGold, 10.sp, bold = true)
            }

            // 8. Live 16ft Flatbed Truck Pins
            trucks.forEachIndexed { idx, truck ->
                val tLat = if (idx == 0) animatedTruckLat.toDouble() else truck.lat
                val tLng = if (idx == 0) animatedTruckLng.toDouble() else truck.lng
                val center = geoToScreen(tLat, tLng)

                val pinColor = parseHexColor(truck.pinColorHex, CrimsonBright)
                val statusRingColor = when (truck.status) {
                    TruckStatus.FREE.name -> StatusEmerald
                    TruckStatus.ON_JOB.name -> CrimsonBright
                    else -> StatusOffline
                }
                val isSelected = truck.id == selectedTruckId

                if (truck.status != TruckStatus.OFFLINE.name) {
                    drawCircle(
                        color = statusRingColor.copy(alpha = pulseAlpha),
                        radius = pulseRadius * if (isSelected) 1.3f else 1f,
                        center = center
                    )
                }

                if (isSelected) {
                    drawCircle(
                        color = ChampagneGold,
                        radius = 18f,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Truck Core Marker
                drawCircle(color = ObsidianBlack, radius = 13f, center = center)
                drawCircle(color = pinColor, radius = 10f, center = center)
                drawCircle(color = Color.White, radius = 4f, center = center)

                val badgeText = "${truck.truckNumber} • ${truck.speedKmh.toInt()} km/h"
                drawMapLabel(
                    textMeasurer = textMeasurer,
                    text = badgeText,
                    topLeft = Offset(center.x + 14f, center.y - 12f),
                    color = if (isSelected) ChampagneGold else TextPrimary,
                    fontSize = 10.sp,
                    bold = true
                )
            }
        }

        // Top-Start Live Dubai Badge
        Surface(
            color = CarbonSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CrimsonBright)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "دبي مباشر • ١٦ قدم" else "DUBAI GPS • 16FT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
        }

        // Zoom Controls
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            MapControlButton(
                icon = Icons.Default.Add,
                desc = "Zoom in",
                onClick = { zoom = (zoom + 0.35f).coerceAtMost(3.0f) }
            )
            MapControlButton(
                icon = Icons.Default.Remove,
                desc = "Zoom out",
                onClick = {
                    zoom = (zoom - 0.35f).coerceAtLeast(1.0f)
                    if (zoom <= 1.05f) panOffset = Offset.Zero
                }
            )
            MapControlButton(
                icon = Icons.Default.MyLocation,
                desc = "Reset view",
                onClick = {
                    zoom = 1f
                    panOffset = Offset.Zero
                }
            )
        }

        // Interactive Truck Selector Strip (for Admin multi-truck inspection)
        if (onTruckClick != null && trucks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                trucks.take(4).forEach { truck ->
                    val selected = truck.id == selectedTruckId
                    val dotColor = parseHexColor(truck.pinColorHex, CrimsonBright)
                    Surface(
                        color = if (selected) ElevatedCarbon else CarbonSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) ChampagneGold else BorderSubtle
                        ),
                        modifier = Modifier.clickable { onTruckClick(truck) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = truck.truckNumber,
                                tint = dotColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = truck.truckNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ChampagneGold else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        color = CarbonSurface.copy(alpha = 0.92f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.size(34.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = desc, tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun DrawScope.drawHighwayPath(points: List<Offset>, color: Color, strokeWidth: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        points.forEachIndexed { i, p ->
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawMapLabel(
    textMeasurer: TextMeasurer,
    text: String,
    topLeft: Offset,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean = false
) {
    val result = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    )
    if (topLeft.x >= 0 && topLeft.y >= 0 &&
        topLeft.x + result.size.width <= size.width &&
        topLeft.y + result.size.height <= size.height
    ) {
        drawText(result, topLeft = topLeft)
    }
}

fun parseHexColor(hex: String, fallback: Color = CrimsonPrimary): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
