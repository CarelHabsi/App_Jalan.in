package com.example.app_jalanin.data.remote

import com.example.app_jalanin.data.local.entity.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

object FirestoreUserService {

    private val db get() = Firebase.firestore

    /**
     * Get user from Firestore by email
     *
     * ⚠️ WARNING: Firestore does NOT store passwords for security!
     * ⚠️ This function returns user with EMPTY password
     * ⚠️ DO NOT use for login! Only for profile data sync
     */
    suspend fun getUserByEmail(email: String): User? {
        return try {
            val docId = email.replace("@", "_").replace(".", "_")
            val document = db.collection("users").document(docId).get().await()

            if (document.exists()) {
                User(
                    id = 0, // Will be assigned by Local DB
                    email = document.getString("email") ?: email,
                    password = "", // Password not stored in Firestore
                    role = document.getString("role") ?: "penumpang",
                    fullName = document.getString("fullName"),
                    phoneNumber = document.getString("phoneNumber"),
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                    synced = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserService", "Error getting user by email: ${e.message}")
            null
        }
    }

    /**
     * Upsert (insert or update) user to Firestore
     *
     * ✅ SECURITY: Password is NOT synced to Firestore!
     * ✅ Only profile data (email, role, name, phone) is stored in cloud
     * ✅ Password remains ONLY in Local DB (encrypted by Android)
     */
    suspend fun upsertUser(user: User) {
        val data = mapOf(
            "email" to user.email,
            "role" to user.role,
            "fullName" to user.fullName,
            "phoneNumber" to user.phoneNumber,
            "createdAt" to user.createdAt
            // ✅ PASSWORD NOT INCLUDED - Security by design!
        )
        // Gunakan email sebagai document ID (ganti karakter @ dan . dengan _)
        val docId = user.email.replace("@", "_").replace(".", "_")
        db.collection("users").document(docId).set(data).await()
    }

    suspend fun ping() {
        val data = mapOf("ts" to System.currentTimeMillis())
        db.collection("diagnostic").document("ping").set(data).await()
    }
}
