package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.RecoveryRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.model.CarCondition
import com.example.data.model.CarType
import com.example.data.model.DubaiGeoCatalog
import com.example.data.model.PricingConfig
import com.example.i18n.AppLanguage
import com.example.i18n.ArabicStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.components.AppPortal
import com.example.ui.components.FarmanTopBrandBar
import com.example.ui.screens.AdminAuthGate
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CustomerAuthGate
import com.example.ui.screens.CustomerPortalScreen
import com.example.ui.screens.DriverAuthGate
import com.example.ui.screens.DriverPortalScreen
import com.example.ui.screens.PublicHomeScreen
import com.example.ui.theme.FarmanRecoveryTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.DriverViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarmanRecoveryTheme {
                FarmanRecoveryApp()
            }
        }
    }
}

@Composable
fun FarmanRecoveryApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var language by remember { mutableStateOf(AppLanguage.EN) }
    val strings = if (language == AppLanguage.AR) ArabicStrings else EnglishStrings
    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    var activePortal by remember { mutableStateOf(AppPortal.HOME) }
    var draftPickupZone by remember { mutableStateOf(DubaiGeoCatalog.zones[0]) }
    var draftDropZone by remember { mutableStateOf(DubaiGeoCatalog.zones[3]) }
    var draftCarType by remember { mutableStateOf(CarType.LUXURY) }
    var draftCondition by remember { mutableStateOf(CarCondition.BREAKDOWN) }

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    // Silent auto sign-in on startup (will quietly yield if no authorized credential yet)
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    if (activePortal != AppPortal.HOME) {
        BackHandler { activePortal = AppPortal.HOME }
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FarmanTopBrandBar(
                    activePortal = activePortal,
                    language = language,
                    isSignedIn = currentUser != null,
                    onSelectPortal = { activePortal = it },
                    onToggleLanguage = {
                        language = if (language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
                    },
                    onSignOut = {
                        signOut(credentialManager, scope) {
                            activePortal = AppPortal.HOME
                        }
                    }
                )

                val user = currentUser
                if (user == null) {
                    // Unauthenticated session: NO Firestore listeners or queries executed
                    when (activePortal) {
                        AppPortal.HOME -> PublicHomeScreen(
                            isArabic = language == AppLanguage.AR,
                            pricingConfig = PricingConfig(),
                            publishedReviews = emptyList(),
                            onBookNow = { pk, dp, ct, cond ->
                                draftPickupZone = pk
                                draftDropZone = dp
                                draftCarType = ct
                                draftCondition = cond
                                activePortal = AppPortal.CUSTOMER
                            }
                        )
                        AppPortal.CUSTOMER -> CustomerAuthGate(onAuthSuccess = {})
                        AppPortal.DRIVER -> DriverAuthGate(
                            currentUser = null,
                            isVerifying = false,
                            accessDenied = false,
                            errorMessage = null,
                            onGoogleAuthSuccess = {},
                            onVerifyDriver = { _, _ -> },
                            onBackHome = { activePortal = AppPortal.HOME }
                        )
                        AppPortal.ADMIN -> AdminAuthGate(
                            currentUser = null,
                            isAuthorizedAdmin = null,
                            issuedOtp = null,
                            otpError = null,
                            onGoogleAuthSuccess = {},
                            onRequestOtp = {},
                            onVerifyAdmin = { _, _, _ -> },
                            onBackHome = { activePortal = AppPortal.HOME }
                        )
                    }
                } else {
                    // Authenticated session: key everything by uid so account switches cleanly reset ViewModels & listeners
                    key(user.uid) {
                        AuthenticatedPortalRouter(
                            user = user,
                            activePortal = activePortal,
                            isArabic = language == AppLanguage.AR,
                            languageCode = language.code,
                            draftPickupZone = draftPickupZone,
                            draftDropZone = draftDropZone,
                            draftCarType = draftCarType,
                            draftCondition = draftCondition,
                            onChangePortal = { activePortal = it },
                            onBookFromHome = { pk, dp, ct, cond ->
                                draftPickupZone = pk
                                draftDropZone = dp
                                draftCarType = ct
                                draftCondition = cond
                                activePortal = AppPortal.CUSTOMER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedPortalRouter(
    user: FirebaseUser,
    activePortal: AppPortal,
    isArabic: Boolean,
    languageCode: String,
    draftPickupZone: com.example.data.model.DubaiZone,
    draftDropZone: com.example.data.model.DubaiZone,
    draftCarType: CarType,
    draftCondition: CarCondition,
    onChangePortal: (AppPortal) -> Unit,
    onBookFromHome: (
        com.example.data.model.DubaiZone,
        com.example.data.model.DubaiZone,
        CarType,
        CarCondition
    ) -> Unit
) {
    val context = LocalContext.current
    val repo = remember(context) { RecoveryRepository(context) }

    // Ensure user profile exists on sign-in & observe blocked state
    LaunchedEffect(user.uid) {
        repo.ensureUserProfile(user, languageCode)
    }
    val userProfile by repo.observeUserProfile(user.uid).collectAsStateWithLifecycle(initialValue = null)

    if (userProfile?.blocked == true && activePortal != AppPortal.HOME) {
        AccessDeniedScreen(
            message = "Your account has been suspended by Farman Recovery Administration.",
            onBackHome = { onChangePortal(AppPortal.HOME) }
        )
        return
    }

    val customerVm: CustomerViewModel = viewModel(
        key = "cust_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                CustomerViewModel(RecoveryRepository(FirebaseFirestore.getInstance(dbId)), user.uid)
            }
        }
    )

    when (activePortal) {
        AppPortal.HOME -> {
            val pricing by customerVm.pricingConfig.collectAsStateWithLifecycle()
            val reviews by customerVm.reviewsState.collectAsStateWithLifecycle()
            PublicHomeScreen(
                isArabic = isArabic,
                pricingConfig = pricing,
                publishedReviews = reviews,
                onBookNow = onBookFromHome
            )
        }
        AppPortal.CUSTOMER -> {
            CustomerPortalScreen(
                viewModel = customerVm,
                isArabic = isArabic,
                customerDisplayName = user.displayName ?: "Dubai Client",
                initialPickupZone = draftPickupZone,
                initialDropZone = draftDropZone,
                initialCarType = draftCarType,
                initialCondition = draftCondition
            )
        }
        AppPortal.DRIVER -> {
            val driverVm: DriverViewModel = viewModel(
                key = "drv_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        DriverViewModel(RecoveryRepository(FirebaseFirestore.getInstance(dbId)), user.uid)
                    }
                }
            )
            LaunchedEffect(user.uid) {
                driverVm.restoreDriverSession(user)
            }
            val verifiedDriver by driverVm.verifiedDriver.collectAsStateWithLifecycle()
            val isVerifying by driverVm.isVerifying.collectAsStateWithLifecycle()
            val accessDenied by driverVm.accessDenied.collectAsStateWithLifecycle()
            val authError by driverVm.authError.collectAsStateWithLifecycle()

            if (verifiedDriver == null) {
                DriverAuthGate(
                    currentUser = user,
                    isVerifying = isVerifying,
                    accessDenied = accessDenied,
                    errorMessage = authError,
                    onGoogleAuthSuccess = {},
                    onVerifyDriver = { code, pass -> driverVm.verifyDriverLogin(code, pass, user) },
                    onBackHome = { onChangePortal(AppPortal.HOME) }
                )
            } else {
                DriverPortalScreen(
                    viewModel = driverVm,
                    driver = verifiedDriver!!,
                    isArabic = isArabic
                )
            }
        }
        AppPortal.ADMIN -> {
            val adminVm: AdminViewModel = viewModel(
                key = "adm_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        AdminViewModel(RecoveryRepository(FirebaseFirestore.getInstance(dbId)), user.uid)
                    }
                }
            )
            LaunchedEffect(user.uid) {
                adminVm.checkAdminAccess(user)
            }
            val isAuthorizedAdmin by adminVm.isAuthorizedAdmin.collectAsStateWithLifecycle()
            val otpVerified by adminVm.otpVerified.collectAsStateWithLifecycle()
            val issuedOtp by adminVm.issuedOtp.collectAsStateWithLifecycle()
            val otpError by adminVm.otpError.collectAsStateWithLifecycle()

            if (isAuthorizedAdmin == true && otpVerified) {
                AdminPanelScreen(viewModel = adminVm, isArabic = isArabic)
            } else {
                AdminAuthGate(
                    currentUser = user,
                    isAuthorizedAdmin = isAuthorizedAdmin,
                    issuedOtp = issuedOtp,
                    otpError = otpError,
                    onGoogleAuthSuccess = {},
                    onRequestOtp = { adminVm.generateAdminOtp() },
                    onVerifyAdmin = { email, pass, otp -> adminVm.verifyAdminCredentials(email, pass, otp, user) },
                    onBackHome = { onChangePortal(AppPortal.HOME) }
                )
            }
        }
    }
}
