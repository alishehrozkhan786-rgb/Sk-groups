package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.DriverRecord
import com.example.data.model.DubaiGeoCatalog
import com.example.data.model.JobDispatch
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PricingConfig
import com.example.data.model.RoutePoint
import com.example.data.model.Truck
import com.example.data.model.TruckStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class RecoveryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "alishehrozkhan786@gmail.com"
        const val PRICING_DOC_ID = "dubai_16ft"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String? = null) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    // --- AUTH & PROFILE ---

    suspend fun ensureUserProfile(user: FirebaseUser, preferredLang: String): Result<UserProfile> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return Result.success(existing)
            }
            val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Dubai Client").take(80)
            val email = (user.email ?: "").take(120)
            val payload = mapOf(
                "userId" to uid,
                "name" to displayName,
                "email" to email,
                "phone" to (user.phoneNumber ?: "").take(30),
                "role" to UserRole.CUSTOMER.name,
                "blocked" to false,
                "preferredLang" to if (preferredLang == "ar") "ar" else "en",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, name = displayName, email = email, preferredLang = preferredLang)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPhoneAndLang(phone: String, preferredLang: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "phone" to phone.trim().take(30),
                    "preferredLang" to if (preferredLang == "ar") "ar" else "en",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.GET, "users/$uid", uid)
                throw error
            }

    suspend fun checkIsAdmin(user: FirebaseUser): Boolean {
        if (user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            val doc = db.collection("admins").document(user.uid).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    // --- PRICING & REVIEWS ---

    fun observePricingConfig(): Flow<PricingConfig> =
        db.collection("pricing_config").document(PRICING_DOC_ID)
            .snapshots()
            .map { snap ->
                snap.toObject(PricingConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: PricingConfig(id = PRICING_DOC_ID)
            }
            .catch { error ->
                handleFlowError(error, OperationType.GET, "pricing_config/$PRICING_DOC_ID")
                emit(PricingConfig(id = PRICING_DOC_ID))
            }

    fun observePublishedReviews(): Flow<List<CustomerReview>> =
        db.collection("reviews")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(CustomerReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "reviews")
                throw error
            }

    // --- CUSTOMER BOOKINGS & LIVE TRACKING ---

    fun observeCustomerBookings(customerId: String): Flow<List<Booking>> = flow {
        require(customerId.isNotBlank())
        val path = "bookings"
        emitAll(
            db.collection(path)
                .whereEqualTo("customerId", customerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, customerId)
                    throw error
                }
        )
    }

    fun observeBooking(bookingId: String): Flow<Booking?> =
        db.collection("bookings").document(bookingId)
            .snapshots()
            .map { it.toObject(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.GET, "bookings/$bookingId")
                throw error
            }

    fun observeTruck(truckId: String): Flow<Truck?> =
        db.collection("trucks").document(truckId)
            .snapshots()
            .map { it.toObject(Truck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.GET, "trucks/$truckId")
                throw error
            }

    suspend fun createBooking(
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
        paymentMethod: PaymentMethod
    ): Result<String> {
        var path = "bookings"
        return try {
            val uid = requireUserId()
            val bookingRef = db.collection("bookings").document()
            val bookingId = bookingRef.id
            val dispatchRef = db.collection("job_dispatch").document(bookingId)
            path = bookingRef.path

            val payStatus = when (paymentMethod) {
                PaymentMethod.CARD, PaymentMethod.APPLE_PAY -> PaymentStatus.PAID.name
                PaymentMethod.CASH -> PaymentStatus.COD_DUE.name
            }

            val bookingMap = mapOf(
                "customerId" to uid,
                "customerName" to customerName.trim().ifBlank { "Dubai Client" }.take(80),
                "customerPhone" to customerPhone.trim().take(30),
                "pickupArea" to pickupArea.trim().take(80),
                "pickupAddress" to pickupAddress.trim().ifBlank { pickupArea }.take(200),
                "pickupLat" to pickupLat,
                "pickupLng" to pickupLng,
                "dropArea" to dropArea.trim().take(80),
                "dropAddress" to dropAddress.trim().ifBlank { dropArea }.take(200),
                "dropLat" to dropLat,
                "dropLng" to dropLng,
                "carType" to carType,
                "carCondition" to carCondition,
                "issueNotes" to issueNotes.trim().take(300),
                "distanceKm" to distanceKm.coerceIn(0.0, 1000.0),
                "priceAed" to priceAed.coerceIn(0.0, 50000.0),
                "paymentMethod" to paymentMethod.name,
                "paymentStatus" to payStatus,
                "status" to BookingStatus.REQUESTED.name,
                "driverId" to "",
                "driverName" to "",
                "driverPhone" to "",
                "truckId" to "",
                "truckNumber" to "",
                "truckLat" to 25.1375,
                "truckLng" to 55.2255,
                "truckSpeedKmh" to 0.0,
                "etaMinutes" to 18L,
                "distanceToTargetKm" to distanceKm.coerceIn(0.0, 1000.0),
                "carPhotoBase64" to "",
                "rating" to 0L,
                "reviewText" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val dispatchMap = mapOf(
                "bookingId" to bookingId,
                "pickupArea" to pickupArea.trim().take(80),
                "pickupAddress" to pickupAddress.trim().ifBlank { pickupArea }.take(200),
                "pickupLat" to pickupLat,
                "pickupLng" to pickupLng,
                "dropArea" to dropArea.trim().take(80),
                "dropAddress" to dropAddress.trim().ifBlank { dropArea }.take(200),
                "dropLat" to dropLat,
                "dropLng" to dropLng,
                "carType" to carType,
                "carCondition" to carCondition,
                "issueNotes" to issueNotes.trim().take(300),
                "distanceKm" to distanceKm.coerceIn(0.0, 1000.0),
                "status" to "OPEN",
                "assignedDriverId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(bookingRef, bookingMap)
            batch.set(dispatchRef, dispatchMap)
            batch.commit().await()
            Result.success(bookingId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        val path = "bookings/$bookingId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("bookings").document(bookingId),
                mapOf("status" to BookingStatus.CANCELLED.name, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                db.collection("job_dispatch").document(bookingId),
                mapOf("status" to "CANCELLED", "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitRatingAndReview(
        bookingId: String,
        customerName: String,
        carType: String,
        rating: Int,
        reviewText: String
    ): Result<Unit> {
        val path = "bookings/$bookingId"
        return try {
            val uid = requireUserId()
            val safeRating = rating.coerceIn(1, 5).toLong()
            val safeText = reviewText.trim().ifBlank { "Excellent 16ft flatbed recovery service." }.take(300)
            val batch = db.batch()
            batch.update(
                db.collection("bookings").document(bookingId),
                mapOf(
                    "rating" to safeRating,
                    "reviewText" to safeText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val reviewRef = db.collection("reviews").document(bookingId)
            batch.set(
                reviewRef,
                mapOf(
                    "bookingId" to bookingId,
                    "customerId" to uid,
                    "customerName" to customerName.trim().ifBlank { "Dubai Client" }.take(80),
                    "carType" to carType.take(40),
                    "rating" to safeRating,
                    "comment" to safeText,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DRIVER OPERATIONS ---

    suspend fun verifyAndActivateDriver(
        driverCode: String,
        passcode: String,
        user: FirebaseUser
    ): Result<DriverRecord> {
        val email = user.email ?: ""
        val path = "drivers"
        return try {
            val snap = db.collection("drivers")
                .whereEqualTo("email", email)
                .get()
                .await()
            val records = snap.toObjects(DriverRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val matched = records.firstOrNull {
                it.driverCode.equals(driverCode.trim(), ignoreCase = true) &&
                    it.passcode == passcode.trim() &&
                    it.approved
            } ?: return Result.failure(SecurityException("Access Denied: Invalid Driver ID, password, or driver not approved by Admin."))

            // Bind UID onto driver_sessions/{uid} and drivers/{id}
            db.collection("driver_sessions").document(user.uid).set(
                mapOf(
                    "userId" to user.uid,
                    "driverDocId" to matched.id,
                    "truckId" to matched.truckId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (matched.userId != user.uid) {
                db.collection("drivers").document(matched.id).update(
                    mapOf(
                        "userId" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(matched.copy(userId = user.uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun checkExistingDriverSession(user: FirebaseUser): DriverRecord? {
        return try {
            val session = db.collection("driver_sessions").document(user.uid).get().await()
            if (!session.exists()) return null
            val driverDocId = session.getString("driverDocId") ?: return null
            val drv = db.collection("drivers").document(driverDocId).get().await()
                .toObject(DriverRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (drv != null && drv.approved) drv else null
        } catch (_: Exception) {
            null
        }
    }

    fun observeOpenJobDispatches(): Flow<List<JobDispatch>> =
        db.collection("job_dispatch")
            .whereEqualTo("status", "OPEN")
            .snapshots()
            .map { snap ->
                snap.toObjects(JobDispatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "job_dispatch")
                throw error
            }

    fun observeDriverBookings(driverUid: String): Flow<List<Booking>> =
        db.collection("bookings")
            .whereEqualTo("driverId", driverUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "bookings", driverUid)
                throw error
            }

    fun observeDriverTruck(driverUid: String): Flow<Truck?> =
        db.collection("trucks")
            .whereEqualTo("driverId", driverUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Truck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).firstOrNull()
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "trucks", driverUid)
                throw error
            }

    suspend fun acceptJobDispatch(
        dispatch: JobDispatch,
        driver: DriverRecord,
        truckLat: Double,
        truckLng: Double
    ): Result<Unit> {
        val path = "bookings/${dispatch.bookingId}"
        return try {
            val uid = requireUserId()
            val distToPickup = DubaiGeoCatalog.distanceKmBetween(truckLat, truckLng, dispatch.pickupLat, dispatch.pickupLng)
            val etaMins = ((distToPickup / 55.0) * 60.0).roundToInt().coerceAtLeast(2).toLong()

            db.collection("job_dispatch").document(dispatch.bookingId).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "assignedDriverId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("bookings").document(dispatch.bookingId).update(
                mapOf(
                    "driverId" to uid,
                    "driverName" to driver.name,
                    "driverPhone" to driver.phone,
                    "truckId" to driver.truckId,
                    "truckNumber" to driver.truckNumber,
                    "truckLat" to truckLat,
                    "truckLng" to truckLng,
                    "truckSpeedKmh" to 48.0,
                    "etaMinutes" to etaMins,
                    "distanceToTargetKm" to distToPickup,
                    "status" to BookingStatus.ASSIGNED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (driver.truckId.isNotBlank()) {
                db.collection("trucks").document(driver.truckId).update(
                    mapOf(
                        "status" to TruckStatus.ON_JOB.name,
                        "activeBookingId" to dispatch.bookingId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectJobDispatch(bookingId: String): Result<Unit> {
        val path = "job_dispatch/$bookingId"
        return try {
            val uid = requireUserId()
            db.collection("job_dispatch").document(bookingId).update(
                mapOf(
                    "status" to "REJECTED",
                    "assignedDriverId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun pushDriverLiveGps(
        truckId: String,
        truckNumber: String,
        activeBooking: Booking?,
        lat: Double,
        lng: Double,
        speedKmh: Double,
        headingDeg: Double
    ): Result<Unit> {
        val path = "trucks/$truckId"
        return try {
            val uid = requireUserId()
            val locationName = DubaiGeoCatalog.nearestZoneName(lat, lng, isArabic = false)
            val clampedSpeed = speedKmh.coerceIn(0.0, 200.0)

            if (truckId.isNotBlank()) {
                db.collection("trucks").document(truckId).update(
                    mapOf(
                        "lat" to lat.coerceIn(22.0, 27.0),
                        "lng" to lng.coerceIn(51.0, 57.0),
                        "speedKmh" to clampedSpeed,
                        "headingDeg" to headingDeg.coerceIn(0.0, 360.0),
                        "locationName" to locationName.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (activeBooking != null && activeBooking.status !in listOf(BookingStatus.DELIVERED.name, BookingStatus.CANCELLED.name)) {
                val targetLat = if (activeBooking.status == BookingStatus.CAR_LOADED.name) activeBooking.dropLat else activeBooking.pickupLat
                val targetLng = if (activeBooking.status == BookingStatus.CAR_LOADED.name) activeBooking.dropLng else activeBooking.pickupLng
                val distKm = DubaiGeoCatalog.distanceKmBetween(lat, lng, targetLat, targetLng)
                val effectiveSpeed = clampedSpeed.coerceAtLeast(35.0)
                val eta = ((distKm / effectiveSpeed) * 60.0).roundToInt().coerceAtLeast(1).toLong()

                db.collection("bookings").document(activeBooking.id).update(
                    mapOf(
                        "truckLat" to lat,
                        "truckLng" to lng,
                        "truckSpeedKmh" to clampedSpeed,
                        "distanceToTargetKm" to distKm,
                        "etaMinutes" to eta,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (truckId.isNotBlank()) {
                db.collection("route_history").document().set(
                    mapOf(
                        "truckId" to truckId,
                        "truckNumber" to truckNumber.ifBlank { "DXB-16FT" }.take(30),
                        "driverId" to uid,
                        "customerId" to (activeBooking?.customerId ?: ""),
                        "bookingId" to (activeBooking?.id ?: ""),
                        "lat" to lat,
                        "lng" to lng,
                        "speedKmh" to clampedSpeed,
                        "locationName" to locationName.take(120),
                        "recordedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceBookingStatus(
        booking: Booking,
        newStatus: BookingStatus,
        carPhotoBase64: String? = null
    ): Result<Unit> {
        val path = "bookings/${booking.id}"
        return try {
            requireUserId()
            val updateMap = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!carPhotoBase64.isNullOrBlank()) {
                updateMap["carPhotoBase64"] = carPhotoBase64.take(155000)
            }
            if (newStatus == BookingStatus.TRUCK_ARRIVED) {
                updateMap["etaMinutes"] = 0L
                updateMap["distanceToTargetKm"] = 0.0
                updateMap["truckSpeedKmh"] = 0.0
                updateMap["truckLat"] = booking.pickupLat
                updateMap["truckLng"] = booking.pickupLng
            }
            if (newStatus == BookingStatus.CAR_LOADED) {
                val dropDist = DubaiGeoCatalog.distanceKmBetween(
                    booking.pickupLat, booking.pickupLng, booking.dropLat, booking.dropLng
                )
                updateMap["distanceToTargetKm"] = dropDist
                updateMap["etaMinutes"] = ((dropDist / 50.0) * 60.0).roundToInt().coerceAtLeast(3).toLong()
            }
            if (newStatus == BookingStatus.DELIVERED) {
                updateMap["etaMinutes"] = 0L
                updateMap["distanceToTargetKm"] = 0.0
                updateMap["truckSpeedKmh"] = 0.0
                updateMap["truckLat"] = booking.dropLat
                updateMap["truckLng"] = booking.dropLng
                if (booking.paymentMethod == PaymentMethod.CASH.name) {
                    updateMap["paymentStatus"] = PaymentStatus.PAID.name
                }
            }

            db.collection("bookings").document(booking.id).update(updateMap).await()

            if (newStatus == BookingStatus.DELIVERED) {
                db.collection("job_dispatch").document(booking.id).update(
                    mapOf("status" to "COMPLETED", "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                if (booking.truckId.isNotBlank()) {
                    db.collection("trucks").document(booking.truckId).update(
                        mapOf(
                            "status" to TruckStatus.FREE.name,
                            "activeBookingId" to "",
                            "speedKmh" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleDriverStatus(driverDocId: String, truckId: String, newStatus: TruckStatus): Result<Unit> {
        val path = "drivers/$driverDocId"
        return try {
            requireUserId()
            db.collection("drivers").document(driverDocId).update(
                mapOf("status" to newStatus.name, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (truckId.isNotBlank()) {
                db.collection("trucks").document(truckId).update(
                    mapOf("status" to newStatus.name, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS ---

    suspend fun seedInitialFleetIfEmpty(): Result<Unit> {
        return try {
            requireUserId()
            val pricingSnap = db.collection("pricing_config").document(PRICING_DOC_ID).get().await()
            if (!pricingSnap.exists()) {
                db.collection("pricing_config").document(PRICING_DOC_ID).set(
                    mapOf(
                        "baseFeeAed" to 150.0,
                        "perKmAed" to 7.0,
                        "accidentSurchargeAed" to 90.0,
                        "noPapersSurchargeAed" to 120.0,
                        "luxuryMultiplier" to 1.35,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val trucksSnap = db.collection("trucks").limit(1).get().await()
            if (trucksSnap.isEmpty) {
                db.collection("trucks").document("truck_16ft_01").set(
                    mapOf(
                        "truckNumber" to "DXB-16FT-01",
                        "modelName" to "Isuzu 16ft Hydraulic Flatbed",
                        "pinColorHex" to "#D32F2F",
                        "driverId" to "",
                        "driverName" to "Farman Fleet Lead",
                        "status" to TruckStatus.FREE.name,
                        "lat" to 25.1375,
                        "lng" to 55.2255,
                        "speedKmh" to 0.0,
                        "headingDeg" to 45.0,
                        "locationName" to "Al Quoz Industrial 3",
                        "activeBookingId" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "pricing_config/$PRICING_DOC_ID")
            Result.failure(e)
        }
    }

    fun observeAllTrucks(): Flow<List<Truck>> =
        db.collection("trucks")
            .snapshots()
            .map { snap -> snap.toObjects(Truck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "trucks")
                throw error
            }

    fun observeAllDrivers(): Flow<List<DriverRecord>> =
        db.collection("drivers")
            .snapshots()
            .map { snap -> snap.toObjects(DriverRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "drivers")
                throw error
            }

    fun observeAllBookings(): Flow<List<Booking>> =
        db.collection("bookings")
            .snapshots()
            .map { snap ->
                snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "bookings")
                throw error
            }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "users")
                throw error
            }

    fun observeTruckRouteHistory(truckId: String): Flow<List<RoutePoint>> =
        db.collection("route_history")
            .whereEqualTo("truckId", truckId)
            .snapshots()
            .map { snap ->
                snap.toObjects(RoutePoint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.recordedAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, "route_history")
                throw error
            }

    suspend fun addTruck(truckNumber: String, modelName: String, pinColorHex: String): Result<Unit> {
        val docRef = db.collection("trucks").document()
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "truckNumber" to truckNumber.trim().uppercase().take(30),
                    "modelName" to modelName.trim().ifBlank { "16ft Hydraulic Flatbed" }.take(80),
                    "pinColorHex" to pinColorHex,
                    "driverId" to "",
                    "driverName" to "",
                    "status" to TruckStatus.FREE.name,
                    "lat" to 25.1972,
                    "lng" to 55.2744,
                    "speedKmh" to 0.0,
                    "headingDeg" to 0.0,
                    "locationName" to "Downtown Dubai",
                    "activeBookingId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addDriver(
        driverCode: String,
        passcode: String,
        name: String,
        email: String,
        phone: String,
        truck: Truck?
    ): Result<Unit> {
        val docRef = db.collection("drivers").document()
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "userId" to "",
                    "driverCode" to driverCode.trim().uppercase().take(30),
                    "passcode" to passcode.trim().take(40),
                    "name" to name.trim().take(80),
                    "email" to email.trim().lowercase().take(120),
                    "phone" to phone.trim().take(30),
                    "truckId" to (truck?.id ?: ""),
                    "truckNumber" to (truck?.truckNumber ?: ""),
                    "approved" to true,
                    "status" to TruckStatus.FREE.name,
                    "ratingAvg" to 5.0,
                    "totalTrips" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (truck != null) {
                db.collection("trucks").document(truck.id).update(
                    mapOf(
                        "driverName" to name.trim().take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun approveDriver(driver: DriverRecord, approved: Boolean): Result<Unit> {
        val path = "drivers/${driver.id}"
        return try {
            requireUserId()
            db.collection("drivers").document(driver.id).update(
                mapOf("approved" to approved, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (!approved && driver.userId.isNotBlank()) {
                db.collection("driver_sessions").document(driver.userId).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun assignBookingToDriver(booking: Booking, driver: DriverRecord, truck: Truck?): Result<Unit> {
        val path = "bookings/${booking.id}"
        return try {
            requireUserId()
            val targetDriverUid = driver.userId.ifBlank { driver.id }
            val truckLat = truck?.lat ?: 25.1375
            val truckLng = truck?.lng ?: 55.2255
            val dist = DubaiGeoCatalog.distanceKmBetween(truckLat, truckLng, booking.pickupLat, booking.pickupLng)
            val eta = ((dist / 50.0) * 60.0).roundToInt().coerceAtLeast(3).toLong()

            db.collection("bookings").document(booking.id).update(
                mapOf(
                    "driverId" to targetDriverUid,
                    "driverName" to driver.name,
                    "driverPhone" to driver.phone,
                    "truckId" to (truck?.id ?: driver.truckId),
                    "truckNumber" to (truck?.truckNumber ?: driver.truckNumber),
                    "truckLat" to truckLat,
                    "truckLng" to truckLng,
                    "etaMinutes" to eta,
                    "distanceToTargetKm" to dist,
                    "status" to BookingStatus.ASSIGNED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("job_dispatch").document(booking.id).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "assignedDriverId" to targetDriverUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (truck != null) {
                db.collection("trucks").document(truck.id).update(
                    mapOf(
                        "driverId" to targetDriverUid,
                        "driverName" to driver.name,
                        "status" to TruckStatus.ON_JOB.name,
                        "activeBookingId" to booking.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePricingConfig(
        baseFeeAed: Double,
        perKmAed: Double,
        accidentSurchargeAed: Double,
        noPapersSurchargeAed: Double,
        luxuryMultiplier: Double
    ): Result<Unit> {
        val path = "pricing_config/$PRICING_DOC_ID"
        return try {
            requireUserId()
            db.collection("pricing_config").document(PRICING_DOC_ID).set(
                mapOf(
                    "baseFeeAed" to baseFeeAed.coerceIn(0.0, 5000.0),
                    "perKmAed" to perKmAed.coerceIn(0.0, 200.0),
                    "accidentSurchargeAed" to accidentSurchargeAed.coerceIn(0.0, 2000.0),
                    "noPapersSurchargeAed" to noPapersSurchargeAed.coerceIn(0.0, 2000.0),
                    "luxuryMultiplier" to luxuryMultiplier.coerceIn(1.0, 5.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserBlocked(userId: String, blocked: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            db.collection("users").document(userId).update(
                mapOf("blocked" to blocked, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markBookingPaid(bookingId: String): Result<Unit> {
        val path = "bookings/$bookingId"
        return try {
            requireUserId()
            db.collection("bookings").document(bookingId).update(
                mapOf("paymentStatus" to PaymentStatus.PAID.name, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
