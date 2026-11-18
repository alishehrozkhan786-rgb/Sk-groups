package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RecoveryRepository
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverRecord
import com.example.data.model.JobDispatch
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.location.DriverGpsReading
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DriverViewModel(
    private val repository: RecoveryRepository,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "DriverVM"
        const val STOP_TIMEOUT = 5000L
    }

    private val _verifiedDriver = MutableStateFlow<DriverRecord?>(null)
    val verifiedDriver = _verifiedDriver.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _accessDenied = MutableStateFlow(false)
    val accessDenied = _accessDenied.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying = _isVerifying.asStateFlow()

    private val _lastGpsReading = MutableStateFlow<DriverGpsReading?>(null)
    val lastGpsReading = _lastGpsReading.asStateFlow()

    private val _lastPingTimestampMs = MutableStateFlow(0L)
    val lastPingTimestampMs = _lastPingTimestampMs.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    val openDispatchesState: StateFlow<UiState<List<JobDispatch>>> = repository.observeOpenJobDispatches()
        .map<List<JobDispatch>, UiState<List<JobDispatch>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load job requests"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val driverBookingsState: StateFlow<UiState<List<Booking>>> = repository.observeDriverBookings(currentUserId)
        .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load driver jobs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val driverTruck: StateFlow<Truck?> = repository.observeDriverTruck(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun restoreDriverSession(user: FirebaseUser) {
        viewModelScope.launch {
            val existing = repository.checkExistingDriverSession(user)
            if (existing != null) {
                _verifiedDriver.value = existing
                _accessDenied.value = false
            }
        }
    }

    fun verifyDriverLogin(driverCode: String, passcode: String, user: FirebaseUser) {
        viewModelScope.launch {
            _isVerifying.value = true
            _authError.value = null
            _accessDenied.value = false
            val res = repository.verifyAndActivateDriver(driverCode, passcode, user)
            _isVerifying.value = false
            res.onSuccess { record ->
                _verifiedDriver.value = record
                _accessDenied.value = false
            }.onFailure { err ->
                Log.w(TAG, "Driver verification failed: ${err.message}")
                _accessDenied.value = true
                _authError.value = err.message ?: "Access Denied"
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun onLiveGpsTick(reading: DriverGpsReading, activeBooking: Booking?) {
        _lastGpsReading.value = reading
        val now = System.currentTimeMillis()
        if (now - _lastPingTimestampMs.value < 4500L) return
        _lastPingTimestampMs.value = now

        val drv = _verifiedDriver.value ?: return
        val truck = driverTruck.value
        val truckId = truck?.id?.ifBlank { drv.truckId } ?: drv.truckId
        val truckNumber = truck?.truckNumber?.ifBlank { drv.truckNumber } ?: drv.truckNumber

        viewModelScope.launch {
            repository.pushDriverLiveGps(
                truckId = truckId,
                truckNumber = truckNumber,
                activeBooking = activeBooking,
                lat = reading.lat,
                lng = reading.lng,
                speedKmh = reading.speedKmh,
                headingDeg = reading.bearingDeg
            )
        }
    }

    fun manualGpsPing(lat: Double, lng: Double, speedKmh: Double, activeBooking: Booking?) {
        val drv = _verifiedDriver.value ?: return
        val truck = driverTruck.value
        val truckId = truck?.id?.ifBlank { drv.truckId } ?: drv.truckId
        val truckNumber = truck?.truckNumber?.ifBlank { drv.truckNumber } ?: drv.truckNumber
        _lastPingTimestampMs.value = System.currentTimeMillis()
        _lastGpsReading.value = DriverGpsReading(lat, lng, speedKmh, 45.0)
        viewModelScope.launch {
            repository.pushDriverLiveGps(truckId, truckNumber, activeBooking, lat, lng, speedKmh, 45.0)
                .onSuccess { _actionMessage.value = "Live GPS ping broadcasted to Customer & Admin." }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun acceptJob(dispatch: JobDispatch) {
        val drv = _verifiedDriver.value ?: return
        val lat = _lastGpsReading.value?.lat ?: driverTruck.value?.lat ?: 25.1375
        val lng = _lastGpsReading.value?.lng ?: driverTruck.value?.lng ?: 55.2255
        viewModelScope.launch {
            repository.acceptJobDispatch(dispatch, drv, lat, lng)
                .onSuccess { _actionMessage.value = "Job accepted! Navigate to pickup." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to accept job" }
        }
    }

    fun rejectJob(bookingId: String) {
        viewModelScope.launch {
            repository.rejectJobDispatch(bookingId)
                .onSuccess { _actionMessage.value = "Job request declined." }
                .onFailure { _actionMessage.value = it.message }
        }
    }

    fun advanceJobStatus(booking: Booking, nextStatus: BookingStatus, photoBase64: String? = null) {
        viewModelScope.launch {
            repository.advanceBookingStatus(booking, nextStatus, photoBase64)
                .onSuccess { _actionMessage.value = "Status updated: ${nextStatus.name}" }
                .onFailure { _actionMessage.value = it.message ?: "Status update failed" }
        }
    }

    fun setDriverAvailability(status: TruckStatus) {
        val drv = _verifiedDriver.value ?: return
        viewModelScope.launch {
            repository.toggleDriverStatus(drv.id, drv.truckId, status)
                .onSuccess {
                    _verifiedDriver.value = drv.copy(status = status.name)
                }
                .onFailure { _actionMessage.value = it.message }
        }
    }
}
