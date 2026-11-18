package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FarmanLuxuryColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = GarnetDeep,
    onPrimaryContainer = TextPrimary,
    secondary = ChampagneGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF2B230B),
    onSecondaryContainer = ChampagneGold,
    tertiary = CrimsonBright,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCarbon,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderCrimson,
    error = Color(0xFFFF5252),
    onError = ObsidianBlack,
)

val FarmanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FarmanRecoveryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FarmanLuxuryColorScheme,
        typography = FarmanTypography,
        shapes = FarmanShapes,
        content = content
    )
}
