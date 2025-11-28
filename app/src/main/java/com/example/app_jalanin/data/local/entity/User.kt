package com.example.app_jalanin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String,  // Di production harus di-hash
    val role: String,      // "penumpang", "driver_motor", "driver_mobil", "driver_pengganti", "pemilik_kendaraan"
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

