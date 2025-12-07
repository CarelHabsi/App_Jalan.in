package com.example.app_jalanin.ui.owner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.model.Vehicle
import com.example.app_jalanin.data.model.VehicleStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.vehicleDao()

    private val _ownerEmail = MutableStateFlow<String?>(null)

    // State untuk statistik
    private val _countTersedia = MutableStateFlow(0)
    val countTersedia: StateFlow<Int> = _countTersedia

    private val _countSedangDisewa = MutableStateFlow(0)
    val countSedangDisewa: StateFlow<Int> = _countSedangDisewa

    private val _countTidakTersedia = MutableStateFlow(0)
    val countTidakTersedia: StateFlow<Int> = _countTidakTersedia

    // List kendaraan
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun setOwnerEmail(email: String) {
        _ownerEmail.value = email
        loadVehicles()
        loadStatistics()
    }

    private fun loadVehicles() {
        val email = _ownerEmail.value ?: return

        viewModelScope.launch {
            try {
                dao.getAllVehiclesByOwner(email)
                    .collect { vehicleList ->
                        _vehicles.value = vehicleList
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat kendaraan: ${e.message}"
            }
        }
    }

    private fun loadStatistics() {
        val email = _ownerEmail.value ?: return

        viewModelScope.launch {
            try {
                _countTersedia.value = dao.countVehiclesByStatus(email, VehicleStatus.TERSEDIA)
                _countSedangDisewa.value = dao.countVehiclesByStatus(email, VehicleStatus.SEDANG_DISEWA)
                _countTidakTersedia.value = dao.countVehiclesByStatus(email, VehicleStatus.TIDAK_TERSEDIA)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat statistik: ${e.message}"
            }
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                dao.insertVehicle(vehicle)
                loadStatistics() // Refresh statistics
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menambah kendaraan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updatedVehicle = vehicle.copy(updatedAt = System.currentTimeMillis())
                dao.updateVehicle(updatedVehicle)
                loadStatistics() // Refresh statistics
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengubah kendaraan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                dao.deleteVehicle(vehicle)
                loadStatistics() // Refresh statistics
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus kendaraan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateVehicleStatus(vehicleId: Int, status: VehicleStatus, reason: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                dao.updateVehicleStatus(
                    vehicleId = vehicleId,
                    status = status,
                    reason = reason,
                    updatedAt = System.currentTimeMillis()
                )
                loadStatistics() // Refresh statistics
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengubah status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

