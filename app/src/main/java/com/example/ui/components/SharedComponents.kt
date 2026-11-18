package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.onGoogleSignInClicked
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GarnetDeep
import com.example.ui.theme.GarnetSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AppPortal { HOME, CUSTOMER, DRIVER, ADMIN }

const val FARMAN_HOTLINE = "+971503610474"
const val FARMAN_HOTLINE_DISPLAY = "+97150 361 0474"

fun dialPhoneNumber(context: Context, phone: String) {
    val clean = phone.ifBlank { FARMAN_HOTLINE }
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openWhatsAppDispatch(context: Context, message: String = "Hello Farman Recovery 16ft, I need flatbed recovery in Dubai.") {
    val encoded = Uri.encode(message)
    val url = "https://wa.me/971503610474?text=$encoded"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openGoogleMapsNavigation(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (runCatching { context.startActivity(intent) }.isFailure) {
        runCatching { context.startActivity(fallback) }
    }
}

@Composable
fun FarmanTopBrandBar(
    activePortal: AppPortal,
    language: AppLanguage,
    isSignedIn: Boolean,
    onSelectPortal: (AppPortal) -> Unit,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    val s = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBlack, CarbonSurface)
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(CrimsonPrimary, GarnetDeep)))
                        .border(1.dp, ChampagneGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = s.brandName,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = s.brandName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = s.brandTagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = ElevatedCarbon,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderCrimson),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable { onToggleLanguage() }
                        .testTag("language_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = CrimsonBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (language == AppLanguage.EN) "عربي" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                if (isSignedIn) {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = s.signOut,
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Role Portal Switcher Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                AppPortal.HOME to s.portalHome,
                AppPortal.CUSTOMER to s.portalCustomer,
                AppPortal.DRIVER to s.portalDriver,
                AppPortal.ADMIN to s.portalAdmin
            ).forEach { (portal, label) ->
                val selected = activePortal == portal
                Surface(
                    color = if (selected) CrimsonPrimary else ElevatedCarbon,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selected) ChampagneGold.copy(alpha = 0.65f) else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clickable { onSelectPortal(portal) }
                        .testTag("portal_tab_${portal.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isLoading = false
                    onAuthError(msg)
                },
                scope = scope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = CrimsonPrimary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.signInWithGoogle, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AccessDeniedScreen(
    message: String,
    onBackHome: () -> Unit
) {
    val s = LocalAppStrings.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("access_denied_screen")
    ) {
        Surface(
            color = GarnetSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, CrimsonBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = s.accessDeniedTitle,
                        tint = CrimsonBright,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = s.accessDeniedTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrimsonBright,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onBackHome,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("access_denied_back_button")
                ) {
                    Text(s.backToHome)
                }
            }
        }
    }
}

@Composable
fun WhatsAppAndCallRow(modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = { openWhatsAppDispatch(context) },
            border = BorderStroke(1.dp, StatusEmerald),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusEmerald),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("whatsapp_button")
        ) {
            Icon(Icons.Default.Chat, contentDescription = s.whatsappBtn, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.whatsappBtn, maxLines = 1)
        }
        Button(
            onClick = { dialPhoneNumber(context, FARMAN_HOTLINE) },
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("call_247_button")
        ) {
            Icon(Icons.Default.Call, contentDescription = s.callBtn, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.callBtn, maxLines = 1)
        }
    }
}

@Composable
fun Base64CarPhotoPreview(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64) {
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderCrimson, RoundedCornerShape(14.dp))
        )
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
