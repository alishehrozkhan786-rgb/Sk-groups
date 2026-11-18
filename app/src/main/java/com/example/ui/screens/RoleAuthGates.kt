package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.BorderCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GarnetSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseUser

@Composable
fun CustomerAuthGate(
    onAuthSuccess: () -> Unit
) {
    val s = LocalAppStrings.current
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = s.customerLoginTitle,
                    tint = CrimsonBright,
                    modifier = Modifier.size(48.dp)
                )
                Text(s.customerLoginTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(s.customerLoginDesc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                errorMsg?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = CrimsonBright)
                }
                GoogleSignInButton(
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = { errorMsg = it }
                )
            }
        }
    }
}

@Composable
fun DriverAuthGate(
    currentUser: FirebaseUser?,
    isVerifying: Boolean,
    accessDenied: Boolean,
    errorMessage: String?,
    onGoogleAuthSuccess: () -> Unit,
    onVerifyDriver: (driverCode: String, passcode: String) -> Unit,
    onBackHome: () -> Unit
) {
    val s = LocalAppStrings.current
    var driverId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleError by remember { mutableStateOf<String?>(null) }

    if (accessDenied) {
        AccessDeniedScreen(
            message = errorMessage ?: s.accessDeniedDriverMsg,
            onBackHome = onBackHome
        )
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = s.driverLoginTitle,
                    tint = CrimsonBright,
                    modifier = Modifier.size(48.dp)
                )
                Text(s.driverLoginTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(s.driverLoginDesc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                if (currentUser == null) {
                    googleError?.let { Text(it, color = CrimsonBright, style = MaterialTheme.typography.bodyMedium) }
                    GoogleSignInButton(
                        onAuthSuccess = onGoogleAuthSuccess,
                        onAuthError = { googleError = it }
                    )
                } else {
                    Surface(
                        color = ElevatedCarbon,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Google: ${currentUser.email ?: currentUser.uid}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneGold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = driverId,
                        onValueChange = { driverId = it },
                        label = { Text(s.driverIdLabel) },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_id_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(s.passwordLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_password_input")
                    )

                    errorMessage?.let { Text(it, color = CrimsonBright, style = MaterialTheme.typography.bodyMedium) }

                    Button(
                        onClick = { onVerifyDriver(driverId, password) },
                        enabled = !isVerifying && driverId.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("driver_verify_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(s.verifyDriverBtn, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuthGate(
    currentUser: FirebaseUser?,
    isAuthorizedAdmin: Boolean?,
    issuedOtp: String?,
    otpError: String?,
    onGoogleAuthSuccess: () -> Unit,
    onRequestOtp: () -> Unit,
    onVerifyAdmin: (email: String, password: String, otp: String) -> Unit,
    onBackHome: () -> Unit
) {
    val s = LocalAppStrings.current
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var passcode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var googleError by remember { mutableStateOf<String?>(null) }

    if (currentUser != null && isAuthorizedAdmin == false) {
        AccessDeniedScreen(
            message = s.accessDeniedAdminMsg,
            onBackHome = onBackHome
        )
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = s.adminLoginTitle,
                    tint = ChampagneGold,
                    modifier = Modifier.size(48.dp)
                )
                Text(s.adminLoginTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(s.adminLoginDesc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                if (currentUser == null) {
                    googleError?.let { Text(it, color = CrimsonBright, style = MaterialTheme.typography.bodyMedium) }
                    GoogleSignInButton(
                        onAuthSuccess = onGoogleAuthSuccess,
                        onAuthError = { googleError = it }
                    )
                } else if (isAuthorizedAdmin == null) {
                    CircularProgressIndicator(color = CrimsonPrimary)
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(s.adminEmailLabel) },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_email_input")
                    )

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("${s.passwordLabel} (FARMAN16FT)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    OutlinedButton(
                        onClick = onRequestOtp,
                        border = BorderStroke(1.dp, ChampagneGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_send_otp_button")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(s.generateOtpBtn)
                    }

                    if (issuedOtp != null) {
                        Surface(
                            color = GarnetSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ChampagneGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Security OTP Token: $issuedOtp",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text(s.adminOtpLabel) },
                        singleLine = true,
                        colors = darkInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_otp_input")
                    )

                    otpError?.let { Text(it, color = CrimsonBright, style = MaterialTheme.typography.bodyMedium) }

                    Button(
                        onClick = { onVerifyAdmin(email, passcode, otpInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_verify_button")
                    ) {
                        Text(s.verifyAdminBtn, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun darkInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrimsonPrimary,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = ElevatedCarbon,
    unfocusedContainerColor = ElevatedCarbon
)
