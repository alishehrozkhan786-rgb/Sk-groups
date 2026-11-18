package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RecoveryRepository
import com.example.data.model.Booking
import com.example.data.model.DriverRecord
import com.example.data.model.PricingConfig
import com.example.data.model.RoutePoint
import com.example.data.model.Truck
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class AdminViewModel(
    private val repository: RecoveryRepository,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "AdminVM"
        const val STOP_TIMEOUT = 5000L
        const val ADMIN_PASSCODE = "FARMAN16FT"
    }

    private val _isAuthorizedAdmin = MutableStateFlow<Boolean?>(null)
    val isAuthorizedAdmin = _isAuthorizedAdmin.asStateFlow()

    private val _otpVerified = MutableStateFlow(false)
    val otpVerified = _otpVerified.asStateFlow()

    private val _issuedOtp = MutableStateFlow<String?>(null)
    val issuedOtp = _issuedOtp.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError = _otpError.asStateFlow()

    private val _selectedTruckId = MutableStateFlow<String?>(null)
    val selectedTruckId = _selectedTruckId.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    val trucksState: StateFlow<UiState<List<Truck>>> = repository.observeAllTrucks()
        .map<List<Truck>, UiState<List<Truck>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error((fe ?: error).message ?: "Failed to load fleet"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val driversState: StateFlow<UiState<List<DriverRecord>>> = repository.observeAllDrivers()
        .map<List<DriverRecord>, UiState<List<DriverRecord>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error((fe ?: error).message ?: "Failed to load drivers"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val bookingsState: StateFlow<UiState<List<Booking>>> = repository.observeAllBookings()
        .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error((fe ?: error).message ?: "Failed to load bookings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val usersState: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error((fe ?: error).message ?: "Failed to load users"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val pricingConfig: StateFlow<PricingConfig> = repository.observePricingConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), PricingConfig())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTruckRoute: StateFlow<List<RoutePoint>> = _selectedTruckId
        .flatMapLatest { truckId ->
            if (truckId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeTruckRouteHistory(truckId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun checkAdminAccess(user: FirebaseUser) {
        viewModelScope.launch {
            val allowed = repository.checkIsAdmin(user)
            _isAuthorizedAdmin.value = allowed
            if (allowed) {
                repository.seedInitialFleetIfEmpty()
            }
        }
    }

    fun generateAdminOtp() {
        val code = Random.nextInt(100000, 999999).toString()
        _issuedOtp.value = code
        _otpError.value = null
    }

    fun verifyAdminCredentials(email: String, password: String, otp: String, user: FirebaseUser) {
        if (_isAuthorizedAdmin.value != true) {
            _otpError.value = "Access Denied: Google account is not an authorized Admin."
            return
        }
        if (!user.email.equals(email.trim(), ignoreCase = true)) {
            _otpError.value = "Email must match authenticated Admin Google account."
            return
        }
        if (password.trim() != ADMIN_PASSCODE) {
            _otpError.value = "Invalid Admin Passcode. (Default: FARMAN16FT)"
            return
        }
        val expected = _issuedOtp.value
        if (expected == null || otp.trim() != expected) {
            _otpError.value = "Invalid 6-digit OTP code. Tap Send Verification OTP first."
            return
        }
        _otpError.value = null
        _otpVerified.value = true
    }

    fun selectTruck(truckId: String?) {
        _selectedTruckId.value = truckId
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun addTruck(truckNumber: String, modelName: String, pinColorHex: String) {
        if (truckNumber.isBlank()) return
        viewModelScope.launch {
            repository.addTruck(truckNumber, modelName, pinColorHex)
                .onSuccess { _actionMessage.value = "Truck $truckNumber added to Dubai fleet." }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun addDriver(
        driverCode: String,
        passcode: String,
        name: String,
        email: String,
        phone: String,
        truck: Truck?
    ) {
        if (driverCode.isBlank() || passcode.length < 4 || name.isBlank() || email.isBlank()) {
            _actionMessage.value = "Fill Driver ID, Password (min 4 chars), Name, and Google Email."
            return
        }
        viewModelScope.launch {
            repository.addDriver(driverCode, passcode, name, email, phone, truck)
                .onSuccess { _actionMessage.value = "Driver $driverCode provisioned and approved." }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun setDriverApproved(driver: DriverRecord, approved: Boolean) {
        viewModelScope.launch {
            repository.approveDriver(driver, approved)
                .onSuccess { _actionMessage.value = if (approved) "Driver approved" else "Driver suspended" }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun assignDriverToBooking(booking: Booking, driver: DriverRecord, truck: Truck?) {
        viewModelScope.launch {
            repository.assignBookingToDriver(booking, driver, truck)
                .onSuccess { _actionMessage.value = "Assigned ${driver.name} to ${booking.pickupArea}" }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun savePricing(base: Double, perKm: Double, accident: Double, noPapers: Double, luxuryMult: Double) {
        viewModelScope.launch {
            repository.updatePricingConfig(base, perKm, accident, noPapers, luxuryMult)
                .onSuccess { _actionMessage.value = "Dubai pricing matrix updated." }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun setUserBlocked(user: UserProfile, blocked: Boolean) {
        viewModelScope.launch {
            repository.toggleUserBlocked(user.userId, blocked)
                .onSuccess { _actionMessage.value = if (blocked) "User blocked" else "User unblocked" }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun markPaid(bookingId: String) {
        viewModelScope.launch {
            repository.markBookingPaid(bookingId)
                .onSuccess { _actionMessage.value = "Payment marked PAID." }
                .onFailure { _actionMessage.value = it.message }
        }
    }
}
