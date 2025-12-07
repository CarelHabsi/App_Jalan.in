package com.example.app_jalanin.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room Entity untuk Rental History
 * Format durasi: "hari|jam|menit" (contoh: "0|7|30" = 7 jam 30 menit)
 */
@Entity(
    tableName = "rentals",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userEmail"]), // ✅ Added for email-based queries
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class Rental(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Format: "RENT_<timestamp>_<random>"

    @ColumnInfo(name = "userId")
    val userId: Int, // Foreign key ke users.id

    @ColumnInfo(name = "userEmail")
    val userEmail: String,

    @ColumnInfo(name = "vehicleId")
    val vehicleId: String,

    @ColumnInfo(name = "vehicleName")
    val vehicleName: String,

    @ColumnInfo(name = "vehicleType")
    val vehicleType: String, // "Motor" atau "Mobil"

    @ColumnInfo(name = "startDate")
    val startDate: Long, // Timestamp saat kendaraan tiba & rental dimulai

    @ColumnInfo(name = "endDate")
    val endDate: Long, // Timestamp saat rental seharusnya selesai

    @ColumnInfo(name = "durationDays")
    val durationDays: Int, // Komponen hari

    @ColumnInfo(name = "durationHours")
    val durationHours: Int, // Komponen jam

    @ColumnInfo(name = "durationMinutes")
    val durationMinutes: Int, // Komponen menit

    @ColumnInfo(name = "durationMillis")
    val durationMillis: Long, // Total durasi dalam milliseconds untuk countdown

    @ColumnInfo(name = "totalPrice")
    val totalPrice: Int,

    @ColumnInfo(name = "status")
    val status: String, // "DELIVERING", "ACTIVE", "OVERDUE", "COMPLETED", "CANCELLED"

    @ColumnInfo(name = "overtimeFee")
    val overtimeFee: Int = 0,

    @ColumnInfo(name = "isWithDriver")
    val isWithDriver: Boolean = false,

    @ColumnInfo(name = "deliveryAddress")
    val deliveryAddress: String = "",

    @ColumnInfo(name = "deliveryLat")
    val deliveryLat: Double = 0.0,

    @ColumnInfo(name = "deliveryLon")
    val deliveryLon: Double = 0.0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false // Apakah sudah sync ke Firestore
) {
    /**
     * Format durasi untuk display: "X Hari Y Jam Z Menit"
     */
    fun getFormattedDuration(): String {
        return buildString {
            if (durationDays > 0) append("$durationDays Hari ")
            if (durationHours > 0) append("$durationHours Jam ")
            if (durationMinutes > 0) append("$durationMinutes Menit")
        }.trim()
    }

    /**
     * Format durasi singkat: "7 Jam" atau "2 Hari"
     */
    fun getShortDuration(): String {
        return when {
            durationDays > 0 -> "$durationDays Hari"
            durationHours > 0 -> "$durationHours Jam"
            durationMinutes > 0 -> "$durationMinutes Menit"
            else -> "0 Menit"
        }
    }

    /**
     * Check apakah rental sudah overdue
     */
    fun isOverdue(): Boolean {
        return System.currentTimeMillis() > endDate && status == "ACTIVE"
    }

    /**
     * Get remaining time in milliseconds
     */
    fun getRemainingTime(): Long {
        val now = System.currentTimeMillis()
        return if (now > endDate) {
            -(now - endDate) // Negative untuk overdue
        } else {
            endDate - now
        }
    }
}

