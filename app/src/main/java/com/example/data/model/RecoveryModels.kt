package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class UserRole { CUSTOMER, DRIVER, ADMIN }
enum class CarType(val enLabel: String, val arLabel: String) {
    SEDAN("Sedan", "سيدان"),
    SUV("SUV / 4x4", "دفع رباعي"),
    LUXURY("Sports / Luxury", "رياضية / فاخرة"),
    VAN("Van / Classic", "فان / كلاسيك")
}

enum class CarCondition(val enLabel: String, val arLabel: String) {
    ACCIDENT("Accident", "حادث"),
    NO_PAPERS("No Papers", "بدون أوراق"),
    BREAKDOWN("Breakdown", "عطل فني"),
    OTHER("Other Issue", "نقل / أخرى")
}

enum class PaymentMethod { CARD, APPLE_PAY, CASH }
enum class PaymentStatus { PENDING, PAID, COD_DUE }
enum class BookingStatus {
    REQUESTED, ASSIGNED, EN_ROUTE_PICKUP, TRUCK_ARRIVED, CAR_LOADED, DELIVERED, CANCELLED
}
enum class TruckStatus { FREE, ON_JOB, OFFLINE }

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.CUSTOMER.name,
    val blocked: Boolean = false,
    val preferredLang: String = "en",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PricingConfig(
    @DocumentId val id: String = "",
    val baseFeeAed: Double = 150.0,
    val perKmAed: Double = 7.0,
    val accidentSurchargeAed: Double = 90.0,
    val noPapersSurchargeAed: Double = 120.0,
    val luxuryMultiplier: Double = 1.35,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Truck(
    @DocumentId val id: String = "",
    val truckNumber: String = "",
    val modelName: String = "",
    val pinColorHex: String = "#D32F2F",
    val driverId: String = "",
    val driverName: String = "",
    val status: String = TruckStatus.FREE.name,
    val lat: Double = 25.1375,
    val lng: Double = 55.2255,
    val speedKmh: Double = 0.0,
    val headingDeg: Double = 0.0,
    val locationName: String = "Al Quoz Industrial 3",
    val activeBookingId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DriverRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val driverCode: String = "",
    val passcode: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val truckId: String = "",
    val truckNumber: String = "",
    val approved: Boolean = true,
    val status: String = TruckStatus.FREE.name,
    val ratingAvg: Double = 5.0,
    val totalTrips: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val pickupArea: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double = 25.2048,
    val pickupLng: Double = 55.2708,
    val dropArea: String = "",
    val dropAddress: String = "",
    val dropLat: Double = 25.1375,
    val dropLng: Double = 55.2255,
    val carType: String = CarType.SEDAN.name,
    val carCondition: String = CarCondition.BREAKDOWN.name,
    val issueNotes: String = "",
    val distanceKm: Double = 0.0,
    val priceAed: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val paymentStatus: String = PaymentStatus.COD_DUE.name,
    val status: String = BookingStatus.REQUESTED.name,
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val truckId: String = "",
    val truckNumber: String = "",
    val truckLat: Double = 25.1375,
    val truckLng: Double = 55.2255,
    val truckSpeedKmh: Double = 0.0,
    val etaMinutes: Long = 15L,
    val distanceToTargetKm: Double = 0.0,
    val carPhotoBase64: String = "",
    val rating: Long = 0L,
    val reviewText: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JobDispatch(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val pickupArea: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double = 25.2048,
    val pickupLng: Double = 55.2708,
    val dropArea: String = "",
    val dropAddress: String = "",
    val dropLat: Double = 25.1375,
    val dropLng: Double = 55.2255,
    val carType: String = CarType.SEDAN.name,
    val carCondition: String = CarCondition.BREAKDOWN.name,
    val issueNotes: String = "",
    val distanceKm: Double = 0.0,
    val status: String = "OPEN",
    val assignedDriverId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoutePoint(
    @DocumentId val id: String = "",
    val truckId: String = "",
    val truckNumber: String = "",
    val driverId: String = "",
    val customerId: String = "",
    val bookingId: String = "",
    val lat: Double = 25.2048,
    val lng: Double = 55.2708,
    val speedKmh: Double = 0.0,
    val locationName: String = "",
    val recordedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerReview(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val carType: String = CarType.SEDAN.name,
    val rating: Long = 5L,
    val comment: String = "",
    @get:PropertyName("published") @PropertyName("published")
    val published: Boolean = true,
    val createdAt: Timestamp? = null
)

data class DubaiZone(
    val id: String,
    val enName: String,
    val arName: String,
    val lat: Double,
    val lng: Double
)

object DubaiGeoCatalog {
    val zones = listOf(
        DubaiZone("downtown", "Downtown Dubai / Burj Khalifa", "وسط مدينة دبي / برج خليفة", 25.1972, 55.2744),
        DubaiZone("marina", "Dubai Marina / JBR", "دبي مارينا / جميرا بيتش", 25.0805, 55.1403),
        DubaiZone("palm", "Palm Jumeirah", "نخلة جميرا", 25.1124, 55.1390),
        DubaiZone("alquoz", "Al Quoz Industrial", "القوز الصناعية", 25.1375, 55.2255),
        DubaiZone("business_bay", "Business Bay", "الخليج التجاري", 25.1851, 55.2662),
        DubaiZone("deira", "Deira / Al Rigga", "ديرة / الرقة", 25.2697, 55.3188),
        DubaiZone("dxb", "Dubai Airport (DXB)", "مطار دبي الدولي", 25.2532, 55.3657),
        DubaiZone("barsha", "Al Barsha / Mall of Emirates", "البرشاء / مول الإمارات", 25.1181, 55.2006),
        DubaiZone("jebel_ali", "Jebel Ali / DIP", "جبل علي / مجمع دبي للاستثمار", 24.9857, 55.0273),
        DubaiZone("motor_city", "Motor City / AutoDrome", "موتور سيتي", 25.0472, 55.2371),
        DubaiZone("jumeirah", "Jumeirah Beach Road", "شارع شاطئ جميرا", 25.2089, 55.2435),
        DubaiZone("silicon", "Dubai Silicon Oasis", "واحة دبي للسيليكون", 25.1212, 55.3773),
    )

    fun distanceKmBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val roadFactor = 1.28 // Dubai highway / interchange factor
        return ((r * c * roadFactor) * 10.0).roundToInt() / 10.0
    }

    fun nearestZoneName(lat: Double, lng: Double, isArabic: Boolean = false): String {
        val nearest = zones.minByOrNull { distanceKmBetween(lat, lng, it.lat, it.lng) }
        return if (nearest == null) "Dubai" else if (isArabic) nearest.arName else nearest.enName
    }

    fun calculateRecoveryPrice(
        distanceKm: Double,
        carType: CarType,
        condition: CarCondition,
        config: PricingConfig
    ): Double {
        var subtotal = config.baseFeeAed + (distanceKm.coerceAtLeast(3.0) * config.perKmAed)
        if (condition == CarCondition.ACCIDENT) subtotal += config.accidentSurchargeAed
        if (condition == CarCondition.NO_PAPERS) subtotal += config.noPapersSurchargeAed
        if (carType == CarType.LUXURY) subtotal *= config.luxuryMultiplier
        if (carType == CarType.SUV || carType == CarType.VAN) subtotal *= 1.15
        return ((subtotal / 5.0).roundToInt() * 5.0)
    }
}

inline fun <reified T : Enum<T>> safeEnum(raw: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default
