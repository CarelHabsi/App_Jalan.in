package com.example.app_jalanin.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import com.example.app_jalanin.auth.AuthUtils
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.UserRepository
import com.example.app_jalanin.data.remote.FirestoreUserService
import com.example.app_jalanin.data.local.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ViewModel sederhana menampung state form pendaftaran. */
class RegistrationFormViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        userRepository = UserRepository(userDao)
    }

    var data by mutableStateOf(DriverRegistrationData(driverTypeId = -1))
        private set

    fun initType(typeId: Int) {
        if (data.driverTypeId == -1) {
            data = data.copy(driverTypeId = typeId)
        }
    }

    fun updateFullName(v: String) { data = data.copy(fullName = v) }
    fun updatePhone(v: String) { data = data.copy(phone = v) }
    fun updateEmail(v: String) { data = data.copy(email = v) }
    fun updateAddress(v: String) { data = data.copy(address = v) }
    fun updateIdCard(v: String) { data = data.copy(idCardNumber = v) }

    fun updateMotorPlate(v: String) { data = data.copy(motorPlate = v) }
    fun updateCarPlate(v: String) { data = data.copy(carPlate = v) }
    fun updateShift(v: String) { data = data.copy(shiftAvailability = v) }
    fun updateFleetSize(v: String) { data = data.copy(fleetSize = v) }
    fun updatePhotoSelf(path: String) { data = data.copy(photoSelfPath = path) }
    fun updateVehicleBrandModel(v: String) { data = data.copy(vehicleBrandModel = v) }
    fun updateSimPath(v: String) { data = data.copy(simDocumentPath = v) }
    fun updateStnkPath(v: String) { data = data.copy(stnkDocumentPath = v) }
    fun updateVehicleCategory(v: String) { data = data.copy(vehicleCategory = v) }
    fun updateVehicleEngineCc(v: String) { data = data.copy(vehicleEngineCc = v) }
    fun updateVehiclePhoto(path: String) { data = data.copy(vehiclePhotoPath = path) }
    fun updateSimType(v: String) { data = data.copy(simType = v) }
    fun updateExperienceYears(v: String) { data = data.copy(experienceYears = v) }
    fun updateLocationAddress(v: String) { data = data.copy(locationAddress = v) }
    fun updateOwnerVehicleType(v: String) { data = data.copy(ownerVehicleType = v) }
    fun updateVehicleYear(v: String) { data = data.copy(vehicleYear = v) }
    fun updateVehicleCapacity(v: String) { data = data.copy(vehicleCapacity = v) }
    fun updateRentalPrice(v: String) { data = data.copy(rentalPrice = v) }

    fun isValid(): Boolean {
        val d = data
        if (d.fullName.isBlank() || d.phone.length < 8 || d.email.isBlank()) return false
        return when (d.driverTypeId) {
            1 -> d.photoSelfPath.isNotBlank() && d.vehiclePhotoPath.isNotBlank() &&
                 d.vehicleCategory.isNotBlank() && d.vehicleBrandModel.isNotBlank() &&
                 d.motorPlate.isNotBlank() && d.simDocumentPath.isNotBlank() && d.stnkDocumentPath.isNotBlank() &&
                 (if (d.vehicleCategory.equals("Moge", true)) d.vehicleEngineCc.isNotBlank() else true)
            2 -> d.photoSelfPath.isNotBlank() && d.vehiclePhotoPath.isNotBlank() &&
                 d.vehicleCategory.isNotBlank() && d.vehicleBrandModel.isNotBlank() &&
                 d.motorPlate.isNotBlank() && d.simDocumentPath.isNotBlank() && d.stnkDocumentPath.isNotBlank()
            3 -> d.photoSelfPath.isNotBlank() && d.simType.isNotBlank() && d.experienceYears.isNotBlank()
            4 -> d.vehiclePhotoPath.isNotBlank() && d.ownerVehicleType.isNotBlank() && d.vehicleYear.isNotBlank() &&
                 d.vehicleCapacity.isNotBlank() && d.rentalPrice.isNotBlank() && d.stnkDocumentPath.isNotBlank() &&
                 d.locationAddress.isNotBlank()
            else -> false
        }
    }

    /**
     * Register user (untuk penumpang atau role lainnya) ke database
     * Flow: Register ke Firebase Auth → Kirim email verifikasi (jika bukan dummy) → Save ke local DB + Firestore
     */
    fun registerUser(
        email: String,
        password: String,
        role: String,
        fullName: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Cek apakah email dummy
                val isDummy = AuthUtils.isDummyEmail(email)

                if (isDummy) {
                    // Email dummy - skip Firebase Auth, langsung save ke local DB
                    Log.d("Registration", "⚠️ Email dummy terdeteksi, skip Firebase Auth registration")
                    saveUserToDatabase(email, password, role, fullName, phoneNumber, onSuccess, onError)
                    return@launch
                }

                // Email valid - register ke Firebase Authentication
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Log.d("Registration", "✅ Firebase Auth registration berhasil untuk: $email")

                            // IMPORTANT: Save to database FIRST before any other operations
                            // This ensures data is saved even if email verification fails
                            Log.d("Registration", "🔄 Saving to database immediately...")
                            saveUserToDatabase(email, password, role, fullName, phoneNumber,
                                onSuccess = {
                                    Log.d("Registration", "✅ Database save SUCCESS, now try to send email verification")

                                    // Try to send email verification (optional, don't block on this)
                                    try {
                                        AuthUtils.sendEmailVerification { success, message ->
                                            if (success) {
                                                Log.d("Registration", "✅ Email verifikasi berhasil dikirim ke: $email")
                                            } else {
                                                Log.w("Registration", "⚠️ Gagal kirim email verifikasi: $message (tapi data sudah tersimpan)")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Registration", "⚠️ Email verification error: ${e.message} (tapi data sudah tersimpan)")
                                    }

                                    // Sign out user after everything (optional)
                                    FirebaseAuth.getInstance().signOut()

                                    // Call original onSuccess callback
                                    onSuccess()
                                },
                                onError = { error ->
                                    Log.e("Registration", "❌ Database save FAILED: $error")
                                    onError(error)
                                }
                            )
                        } else {
                            val errorMessage = authTask.exception?.message ?: "Firebase Auth registration gagal"
                            Log.e("Registration", "Firebase Auth error: $errorMessage")

                            // Check if error is "email already in use"
                            if (errorMessage.contains("already in use", ignoreCase = true)) {
                                Log.e("Registration", "❌ GHOST ACCOUNT DETECTED in Firebase Authentication!")
                                Log.e("Registration", "Email: $email exists in Firebase Auth but NOT in Local DB or Firestore")
                                Log.e("Registration", "This is an orphaned/ghost account that needs manual cleanup")

                                // Provide clear error message with solution
                                onError("Email ini terdaftar di sistem Firebase tapi tidak ada di database lokal. " +
                                       "Ini adalah 'ghost account'. Silakan:\n" +
                                       "1. Gunakan Debug Screen → COMPLETE DELETE untuk cleanup, atau\n" +
                                       "2. Gunakan email lain untuk registrasi")
                            } else {
                                onError(errorMessage)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("Registration", "Exception: ${e.message}")
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    /**
     * Helper function untuk save user ke local database + Firestore
     */
    private fun saveUserToDatabase(
        email: String,
        password: String,
        role: String,
        fullName: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) { // ✅ Ensure DB operations on IO thread
            try {
                Log.d("Registration", "📝 saveUserToDatabase CALLED for: $email")
                Log.d("Registration", "   - Role: $role")
                Log.d("Registration", "   - FullName: $fullName")
                Log.d("Registration", "   - Phone: $phoneNumber")
                Log.d("Registration", "   - Password length: ${password.length}") // ✅ DEBUG
                Log.d("Registration", "   - Password isEmpty: ${password.isEmpty()}") // ✅ DEBUG

                if (password.isEmpty()) {
                    Log.e("Registration", "❌ CRITICAL: Password is EMPTY!")
                    withContext(Dispatchers.Main) {
                        onError("Password tidak boleh kosong")
                    }
                    return@launch
                }

                val now = System.currentTimeMillis()

                Log.d("Registration", "🔄 Calling userRepository.registerUser()...")
                Log.d("Registration", "   - Passing password length: ${password.length}") // ✅ DEBUG
                val result = userRepository.registerUser(
                    email = email,
                    password = password,
                    role = role,
                    fullName = fullName,
                    phoneNumber = phoneNumber
                )

                Log.d("Registration", "📊 Repository result: isSuccess = ${result.isSuccess}")

                if (result.isSuccess) {
                    val userId = result.getOrNull()?.toInt() ?: 0

                    if (userId <= 0) {
                        Log.e("Registration", "❌ Invalid user ID: $userId")
                        withContext(Dispatchers.Main) {
                            onError("Gagal menyimpan user: ID tidak valid")
                        }
                        return@launch
                    }

                    Log.d("Registration", "✅ User saved to LOCAL DB with ID: $userId")

                    // ✅ VERIFY: Check if user really exists in Local DB
                    try {
                        val savedUser = userRepository.getUserByEmail(email)
                        if (savedUser == null) {
                            Log.e("Registration", "❌ CRITICAL: User NOT FOUND in Local DB after insert!")
                            withContext(Dispatchers.Main) {
                                onError("User tidak tersimpan di database lokal")
                            }
                            return@launch
                        }
                        Log.d("Registration", "✅ VERIFIED: User exists in Local DB (ID: ${savedUser.id})")
                    } catch (e: Exception) {
                        Log.e("Registration", "❌ Verification failed: ${e.message}")
                        // Continue anyway - insert was successful
                    }

                    // Sync to Firestore
                    try {
                        val user = User(
                            id = userId,
                            email = email,
                            password = password,
                            role = role,
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            createdAt = now,
                            synced = false
                        )

                        Log.d("Registration", "🔄 Syncing to Firestore...")
                        FirestoreUserService.upsertUser(user.copy(synced = true))
                        Log.d("Registration", "✅ User synced to FIRESTORE: $email")

                        userRepository.markSynced(user.id)
                        Log.d("Registration", "✅ User marked as synced in local DB")
                    } catch (e: Exception) {
                        Log.e("Registration", "❌ Firestore sync FAILED: ${e.message}", e)
                        // Continue anyway - local DB save is successful
                    }

                    Log.d("Registration", "🎉 Registration COMPLETE for: $email")
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Registrasi gagal"
                    Log.e("Registration", "❌ Local DB save FAILED: $error")
                    withContext(Dispatchers.Main) {
                        onError(error)
                    }
                }
            } catch (e: Exception) {
                Log.e("Registration", "❌ saveUserToDatabase EXCEPTION: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Terjadi kesalahan saat menyimpan data")
                }
            }
        }
    }
}
