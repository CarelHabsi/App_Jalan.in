package com.example.app_jalanin.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.UserRepository
import kotlinx.coroutines.launch

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
     */
    fun registerUser(
        username: String,
        password: String,
        role: String,
        fullName: String,
        phoneNumber: String,
        email: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = userRepository.registerUser(
                    username = username,
                    password = password,
                    role = role,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    email = email
                )

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Registrasi gagal")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
