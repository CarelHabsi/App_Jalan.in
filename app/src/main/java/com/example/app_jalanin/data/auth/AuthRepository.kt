package com.example.app_jalanin.data.auth

import android.content.Context
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.UserRepository
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_store", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = SessionManager(context)

    suspend fun ensureDummyPassenger() {
        // Selalu cek database terlebih dahulu, bukan hanya SharedPreferences
        val existingUser = userRepository.getUserByEmail("user123@jalanin.com")
        android.util.Log.d("AuthRepository", "Checking dummy user in database: ${existingUser?.email}, role: ${existingUser?.role}")

        if (existingUser == null) {
            // User tidak ada di database, buat baru
            android.util.Log.d("AuthRepository", "Creating dummy user...")
            val result = userRepository.registerUser(
                email = "user123@jalanin.com",
                password = "jalanin_aja_dulu",
                role = UserRole.PENUMPANG.name,
                fullName = "User Test",
                phoneNumber = "081234567890"
            )
            if (result.isSuccess) {
                prefs.edit().putBoolean("dummy_created", true).apply()
                android.util.Log.d("AuthRepository", "✅ Dummy user created successfully with ID: ${result.getOrNull()}")

                // Verifikasi lagi setelah dibuat
                val verifyUser = userRepository.getUserByEmail("user123@jalanin.com")
                android.util.Log.d("AuthRepository", "Verification - User now exists: ${verifyUser?.email}, role: ${verifyUser?.role}, password: ${verifyUser?.password}")
            } else {
                android.util.Log.e("AuthRepository", "❌ Failed to create dummy user: ${result.exceptionOrNull()?.message}")
            }
        } else {
            android.util.Log.d("AuthRepository", "ℹ️ Dummy user already exists in database with role: ${existingUser.role}")
        }
    }

    suspend fun ensureDummyOwner() {
        // Cek database untuk akun dummy owner rental
        val existingOwner = userRepository.getUserByEmail("owner123@jalanin.com")
        android.util.Log.d("AuthRepository", "Checking dummy owner in database: ${existingOwner?.email}, role: ${existingOwner?.role}")

        if (existingOwner == null) {
            // Owner tidak ada di database, buat baru
            android.util.Log.d("AuthRepository", "Creating dummy owner...")
            val result = userRepository.registerUser(
                email = "owner123@jalanin.com",
                password = "owner_rental_2024",
                role = UserRole.PEMILIK_KENDARAAN.name,
                fullName = "Owner Rental Test",
                phoneNumber = "081298765432"
            )
            if (result.isSuccess) {
                prefs.edit().putBoolean("dummy_owner_created", true).apply()
                android.util.Log.d("AuthRepository", "✅ Dummy owner created successfully with ID: ${result.getOrNull()}")

                // Verifikasi lagi setelah dibuat
                val verifyOwner = userRepository.getUserByEmail("owner123@jalanin.com")
                android.util.Log.d("AuthRepository", "Verification - Owner now exists: ${verifyOwner?.email}, role: ${verifyOwner?.role}, password: ${verifyOwner?.password}")
            } else {
                android.util.Log.e("AuthRepository", "❌ Failed to create dummy owner: ${result.exceptionOrNull()?.message}")
            }
        } else {
            android.util.Log.d("AuthRepository", "ℹ️ Dummy owner already exists in database with role: ${existingOwner.role}")
        }
    }

    suspend fun ensureDummyDriver() {
        // Cek database untuk akun dummy driver
        val existingDriver = userRepository.getUserByEmail("driver123@jalanin.com")
        android.util.Log.d("AuthRepository", "Checking dummy driver in database: ${existingDriver?.email}, role: ${existingDriver?.role}")

        if (existingDriver == null) {
            // Driver tidak ada di database, buat baru
            android.util.Log.d("AuthRepository", "Creating dummy driver...")
            val result = userRepository.registerUser(
                email = "driver123@jalanin.com",
                password = "driver_jalan_2024",
                role = UserRole.DRIVER_PENGGANTI.name,
                fullName = "Driver Test",
                phoneNumber = "081234567891"
            )
            if (result.isSuccess) {
                prefs.edit().putBoolean("dummy_driver_created", true).apply()
                android.util.Log.d("AuthRepository", "✅ Dummy driver created successfully with ID: ${result.getOrNull()}")

                // Verifikasi lagi setelah dibuat
                val verifyDriver = userRepository.getUserByEmail("driver123@jalanin.com")
                android.util.Log.d("AuthRepository", "Verification - Driver now exists: ${verifyDriver?.email}, role: ${verifyDriver?.role}, password: ${verifyDriver?.password}")
            } else {
                android.util.Log.e("AuthRepository", "❌ Failed to create dummy driver: ${result.exceptionOrNull()?.message}")
            }
        } else {
            android.util.Log.d("AuthRepository", "ℹ️ Dummy driver already exists in database with role: ${existingDriver.role}")
        }
    }

    suspend fun login(email: String, password: String, selectedRole: UserRole): Boolean {
        // Login menggunakan Room database
        android.util.Log.d("AuthRepository", "Attempting login with email=$email, role=${selectedRole.name}")

        // ✅ CHECK EMAIL VERIFICATION FIRST (for non-dummy users)
        if (!com.example.app_jalanin.auth.AuthUtils.isDummyEmail(email)) {
            try {
                // Sign in to Firebase to check verification status
                val authResult = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Reload to get latest verification status
                    firebaseUser.reload().await()

                    if (!firebaseUser.isEmailVerified) {
                        android.util.Log.w("AuthRepository", "❌ Email NOT verified: $email")
                        android.util.Log.w("AuthRepository", "⚠️ Login BLOCKED - User must verify email first")

                        // Sign out from Firebase
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

                        android.util.Log.d("AuthRepository", "Login result: FAILED - Email not verified")
                        return false // Block login
                    }

                    android.util.Log.d("AuthRepository", "✅ Email verified: $email")

                    // Sign out from Firebase (we only use it for verification check)
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                } else {
                    android.util.Log.w("AuthRepository", "⚠️ Firebase user is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "❌ Firebase auth failed: ${e.message}")
                // If Firebase auth fails, we'll let Room DB login proceed
                // This handles case where user registered but Firebase sync failed
            }
        } else {
            android.util.Log.d("AuthRepository", "🔓 Dummy user - skip email verification check")
        }

        // Debug: List all users in database
        try {
            val allUsers = userRepository.getAllUsers()
            android.util.Log.d("AuthRepository", "Total users in database: ${allUsers.size}")
            allUsers.forEach { user ->
                android.util.Log.d("AuthRepository", "  - ${user.email} | role: ${user.role} | password: ${user.password}")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to list users", e)
        }

        val result = userRepository.login(
            email = email,
            password = password,
            role = selectedRole.name
        )

        if (result.isSuccess) {
            // Save session after successful login
            val user = result.getOrNull()!!
            sessionManager.saveLoginSession(
                email = user.email,
                role = user.role,
                fullName = user.fullName
            )
            android.util.Log.d("AuthRepository", "✅ Login SUCCESS - Session saved")
        }

        android.util.Log.d("AuthRepository", "Login result: ${if (result.isSuccess) "SUCCESS" else "FAILED - ${result.exceptionOrNull()?.message}"}")
        return result.isSuccess
    }

    /**
     * Get saved session data
     */
    fun getSavedSession(): SessionData? {
        return sessionManager.getSavedSession()
    }

    /**
     * Check if user has active session
     */
    fun hasActiveSession(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Logout - clear session
     */
    fun logout() {
        sessionManager.clearSession()
        android.util.Log.d("AuthRepository", "✅ User logged out")
    }

    /**
     * ✅ Check if user exists in database (for email verification check)
     */
    suspend fun userExists(email: String): Boolean {
        return userRepository.getUserByEmail(email) != null
    }

    suspend fun forceRecreateDummyUser() {
        android.util.Log.d("AuthRepository", "Force recreating all dummy users...")

        // Hapus dummy passenger lama jika ada
        val existingUser = userRepository.getUserByEmail("user123@jalanin.com")
        if (existingUser != null) {
            android.util.Log.d("AuthRepository", "Deleting existing passenger with ID: ${existingUser.id}")
            userRepository.deleteUser(existingUser.id)
        }

        // Hapus dummy owner lama jika ada
        val existingOwner = userRepository.getUserByEmail("owner123@jalanin.com")
        if (existingOwner != null) {
            android.util.Log.d("AuthRepository", "Deleting existing owner with ID: ${existingOwner.id}")
            userRepository.deleteUser(existingOwner.id)
        }

        // Hapus dummy driver lama jika ada
        val existingDriver = userRepository.getUserByEmail("driver123@jalanin.com")
        if (existingDriver != null) {
            android.util.Log.d("AuthRepository", "Deleting existing driver with ID: ${existingDriver.id}")
            userRepository.deleteUser(existingDriver.id)
        }

        // Reset flags
        prefs.edit()
            .putBoolean("dummy_created", false)
            .putBoolean("dummy_owner_created", false)
            .putBoolean("dummy_driver_created", false)
            .apply()

        // Buat ulang semua dummy accounts
        ensureDummyPassenger()
        ensureDummyOwner()
        ensureDummyDriver()

        android.util.Log.d("AuthRepository", "✅ All dummy users recreated successfully")
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_ROLE = "role"
    }
}
