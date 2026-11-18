package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RecoveryRepository
import com.example.data.model.Booking
import com.example.data.model.CustomerReview
import com.example.data.model.PaymentMethod
import com.example.data.model.PricingConfig
import com.example.data.model.Truck
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

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CustomerViewModel(
    private val repository: RecoveryRepository,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "CustomerVM"
        const val STOP_TIMEOUT = 5000L
    }

    val pricingConfig: StateFlow<PricingConfig> = repository.observePricingConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), PricingConfig())

    val bookingsState: StateFlow<UiState<List<Booking>>> = repository.observeCustomerBookings(currentUserId)
        .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing customer bookings", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load bookings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val reviewsState: StateFlow<List<CustomerReview>> = repository.observePublishedReviews()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val _selectedTrackingBookingId = MutableStateFlow<String?>(null)
    val selectedTrackingBookingId = _selectedTrackingBookingId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val trackedBooking: StateFlow<Booking?> = _selectedTrackingBookingId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null)
            else repository.observeBooking(id).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trackedTruck: StateFlow<Truck?> = trackedBooking
        .flatMapLatest { booking ->
            val tId = booking?.truckId
            if (tId.isNullOrBlank()) flowOf(null)
            else repository.observeTruck(tId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    fun selectBookingToTrack(bookingId: String) {
        _selectedTrackingBookingId.value = bookingId
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun createBooking(
        customerName: String,
        customerPhone: String,
        pickupArea: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropArea: String,
        dropAddress: String,
        dropLat: Double,
        dropLng: Double,
        carType: String,
        carCondition: String,
        issueNotes: String,
        distanceKm: Double,
        priceAed: Double,
        paymentMethod: PaymentMethod,
        onBooked: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createBooking(
                customerName, customerPhone,
                pickupArea, pickupAddress, pickupLat, pickupLng,
                dropArea, dropAddress, dropLat, dropLng,
                carType, carCondition, issueNotes,
                distanceKm, priceAed, paymentMethod
            )
            _isSubmitting.value = false
            result.onSuccess { bookingId ->
                _selectedTrackingBookingId.value = bookingId
                _actionMessage.value = "Booking confirmed! Searching nearest 16ft flatbed..."
                onBooked(bookingId)
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Unable to create booking"
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            val result = repository.cancelBooking(bookingId)
            result.onSuccess {
                _actionMessage.value = "Booking cancelled."
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not cancel booking"
            }
        }
    }

    fun submitRating(booking: Booking, rating: Int, reviewText: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.submitRatingAndReview(
                bookingId = booking.id,
                customerName = booking.customerName,
                carType = booking.carType,
                rating = rating,
                reviewText = reviewText
            )
            _isSubmitting.value = false
            result.onSuccess {
                _actionMessage.value = "Thank you for rating Farman Recovery!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to save rating"
            }
        }
    }
}
