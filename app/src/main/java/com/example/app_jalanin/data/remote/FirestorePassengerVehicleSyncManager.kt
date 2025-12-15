package com.example.app_jalanin.data.remote

import android.content.Context
import android.util.Log
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.model.PassengerVehicle
import com.example.app_jalanin.data.model.VehicleType
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manager untuk sinkronisasi passenger vehicle data antara Room dan Firestore
 * Memastikan kendaraan pribadi penumpang tersimpan di cloud untuk backup dan cross-device access
 */
object FirestorePassengerVehicleSyncManager {
    private const val TAG = "FirestorePassengerVehicleSync"
    private const val COLLECTION = "passenger_vehicles"
    
    private val db get() = Firebase.firestore
    
    /**
     * Sync single passenger vehicle to Firestore
     */
    suspend fun syncSingleVehicle(context: Context, vehicleId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val localDb = AppDatabase.getDatabase(context)
            val vehicle = localDb.passengerVehicleDao().getVehicleById(vehicleId)
            
            if (vehicle == null) {
                Log.e(TAG, "❌ Vehicle not found: $vehicleId")
                return@withContext false
            }
            
            return@withContext syncVehicle(vehicle, context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing vehicle $vehicleId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sync passenger vehicle to Firestore (insert atau update)
     */
    suspend fun syncVehicle(vehicle: PassengerVehicle, context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Syncing passenger vehicle to Firestore:")
            Log.d(TAG, "   - ID: ${vehicle.id}")
            Log.d(TAG, "   - Passenger ID: '${vehicle.passengerId}'")
            Log.d(TAG, "   - License Plate: ${vehicle.licensePlate}")
            
            if (vehicle.passengerId.isBlank()) {
                Log.e(TAG, "❌ ERROR: passengerId is blank! Cannot sync vehicle.")
                return@withContext false
            }
            
            val vehicleData = hashMapOf(
                "id" to vehicle.id,
                "passengerId" to vehicle.passengerId,
                "type" to vehicle.type.name, // Convert enum to string
                "brand" to vehicle.brand,
                "model" to vehicle.model,
                "year" to vehicle.year,
                "licensePlate" to vehicle.licensePlate,
                "transmission" to (vehicle.transmission ?: ""),
                "seats" to (vehicle.seats ?: 0),
                "engineCapacity" to (vehicle.engineCapacity ?: ""),
                "imageUrl" to (vehicle.imageUrl ?: ""),
                "isActive" to vehicle.isActive,
                "createdAt" to vehicle.createdAt,
                "updatedAt" to vehicle.updatedAt
            )
            
            // Use vehicle ID as document ID for easy lookup
            val documentRef = db.collection(COLLECTION)
                .document(vehicle.id.toString())
            
            documentRef.set(vehicleData).await()
            
            // Mark as synced in local database
            val localDb = AppDatabase.getDatabase(context)
            val updatedVehicle = vehicle.copy(synced = true)
            localDb.passengerVehicleDao().updateVehicle(updatedVehicle)
            
            Log.d(TAG, "✅ Passenger vehicle synced successfully to Firestore")
            Log.d(TAG, "   - Document ID: ${vehicle.id}")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync passenger vehicle to Firestore: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sync all unsynced passenger vehicles to Firestore for a specific passenger
     */
    suspend fun syncUnsyncedVehicles(context: Context, passengerId: String) = withContext(Dispatchers.IO) {
        try {
            val localDb = AppDatabase.getDatabase(context)
            val unsynced = localDb.passengerVehicleDao().getUnsyncedVehicles(passengerId)
            
            if (unsynced.isEmpty()) {
                Log.d(TAG, "✅ No unsynced passenger vehicles to upload for: $passengerId")
                return@withContext
            }
            
            Log.d(TAG, "🔄 Syncing ${unsynced.size} unsynced passenger vehicles to Firestore...")
            
            var successCount = 0
            var failedCount = 0
            
            for (vehicle in unsynced) {
                try {
                    val success = syncVehicle(vehicle, context)
                    if (success) {
                        successCount++
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error syncing vehicle ${vehicle.id}: ${e.message}", e)
                    failedCount++
                }
            }
            
            Log.d(TAG, "✅ Synced $successCount/${unsynced.size} passenger vehicles to Firestore")
            if (failedCount > 0) {
                Log.w(TAG, "⚠️ Failed to sync $failedCount passenger vehicles")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing unsynced passenger vehicles: ${e.message}", e)
        }
    }
    
    /**
     * Download passenger vehicles from Firestore to local database
     */
    suspend fun downloadPassengerVehicles(context: Context, passengerId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Downloading passenger vehicles for: $passengerId from Firestore...")
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("passengerId", passengerId)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Log.d(TAG, "✅ No passenger vehicles found in Firestore for: $passengerId")
                return@withContext
            }
            
            Log.d(TAG, "📦 Found ${snapshot.documents.size} passenger vehicles in Firestore")
            
            val localDb = AppDatabase.getDatabase(context)
            var newCount = 0
            var updatedCount = 0
            
            for (document in snapshot.documents) {
                try {
                    val vehicle = documentToPassengerVehicle(document)
                    if (vehicle != null) {
                        val existing = localDb.passengerVehicleDao().getVehicleById(vehicle.id)
                        
                        if (existing == null) {
                            // Insert new vehicle
                            localDb.passengerVehicleDao().insertVehicle(vehicle.copy(synced = true))
                            newCount++
                            Log.d(TAG, "✅ Inserted new passenger vehicle: ${vehicle.licensePlate}")
                        } else {
                            // Update existing vehicle (keep local changes if newer)
                            if (vehicle.updatedAt > existing.updatedAt) {
                                localDb.passengerVehicleDao().updateVehicle(vehicle.copy(synced = true))
                                updatedCount++
                                Log.d(TAG, "✅ Updated passenger vehicle: ${vehicle.licensePlate}")
                            } else {
                                Log.d(TAG, "⏭️ Passenger vehicle ${vehicle.licensePlate} already up-to-date, skipping")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing passenger vehicle document ${document.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "🎉 Download complete: $newCount new vehicles, $updatedCount updated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading passenger vehicles: ${e.message}", e)
        }
    }
    
    /**
     * Convert Firestore document to PassengerVehicle
     */
    private fun documentToPassengerVehicle(document: com.google.firebase.firestore.DocumentSnapshot): PassengerVehicle? {
        return try {
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return null
            val passengerId = document.getString("passengerId") ?: return null
            val typeStr = document.getString("type") ?: return null
            val type = try {
                VehicleType.valueOf(typeStr)
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Invalid vehicle type: $typeStr")
                return null
            }
            val brand = document.getString("brand") ?: ""
            val model = document.getString("model") ?: ""
            val year = document.getLong("year")?.toInt() ?: 0
            val licensePlate = document.getString("licensePlate") ?: ""
            val transmission = document.getString("transmission")?.takeIf { it.isNotEmpty() }
            val seats = document.getLong("seats")?.toInt()
            val engineCapacity = document.getString("engineCapacity")?.takeIf { it.isNotEmpty() }
            val imageUrl = document.getString("imageUrl")?.takeIf { it.isNotEmpty() }
            val isActive = document.getBoolean("isActive") ?: true
            val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
            
            PassengerVehicle(
                id = id,
                passengerId = passengerId,
                type = type,
                brand = brand,
                model = model,
                year = year,
                licensePlate = licensePlate,
                transmission = transmission,
                seats = seats,
                engineCapacity = engineCapacity,
                imageUrl = imageUrl,
                isActive = isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                synced = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing passenger vehicle document: ${e.message}", e)
            null
        }
    }
}

