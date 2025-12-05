package com.example.app_jalanin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val password: String,  // Di production harus di-hash
    val role: String,      // "penumpang", "driver_motor", "driver_mobil", "driver_pengganti", "pemilik_kendaraan"
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false // menandai apakah sudah tersinkron ke Firestore
)
