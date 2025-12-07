package com.example.app_jalanin.data.local.dao

import androidx.room.*
import com.example.app_jalanin.data.local.entity.Rental
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk Rental operations
 */
@Dao
interface RentalDao {
    /**
     * Insert rental baru
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rental: Rental): Long

    /**
     * Update rental existing
     */
    @Update
    suspend fun update(rental: Rental)

    /**
     * Delete rental
     */
    @Delete
    suspend fun delete(rental: Rental)

    /**
     * Get rental by ID
     */
    @Query("SELECT * FROM rentals WHERE id = :rentalId LIMIT 1")
    suspend fun getRentalById(rentalId: String): Rental?

    /**
     * Get all rentals untuk user tertentu
     */
    @Query("SELECT * FROM rentals WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getRentalsByUserId(userId: Int): List<Rental>

    /**
     * Get all rentals untuk user tertentu (Flow untuk real-time update)
     */
    @Query("SELECT * FROM rentals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRentalsByUserIdFlow(userId: Int): Flow<List<Rental>>

    /**
     * Get all rentals by email (more reliable for dummy users)
     */
    @Query("SELECT * FROM rentals WHERE userEmail = :userEmail ORDER BY createdAt DESC")
    suspend fun getRentalsByEmail(userEmail: String): List<Rental>

    /**
     * Get all rentals by email (Flow)
     */
    @Query("SELECT * FROM rentals WHERE userEmail = :userEmail ORDER BY createdAt DESC")
    fun getRentalsByEmailFlow(userEmail: String): Flow<List<Rental>>

    /**
     * Get active rentals (status = ACTIVE atau DELIVERING)
     */
    @Query("SELECT * FROM rentals WHERE userId = :userId AND (status = 'ACTIVE' OR status = 'DELIVERING') ORDER BY createdAt DESC")
    suspend fun getActiveRentals(userId: Int): List<Rental>

    /**
     * Get active rentals (Flow)
     */
    @Query("SELECT * FROM rentals WHERE userId = :userId AND (status = 'ACTIVE' OR status = 'DELIVERING') ORDER BY createdAt DESC")
    fun getActiveRentalsFlow(userId: Int): Flow<List<Rental>>

    /**
     * Get active rentals by email
     */
    @Query("SELECT * FROM rentals WHERE userEmail = :userEmail AND (status = 'ACTIVE' OR status = 'DELIVERING') ORDER BY createdAt DESC")
    suspend fun getActiveRentalsByEmail(userEmail: String): List<Rental>

    /**
     * Get active rentals by email (Flow)
     */
    @Query("SELECT * FROM rentals WHERE userEmail = :userEmail AND (status = 'ACTIVE' OR status = 'DELIVERING') ORDER BY createdAt DESC")
    fun getActiveRentalsByEmailFlow(userEmail: String): Flow<List<Rental>>

    /**
     * Update rental status
     */
    @Query("UPDATE rentals SET status = :newStatus, updatedAt = :updatedAt WHERE id = :rentalId")
    suspend fun updateStatus(rentalId: String, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update rental start and end times
     */
    @Query("UPDATE rentals SET startDate = :startDate, endDate = :endDate, updatedAt = :updatedAt WHERE id = :rentalId")
    suspend fun updateRentalTimes(rentalId: String, startDate: Long, endDate: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update overtime fee
     */
    @Query("UPDATE rentals SET overtimeFee = :overtimeFee, updatedAt = :updatedAt WHERE id = :rentalId")
    suspend fun updateOvertimeFee(rentalId: String, overtimeFee: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update sync status
     */
    @Query("UPDATE rentals SET synced = :synced, updatedAt = :updatedAt WHERE id = :rentalId")
    suspend fun updateSyncStatus(rentalId: String, synced: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Get unsynced rentals (untuk sync ke Firestore)
     */
    @Query("SELECT * FROM rentals WHERE synced = 0")
    suspend fun getUnsyncedRentals(): List<Rental>

    /**
     * Get all rentals
     */
    @Query("SELECT * FROM rentals ORDER BY createdAt DESC")
    suspend fun getAllRentals(): List<Rental>

    /**
     * Delete all rentals (untuk testing/development)
     */
    @Query("DELETE FROM rentals")
    suspend fun deleteAll()

    /**
     * Delete rentals older than timestamp
     */
    @Query("DELETE FROM rentals WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete completed rentals
     */
    @Query("DELETE FROM rentals WHERE status = 'COMPLETED' OR status = 'CANCELLED'")
    suspend fun deleteCompletedRentals()

    /**
     * Count rentals by status
     */
    @Query("SELECT COUNT(*) FROM rentals WHERE userId = :userId AND status = :status")
    suspend fun countByStatus(userId: Int, status: String): Int

    /**
     * Get overdue rentals
     */
    @Query("SELECT * FROM rentals WHERE userId = :userId AND status = 'ACTIVE' AND endDate < :currentTime")
    suspend fun getOverdueRentals(userId: Int, currentTime: Long = System.currentTimeMillis()): List<Rental>

    /**
     * Get overdue rentals by email (more reliable)
     */
    @Query("SELECT * FROM rentals WHERE userEmail = :userEmail AND status = 'ACTIVE' AND endDate < :currentTime")
    suspend fun getOverdueRentalsByEmail(userEmail: String, currentTime: Long = System.currentTimeMillis()): List<Rental>
}

