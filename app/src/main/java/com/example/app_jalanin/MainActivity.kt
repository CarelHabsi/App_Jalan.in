package com.example.app_jalanin

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.entity.User
import com.example.app_jalanin.data.remote.FirestoreUserService
import com.example.app_jalanin.data.remote.FirestoreSyncManager
import com.example.app_jalanin.data.remote.FirestoreRentalService
import com.example.app_jalanin.auth.AuthStateManager
import com.example.app_jalanin.ui.passenger.PassengerDashboardScreen
import com.example.app_jalanin.ui.passenger.SewaKendaraanScreen
import com.example.app_jalanin.ui.passenger.KonfirmasiSewaScreen
import com.example.app_jalanin.ui.passenger.VehicleTrackingScreen
import com.example.app_jalanin.ui.passenger.VehicleArrivedScreen
import com.example.app_jalanin.ui.passenger.RentalHistoryScreen
import com.example.app_jalanin.ui.passenger.EarlyReturnScreen
import com.example.app_jalanin.ui.passenger.TrackingData
import com.example.app_jalanin.ui.passenger.DriverFoundScreen
import com.example.app_jalanin.ui.passenger.PassengerVehiclesScreen
import com.example.app_jalanin.ui.passenger.PassengerDriverListScreen
import com.example.app_jalanin.ui.passenger.CreateDriverRequestScreen
import com.example.app_jalanin.ui.passenger.RentDriverScreen
import com.example.app_jalanin.ui.passenger.DriverRentalConfirmationScreen
import com.example.app_jalanin.ui.driver.DriverRequestsScreen
import com.example.app_jalanin.ui.driver.DriverRequestDetailScreen
import com.example.app_jalanin.ui.driver.DriverDashboardScreen
import com.example.app_jalanin.ui.owner.OwnerDashboardScreen
import com.example.app_jalanin.ui.owner.OwnerDeliveryOptionScreen
import com.example.app_jalanin.ui.owner.SelectDriverForDeliveryScreen
import com.example.app_jalanin.ui.owner.OwnerRentalHistoryScreen
import com.example.app_jalanin.ui.chat.ChatScreen
import com.example.app_jalanin.ui.login.AccountLoginScreen as LoginScreenWithVm
import com.example.app_jalanin.ui.register.RegistrationFormViewModel
import com.example.app_jalanin.ui.register.AccountRegistrationTypeScreen
import com.example.app_jalanin.ui.register.SimpleRegistrationFormScreen
import com.example.app_jalanin.ui.register.RegistrationAccountType
import com.example.app_jalanin.ui.theme.App_JalanInTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    override fun onResume() {
        super.onResume()
        // ✅ Auto-complete expired rentals and sync to Firestore
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val now = System.currentTimeMillis()
                
                // Check for ACTIVE or OVERDUE rentals that have passed their endDate
                val activeRentals = db.rentalDao().getAllRentals()
                val expiredRentals = activeRentals.filter { rental ->
                    (rental.status == "ACTIVE" || rental.status == "OVERDUE") &&
                    rental.endDate > 0 &&
                    now > rental.endDate
                }
                
                if (expiredRentals.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "🔄 Found ${expiredRentals.size} expired rental(s), auto-completing...")
                    
                    expiredRentals.forEach { rental ->
                        try {
                            // Update rental to COMPLETED
                            val completedRental = rental.copy(
                                status = "COMPLETED",
                                // If early return was in progress, mark it as completed too
                                earlyReturnStatus = if (rental.earlyReturnRequested && 
                                    (rental.earlyReturnStatus == "IN_PROGRESS" || rental.earlyReturnStatus == "CONFIRMED")) {
                                    "COMPLETED"
                                } else {
                                    rental.earlyReturnStatus
                                },
                                updatedAt = now,
                                synced = false
                            )
                            
                            db.rentalDao().update(completedRental)
                            android.util.Log.d("MainActivity", "✅ Auto-completed rental: ${rental.id} (${rental.vehicleName})")
                            
                            // ✅ FIX: Update vehicle status when rental is completed
                            updateVehicleStatusForRental(rental, db)
                            
                            // Sync to Firestore
                            com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                this@MainActivity,
                                rental.id
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error auto-completing rental ${rental.id}: ${e.message}", e)
                        }
                    }
                }
                
                // ✅ Auto-sync unsynced rentals when app resumes
                android.util.Log.d("MainActivity", "🔄 Auto-sync: Checking for unsynced rentals...")
                com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncUnsyncedRentals(this@MainActivity)
                android.util.Log.d("MainActivity", "✅ Rental sync completed")
                
                // ✅ Auto-sync unsynced payment history
                android.util.Log.d("MainActivity", "🔄 Auto-sync: Checking for unsynced payments...")
                com.example.app_jalanin.data.remote.FirestorePaymentSyncManager.syncUnsyncedPayments(this@MainActivity)
                android.util.Log.d("MainActivity", "✅ Payment sync completed")
                
                // ✅ Auto-sync unsynced income history
                android.util.Log.d("MainActivity", "🔄 Auto-sync: Checking for unsynced incomes...")
                com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.syncUnsyncedIncomes(this@MainActivity)
                android.util.Log.d("MainActivity", "✅ Income sync completed")
                
                // ✅ Auto-sync unsynced users to Firestore
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(this@MainActivity)
                        val unsyncedUsers = db.userDao().getUnsyncedUsers()
                        if (unsyncedUsers.isNotEmpty()) {
                            android.util.Log.d("MainActivity", "🔄 Auto-sync: Syncing ${unsyncedUsers.size} unsynced users to Firestore...")
                            for (user in unsyncedUsers) {
                                try {
                                    com.example.app_jalanin.data.remote.FirestoreUserService.upsertUser(user)
                                    db.userDao().markUserSynced(user.id)
                                    android.util.Log.d("MainActivity", "✅ Synced user: ${user.email}")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "❌ Error syncing user ${user.email}: ${e.message}", e)
                                }
                            }
                            android.util.Log.d("MainActivity", "✅ User sync completed")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Error syncing unsynced users: ${e.message}", e)
                    }
                }
                
                // ✅ Auto-sync unsynced driver profiles to Firestore
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        android.util.Log.d("MainActivity", "🔄 Auto-sync: Checking for unsynced driver profiles...")
                        com.example.app_jalanin.data.remote.FirestoreDriverProfileSyncManager.syncUnsyncedProfiles(this@MainActivity)
                        android.util.Log.d("MainActivity", "✅ Driver profile sync completed")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Error syncing driver profiles: ${e.message}", e)
                    }
                }
                
                // ✅ Auto-download payment/income history from Firestore for current user
                try {
                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                    val savedSession = sessionManager.getSavedSession()
                    
                    if (savedSession != null) {
                        when (savedSession.role.uppercase()) {
                            "PENUMPANG" -> {
                                // Download payment history for passenger
                                android.util.Log.d("MainActivity", "📥 Auto-downloading payment history for passenger: ${savedSession.email}")
                                com.example.app_jalanin.data.remote.FirestorePaymentSyncManager.downloadUserPayments(
                                    this@MainActivity,
                                    savedSession.email
                                )
                                
                                // Download passenger vehicles
                                android.util.Log.d("MainActivity", "📥 Auto-downloading passenger vehicles for: ${savedSession.email}")
                                com.example.app_jalanin.data.remote.FirestorePassengerVehicleSyncManager.downloadPassengerVehicles(
                                    this@MainActivity,
                                    savedSession.email
                                )
                                
                                // Auto-sync unsynced passenger vehicles
                                android.util.Log.d("MainActivity", "🔄 Auto-sync: Checking for unsynced passenger vehicles...")
                                com.example.app_jalanin.data.remote.FirestorePassengerVehicleSyncManager.syncUnsyncedVehicles(
                                    this@MainActivity,
                                    savedSession.email
                                )
                            }
                            "PEMILIK_KENDARAAN" -> {
                                // Download income history for owner
                                android.util.Log.d("MainActivity", "📥 Auto-downloading income history for owner: ${savedSession.email}")
                                com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                    this@MainActivity,
                                    savedSession.email,
                                    "PEMILIK_KENDARAAN"
                                )
                                
                                // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                                // Firestore balance is the SINGLE SOURCE OF TRUTH
                                // DO NOT recalculate balance from transaction history
                                com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                    this@MainActivity,
                                    savedSession.email
                                )
                                
                                // ✅ FIX: Download vehicles from Firestore for owner
                                android.util.Log.d("MainActivity", "📥 Auto-downloading vehicles for owner: ${savedSession.email}")
                                try {
                                    com.example.app_jalanin.data.remote.FirestoreVehicleService.downloadVehiclesByOwner(
                                        this@MainActivity,
                                        savedSession.email
                                    )
                                    android.util.Log.d("MainActivity", "✅ Vehicles downloaded for owner")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "❌ Error downloading vehicles: ${e.message}", e)
                                }
                            }
                            "DRIVER" -> {
                                // Download income history for driver
                                android.util.Log.d("MainActivity", "📥 Auto-downloading income history for driver: ${savedSession.email}")
                                com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                    this@MainActivity,
                                    savedSession.email,
                                    "DRIVER"
                                )
                                
                                // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                                // Firestore balance is the SINGLE SOURCE OF TRUTH
                                // DO NOT recalculate balance from transaction history
                                com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                    this@MainActivity,
                                    savedSession.email
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error auto-downloading payment/income history: ${e.message}", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Auto-sync error: ${e.message}", e)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Initialize database with error handling for schema migrations
        lifecycleScope.launch {
            try {
                // Try to get database - this will trigger migration or recreation
                val db = AppDatabase.getDatabase(this@MainActivity)
                android.util.Log.d("MainActivity", "✅ Database initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Database initialization error: ${e.message}", e)
                // Clear database instance and try to recreate
                AppDatabase.clearInstance()
                try {
                    // Force delete old database file if schema is corrupted
                    this@MainActivity.deleteDatabase("jalanin_database")
                    android.util.Log.d("MainActivity", "🗑️ Old database deleted, will recreate...")

                    // Recreate database
                    val db = AppDatabase.getDatabase(this@MainActivity)
                    android.util.Log.d("MainActivity", "✅ Database recreated successfully")
                } catch (e2: Exception) {
                    android.util.Log.e("MainActivity", "❌ Critical database error: ${e2.message}", e2)
                }
            }
        }

        // Seed dummy users ke local database (ONLY if not exists - PERSISTENCE FIX)
        lifecycleScope.launch {
            try {
                // ✅ CRITICAL: Wait for database initialization to complete
                kotlinx.coroutines.delay(500) // Give database time to initialize
                
                val db = try {
                    AppDatabase.getDatabase(this@MainActivity)
                } catch (e: IllegalStateException) {
                    // ✅ Handle schema mismatch - database will be recreated by AppDatabase
                    if (e.message?.contains("Room cannot verify the data integrity") == true || 
                        e.message?.contains("identity hash") == true) {
                        android.util.Log.w("MainActivity", "⚠️ Schema mismatch detected, database will be recreated...")
                        // Clear instance and try again
                        AppDatabase.clearInstance()
                        kotlinx.coroutines.delay(500)
                        try {
                            AppDatabase.getDatabase(this@MainActivity)
                        } catch (e2: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error getting database after schema fix: ${e2.message}", e2)
                            return@launch
                        }
                    } else {
                        android.util.Log.e("MainActivity", "❌ Error getting database for seeding: ${e.message}", e)
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error getting database for seeding: ${e.message}", e)
                    return@launch
                }
                
                val userDao = db.userDao()

                val existingUsers = try {
                    userDao.getAllUsers()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error getting existing users: ${e.message}", e)
                    emptyList()
                }
                
                android.util.Log.d("MainActivity", "📊 Found ${existingUsers.size} existing users in database")

                // ✅ PERSISTENCE FIX: Use fixed IDs for dummy users to maintain rental relationships
                if (existingUsers.isEmpty()) {
                    android.util.Log.d("MainActivity", "🌱 Database is empty, seeding dummy users with FIXED IDs...")

                    val dummyUsers = listOf(
                        User(
                            id = 1001, // ✅ FIXED ID - will never change
                            email = "user123@jalanin.com",
                            password = "jalanin_aja_dulu",
                            role = "penumpang",
                            fullName = "Dummy User 123",
                            phoneNumber = "081234567890",
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        ),
                        User(
                            id = 1002, // ✅ FIXED ID
                            email = "test@jalanin.com",
                            password = "password123",
                            role = "penumpang",
                            fullName = "Test User",
                            phoneNumber = "081234567891",
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        ),
                        User(
                            id = 1003, // ✅ FIXED ID
                            email = "driver@jalanin.com",
                            password = "password123",
                            role = "driver",
                            fullName = "Driver Test",
                            phoneNumber = "081234567892",
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        ),
                        User(
                            id = 1004, // ✅ FIXED ID
                            email = "owner@jalanin.com",
                            password = "password123",
                            role = "pemilik",
                            fullName = "Owner Test",
                            phoneNumber = "081234567893",
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        )
                    )

                    dummyUsers.forEach { user ->
                        try {
                            val insertedId = userDao.insert(user)
                            android.util.Log.d("MainActivity", "✅ SEEDED - ID: $insertedId, Email: ${user.email}, Password: ${user.password}, Role: ${user.role}")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error seeding user ${user.email}: ${e.message}", e)
                            // Continue with next user even if one fails
                        }
                    }

                    android.util.Log.d("MainActivity", "✅ ✅ ✅ Seeding dummy users with FIXED IDs COMPLETE ✅ ✅ ✅")
                } else {
                    android.util.Log.d("MainActivity", "✅ Users already exist, skipping seed (preserving rental history)")

                    // ✅ VERIFY: Check if users have correct fixed IDs
                    val needsFixing = existingUsers.any { it.id < 1001 }
                    if (needsFixing) {
                        android.util.Log.w("MainActivity", "⚠️ Found users with auto-generated IDs, this may cause rental lookup issues")
                        android.util.Log.w("MainActivity", "⚠️ Consider clearing app data to re-seed with fixed IDs")
                    }
                }

                // Verify semua users ada
                try {
                    val allUsers = userDao.getAllUsers()
                    android.util.Log.d("MainActivity", "✅ Total users in database: ${allUsers.size}")
                    allUsers.forEach {
                        android.util.Log.d("MainActivity", "  📋 DB: ${it.email} | Role: '${it.role}' | ID: ${it.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error verifying users: ${e.message}", e)
                }

                // ✅ RENTAL HISTORY DEBUG: Show rental count
                try {
                    val rentalDao = db.rentalDao()
                    val allRentals = rentalDao.getAllRentals()
                    android.util.Log.d("MainActivity", "📦 Total rentals in database: ${allRentals.size}")
                    allRentals.forEach { rental ->
                        android.util.Log.d("MainActivity", "  🚗 Rental: ${rental.vehicleName} | User ID: ${rental.userId} | Status: ${rental.status}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error getting rentals: ${e.message}", e)
                }

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Critical error in user seeding: ${e.message}", e)
                // Don't crash the app, just log the error
            }
        }

        // ✅ PERSISTENCE VERIFICATION & CLEANUP: Log and fix stuck rentals on every app start
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for any startup operations

            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val users = db.userDao().getAllUsers()
                val rentals = db.rentalDao().getAllRentals()

                android.util.Log.d("MainActivity", "================================================================================")
                android.util.Log.d("MainActivity", "🔍 PERSISTENCE CHECK (After Startup)")
                android.util.Log.d("MainActivity", "================================================================================")
                android.util.Log.d("MainActivity", "📊 Users: ${users.size}")
                android.util.Log.d("MainActivity", "📦 Total Rentals: ${rentals.size}")

                // ✅ Count by status
                val statusCount = rentals.groupBy { it.status }.mapValues { it.value.size }
                statusCount.forEach { (status, count) ->
                    android.util.Log.d("MainActivity", "   $status: $count")
                }

                // ✅ Show all rentals with details
                if (rentals.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "")
                    android.util.Log.d("MainActivity", "📋 ALL RENTALS IN DATABASE:")
                    android.util.Log.d("MainActivity", "--------------------------------------------------------------------------------")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    rentals.forEachIndexed { index, rental ->
                        val statusEmoji = when (rental.status) {
                            "ACTIVE" -> "✅"
                            "DELIVERING" -> "🚚"
                            "OVERDUE" -> "⏰"
                            "COMPLETED" -> "✔️"
                            "CANCELLED" -> "❌"
                            else -> "📦"
                        }

                        android.util.Log.d("MainActivity", "${index + 1}. $statusEmoji ${rental.vehicleName}")
                        android.util.Log.d("MainActivity", "   ID: ${rental.id}")
                        android.util.Log.d("MainActivity", "   User: ${rental.userEmail} (UserID: ${rental.userId})")
                        android.util.Log.d("MainActivity", "   Status: ${rental.status}")
                        android.util.Log.d("MainActivity", "   Created: ${dateFormat.format(rental.createdAt)}")

                        if (rental.startDate > 0) {
                            android.util.Log.d("MainActivity", "   Start: ${dateFormat.format(rental.startDate)}")
                            android.util.Log.d("MainActivity", "   End: ${dateFormat.format(rental.endDate)}")
                            android.util.Log.d("MainActivity", "   Duration: ${rental.durationDays}d ${rental.durationHours}h ${rental.durationMinutes}m")
                        } else {
                            android.util.Log.d("MainActivity", "   Duration: ${rental.durationDays}d ${rental.durationHours}h ${rental.durationMinutes}m (not started)")
                        }

                        android.util.Log.d("MainActivity", "   Price: Rp ${"%,d".format(rental.totalPrice)}")
                        android.util.Log.d("MainActivity", "")
                    }
                }

                // ✅ FIX: Update old ACTIVE rentals from seeding to FINISHED
                val now = System.currentTimeMillis()
                val oldActiveRentals = rentals.filter { it.status == "ACTIVE" && it.endDate == it.startDate }

                if (oldActiveRentals.isNotEmpty()) {
                    android.util.Log.w("MainActivity", "⚠️ Found ${oldActiveRentals.size} old seeded ACTIVE rentals, updating to FINISHED...")
                    oldActiveRentals.forEach { rental ->
                        android.util.Log.d("MainActivity", "  ✅ Updating ${rental.vehicleName} to FINISHED")
                        // Set proper end date (7 days ago to make it look finished)
                        val finishedEndDate = rental.createdAt + (rental.durationDays * 24 * 60 * 60 * 1000L)
                        db.rentalDao().updateRentalTimes(rental.id, rental.createdAt, finishedEndDate, now)
                        db.rentalDao().updateStatus(rental.id, "FINISHED", now)
                    }
                }

                // ✅ FIX: Clean up stuck DELIVERING rentals (from previous incomplete sessions)
                val stuckDelivering = rentals.filter { it.status == "DELIVERING" }

                if (stuckDelivering.isNotEmpty()) {
                    android.util.Log.w("MainActivity", "⚠️ Found ${stuckDelivering.size} stuck DELIVERING rentals, cleaning up...")
                    stuckDelivering.forEach { rental ->
                        // If delivery was started more than 2 hours ago, cancel it
                        val ageHours = (now - rental.createdAt) / (1000 * 60 * 60)
                        if (ageHours > 2) {
                            android.util.Log.w("MainActivity", "  ❌ Cancelling old delivery: ${rental.vehicleName} (${ageHours}h old)")
                            db.rentalDao().updateStatus(rental.id, "CANCELLED", now)
                        } else {
                            android.util.Log.d("MainActivity", "  ⏳ Recent delivery (${ageHours}h old): ${rental.vehicleName} - keeping as DELIVERING")
                        }
                    }
                }

                // ✅ FIX: Update real ACTIVE rentals to OVERDUE if past end time
                val activeRentals = rentals.filter { it.status == "ACTIVE" }
                activeRentals.forEach { rental ->
                    if (rental.endDate > 0 && now > rental.endDate) {
                        android.util.Log.w("MainActivity", "⚠️ Rental ${rental.vehicleName} is overdue, updating status...")
                        db.rentalDao().updateStatus(rental.id, "OVERDUE", now)
                    }
                }

                android.util.Log.d("MainActivity", "================================================================================")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error checking persistence: ${e.message}", e)
            }
        }

        // Firebase connection & sync database lokal ke cloud
        lifecycleScope.launch {
            // ✅ CRITICAL: Wait longer to ensure seeding is fully complete
            kotlinx.coroutines.delay(3000) // Increased from 1000ms to 3000ms

            try {
                // ✅ VERIFY: Check which Firebase project is being used
                val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
                android.util.Log.d("MainActivity", "🔍 Firebase Project Info:")
                android.util.Log.d("MainActivity", "   - Project ID: ${firebaseApp.options.projectId}")
                android.util.Log.d("MainActivity", "   - Project Number: ${firebaseApp.options.gcmSenderId}")
                android.util.Log.d("MainActivity", "   - App Name: ${firebaseApp.name}")
                android.util.Log.d("MainActivity", "   - Storage Bucket: ${firebaseApp.options.storageBucket}")
                
                // Login anonim (sekali per install, otomatis reuse sesi)
                // ✅ OPSIONAL: Jika anonymous auth tidak diaktifkan, aplikasi tetap berjalan
                try {
                    Firebase.auth.signInAnonymously().await()
                    android.util.Log.d("MainActivity", "✅ Firebase Anonymous Auth berhasil")
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "⚠️ Firebase Anonymous Auth gagal (opsional): ${e.message}")
                    android.util.Log.w("MainActivity", "   💡 Untuk mengaktifkan: Firebase Console → Authentication → Sign-in method → Anonymous → Enable")
                    // Aplikasi tetap berjalan tanpa anonymous auth
                }

                // ✅ CRITICAL: Only sync if there are existing users in local
                val db = AppDatabase.getDatabase(this@MainActivity)
                val localUsers = db.userDao().getAllUsers()

                if (localUsers.isEmpty()) {
                    android.util.Log.w("MainActivity", "⚠️ No local users yet, skipping Firestore sync")
                    return@launch
                }

                android.util.Log.d("MainActivity", "📊 Found ${localUsers.size} local users, proceeding with sync...")

                // Sync database lokal ke Firestore
                try {
                    FirestoreUserService.ping()

                    // ✅ IMPORTANT: Sync local TO Firestore (not reverse!)
                    FirestoreSyncManager.syncAllLocalUsers(this@MainActivity)

                    android.util.Log.d("MainActivity", "✅ Local users synced to Firestore")


                    // Start real-time sync listener (listen perubahan dari Firestore)
                    // ⚠️ This should NOT delete dummy users (protected in FirestoreSyncManager)
                    FirestoreSyncManager.startRealtimeSync(this@MainActivity)

                    Toast.makeText(
                        this@MainActivity,
                        "✅ Database berhasil sync ke cloud",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (syncError: Exception) {
                    android.util.Log.e("MainActivity", "❌ Firestore sync gagal", syncError)
                    Toast.makeText(
                        this@MainActivity,
                        "⚠️ Sync gagal: ${syncError.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (authError: Exception) {
                android.util.Log.e("MainActivity", "❌ Firebase Auth gagal", authError)
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ Firebase offline: ${authError.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // ========== CREATE DUMMY USER - HANDLED BY AuthRepository ==========
        // The LoginViewModel already calls AuthRepository.ensureDummyPassenger()
        // No need to duplicate the logic here
        // ====================================================================

        enableEdgeToEdge()
        setContent {
            App_JalanInTheme {
                val navController = rememberNavController()
                var loggedUser by remember { mutableStateOf<String?>(null) }
                var loggedRole by remember { mutableStateOf<String?>(null) }
                var isCheckingSession by remember { mutableStateOf(true) }
                // Back to normal: start with login screen
                var initialRoute by remember { mutableStateOf("login") }

                // Rental vehicle temporary data
                var selectedRentalVehicle by remember { mutableStateOf<com.example.app_jalanin.ui.passenger.RentalVehicle?>(null) }
                var selectedRentalDuration by remember { mutableStateOf<String?>(null) }
                var selectedWithDriver by remember { mutableStateOf(false) }
                var selectedTravelDriverEmail by remember { mutableStateOf<String?>(null) } // ✅ Driver yang dipilih penumpang untuk travel (driver mengemudi kendaraan sewa)
    
    // Driver request temporary data
    var selectedDriverForRequest by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.User?>(null) }
    var selectedVehicleForRequest by remember { mutableStateOf<com.example.app_jalanin.data.model.PassengerVehicle?>(null) }
    
    // ✅ NEW: Driver rental temporary data (independent driver rental)
    var selectedDriverRentalDriverEmail by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalDriverName by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalVehicleType by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalDurationType by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalDurationCount by remember { mutableStateOf(0) }
    var selectedDriverRentalPrice by remember { mutableStateOf(0L) }
    var selectedDriverRentalPickupAddress by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalPickupLat by remember { mutableStateOf(0.0) }
    var selectedDriverRentalPickupLon by remember { mutableStateOf(0.0) }
    var selectedDriverRentalDestinationAddress by remember { mutableStateOf<String?>(null) }
    var selectedDriverRentalDestinationLat by remember { mutableStateOf<Double?>(null) }
    var selectedDriverRentalDestinationLon by remember { mutableStateOf<Double?>(null) }
    
    // Owner rental selection state
    var selectedRentalForDelivery by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.Rental?>(null) }
    var selectedRentalForEarlyReturn by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.Rental?>(null) }
    var selectedDriverForDelivery by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.User?>(null) }
    var selectedDeliveryMode by remember { mutableStateOf<String?>(null) }
                var vehicleTrackingData by remember { mutableStateOf<TrackingData?>(null) }

                // Check for saved session on app start
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    // ✅ CRITICAL: Wait for database initialization to complete
                    kotlinx.coroutines.delay(1000) // Give database time to initialize and seed
                    
                    try {
                        val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                        val savedSession = sessionManager.getSavedSession()

                        if (savedSession != null) {
                        android.util.Log.d("MainActivity", "✅ Found saved session: ${savedSession.email}, role: ${savedSession.role}")
                        loggedUser = savedSession.email
                        loggedRole = savedSession.role

                        // ✅ FIX: Also restore AuthStateManager session
                        try {
                            val db = try {
                                com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error getting database: ${e.message}", e)
                                // Clear invalid session on database error
                                sessionManager.clearSession()
                                com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)
                                initialRoute = "login"
                                isCheckingSession = false
                                return@LaunchedEffect
                            }
                            
                            val user = try {
                                db.userDao().getUserByEmail(savedSession.email)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error getting user from database: ${e.message}", e)
                                null
                            }
                            
                            if (user != null) {
                                try {
                                    com.example.app_jalanin.auth.AuthStateManager.saveCurrentUser(this@MainActivity, user)
                                    android.util.Log.d("MainActivity", "✅ AuthStateManager restored for user: ${user.email} (ID: ${user.id})")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "❌ Error saving to AuthStateManager: ${e.message}", e)
                                    // Continue anyway, session is still valid
                                }
                            } else {
                                android.util.Log.w("MainActivity", "⚠️ User not found in database: ${savedSession.email}")
                                // Clear invalid session
                                sessionManager.clearSession()
                                com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)
                                initialRoute = "login"
                                isCheckingSession = false
                                return@LaunchedEffect
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error restoring AuthStateManager: ${e.message}", e)
                            // Clear invalid session on error
                            try {
                                sessionManager.clearSession()
                                com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)
                            } catch (clearError: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error clearing session: ${clearError.message}", clearError)
                            }
                            initialRoute = "login"
                            isCheckingSession = false
                            return@LaunchedEffect
                        }

                        // Route ke dashboard sesuai role
                        initialRoute = when (savedSession.role.uppercase()) {
                            "PENUMPANG" -> "passenger_dashboard"
                            "DRIVER" -> "driver_dashboard"
                            "PEMILIK_KENDARAAN" -> "owner_dashboard"
                            else -> "passenger_dashboard" // fallback ke passenger
                        }

                        // ✅ Download payment/income history from Firestore when restoring session
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                when (savedSession.role.uppercase()) {
                                    "PENUMPANG" -> {
                                        // Download payment history for passenger
                                        android.util.Log.d("MainActivity", "📥 Downloading payment history for passenger: ${savedSession.email}")
                                        com.example.app_jalanin.data.remote.FirestorePaymentSyncManager.downloadUserPayments(
                                            this@MainActivity,
                                            savedSession.email
                                        )
                                        
                                        // Download passenger vehicles
                                        android.util.Log.d("MainActivity", "📥 Downloading passenger vehicles for: ${savedSession.email}")
                                        com.example.app_jalanin.data.remote.FirestorePassengerVehicleSyncManager.downloadPassengerVehicles(
                                            this@MainActivity,
                                            savedSession.email
                                        )
                                    }
                                    "PEMILIK_KENDARAAN" -> {
                                        // Download income history for owner
                                        android.util.Log.d("MainActivity", "📥 Downloading income history for owner: ${savedSession.email}")
                                        com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                            this@MainActivity,
                                            savedSession.email,
                                            "PEMILIK_KENDARAAN"
                                        )
                                    }
                                    "DRIVER" -> {
                                        // Download income history for driver
                                        android.util.Log.d("MainActivity", "📥 Downloading income history for driver: ${savedSession.email}")
                                        com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                            this@MainActivity,
                                            savedSession.email,
                                            "DRIVER"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error downloading payment/income history: ${e.message}", e)
                            }
                        }

                        Toast.makeText(
                            this@MainActivity,
                            "Selamat datang kembali, ${savedSession.fullName ?: savedSession.email}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.util.Log.d("MainActivity", "ℹ️ No saved session found")
                        // ✅ FIX: Ensure AuthStateManager is also cleared when no session
                        try {
                            com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error clearing AuthStateManager: ${e.message}", e)
                        }
                        initialRoute = "login"
                    }

                    isCheckingSession = false
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Critical error in session check: ${e.message}", e)
                        // Fallback to login screen on any error
                        try {
                            com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)
                        } catch (clearError: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error clearing AuthStateManager: ${clearError.message}", clearError)
                        }
                        initialRoute = "login"
                        isCheckingSession = false
                    }
                }

                if (!isCheckingSession) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = initialRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // DEBUG TEST SCREEN
                        composable("debug_test") {
                            com.example.app_jalanin.ui.debug.DebugTestScreen(navController = navController)
                        }

                        composable("login") {
                            LoginScreenWithVm(
                                onRegisterClick = { navController.navigate("register_type") },
                                onDebugScreenClick = { navController.navigate("debug_test") },
                                onAdminLoginClick = { navController.navigate("admin_login") }, // ✅ Admin login access
                                onLoginSuccess = { user, role ->
                                    loggedUser = user
                                    loggedRole = role

                                    // ✅ Initialize balance and download data from Firestore after login
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            // ✅ Initialize balance for all users on login
                                            val balanceRepository = com.example.app_jalanin.data.local.BalanceRepository(this@MainActivity)
                                            balanceRepository.initializeBalance(user)
                                            
                                            // Download balance from Firestore
                                            com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                                this@MainActivity,
                                                user
                                            )
                                            
                                            when (role.uppercase()) {
                                    "PENUMPANG" -> {
                                        // Download payment history for passenger
                                        android.util.Log.d("MainActivity", "📥 Downloading payment history for passenger: $user")
                                        com.example.app_jalanin.data.remote.FirestorePaymentSyncManager.downloadUserPayments(
                                            this@MainActivity,
                                            user
                                        )
                                        
                                        // Download passenger vehicles
                                        android.util.Log.d("MainActivity", "📥 Downloading passenger vehicles for: $user")
                                        com.example.app_jalanin.data.remote.FirestorePassengerVehicleSyncManager.downloadPassengerVehicles(
                                            this@MainActivity,
                                            user
                                        )
                                    }
                                                "PEMILIK KENDARAAN",                                                 "PEMILIK_KENDARAAN" -> {
                                                    // Download income history for owner
                                                    android.util.Log.d("MainActivity", "📥 Downloading income history for owner: $user")
                                                    com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                                        this@MainActivity,
                                                        user,
                                                        "PEMILIK_KENDARAAN"
                                                    )
                                                    
                                                    // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                                                    // Firestore balance is the SINGLE SOURCE OF TRUTH
                                                    // DO NOT recalculate balance from transaction history
                                                    com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                                        this@MainActivity,
                                                        user
                                                    )
                                                }
                                                "DRIVER" -> {
                                                    // Download income history for driver
                                                    android.util.Log.d("MainActivity", "📥 Downloading income history for driver: $user")
                                                    com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.downloadRecipientIncomes(
                                                        this@MainActivity,
                                                        user,
                                                        "DRIVER"
                                                    )
                                                    
                                                    // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                                                    // Firestore balance is the SINGLE SOURCE OF TRUTH
                                                    // DO NOT recalculate balance from transaction history
                                                    com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                                        this@MainActivity,
                                                        user
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "❌ Error initializing balance/downloading data: ${e.message}", e)
                                        }
                                    }

                                    // Navigate ke dashboard sesuai role
                                    val destination = when (role.uppercase()) {
                                        "PENUMPANG" -> "passenger_dashboard"
                                        "DRIVER" -> "driver_dashboard"
                                        "PEMILIK KENDARAAN", "PEMILIK_KENDARAAN" -> "owner_dashboard"
                                        "ADMIN" -> "admin_dashboard"
                                        else -> "passenger_dashboard" // fallback
                                    }

                                    navController.navigate(destination) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Passenger Dashboard
                        composable("passenger_dashboard") {
                            PassengerDashboardScreen(
                                username = loggedUser,
                                role = loggedRole,
                                onServiceClick = { serviceType ->
                                    when (serviceType) {
                                        "cari_driver" -> {
                                            navController.navigate("passenger_driver_list")
                                        }
                                        "sewa_kendaraan" -> navController.navigate("sewa_kendaraan")
                                        "rent_driver" -> navController.navigate("rent_driver")
                                    }
                                },
                                onChatClick = { channelId ->
                                    navController.navigate("chat/$channelId")
                                },
                                onEmergencyClick = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "🚨 Fitur Emergency Call segera hadir!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onHistoryClick = {
                                    navController.navigate("rental_history")
                                },
                                onDriverHistoryClick = {
                                    navController.navigate("passenger_driver_history")
                                },
                                onVehiclesClick = {
                                    navController.navigate("passenger_vehicles")
                                },
                                onDeleteAccount = {
                                    // Delete account PROPERLY while user is logged in
                                    android.util.Log.d("MainActivity", "🗑️ DELETE ACCOUNT initiated for: $loggedUser")

                                    lifecycleScope.launch {
                                        try {
                                            val deleteManager = com.example.app_jalanin.data.sync.DeleteAccountManager(this@MainActivity)
                                            val email = loggedUser ?: ""

                                            if (email.isBlank()) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Error: Email tidak ditemukan",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }

                                            android.util.Log.d("MainActivity", "🗑️ Deleting account: $email")
                                            android.util.Log.d("MainActivity", "🔐 User is LOGGED IN - Firebase Auth can be deleted!")

                                            val result = deleteManager.deleteAccountCompletely(email)

                                            if (result.isSuccess) {
                                                android.util.Log.d("MainActivity", "✅ Account deletion SUCCESS")

                                                // Clear session
                                                val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                                sessionManager.clearSession()

                                                // ✅ FIX: Also clear AuthStateManager
                                                com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)

                                                // Clear memory
                                                loggedUser = null
                                                loggedRole = null

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "✅ Akun berhasil dihapus dari semua sistem",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                                // Navigate to login
                                                navController.navigate("login") {
                                                    popUpTo("passenger_dashboard") { inclusive = true }
                                                }
                                            } else {
                                                android.util.Log.e("MainActivity", "❌ Account deletion FAILED: ${result.exceptionOrNull()?.message}")
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "❌ Gagal menghapus akun: ${result.exceptionOrNull()?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "❌ Delete error: ${e.message}", e)
                                            Toast.makeText(
                                                this@MainActivity,
                                                "❌ Error: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

                                    // ✅ FIX: Also clear AuthStateManager
                                    com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)

                                    // Clear user session in memory
                                    loggedUser = null
                                    loggedRole = null

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Berhasil logout",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate back to login
                                    navController.navigate("login") {
                                        popUpTo("passenger_dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }


                        // Sewa Kendaraan Screen
                        composable("sewa_kendaraan") {
                            SewaKendaraanScreen(
                                passengerEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onVehicleSelected = { vehicle, duration, withDriver, driverEmail ->
                                    // Store vehicle data in memory for confirmation screen
                                    // ✅ For rental with driver: driverEmail is the selected driver (travel driver - driver mengemudi kendaraan sewa)
                                    // ✅ For personal driver: driverEmail is null (driver drives passenger's vehicle)
                                    selectedRentalVehicle = vehicle
                                    selectedRentalDuration = duration
                                    selectedWithDriver = withDriver
                                    selectedTravelDriverEmail = driverEmail // ✅ Store selected driver email for travel driver
                                    if (withDriver && driverEmail != null) {
                                        android.util.Log.d("MainActivity", "✅ Selected travel driver: $driverEmail for vehicle: ${vehicle.name}")
                                    } else if (withDriver && driverEmail == null) {
                                        android.util.Log.d("MainActivity", "✅ Personal driver mode (driver mengemudi kendaraan penumpang)")
                                    }
                                    navController.navigate("konfirmasi_sewa")
                                }
                            )
                        }

                        // Passenger Driver List Screen
                        composable("passenger_driver_list") {
                            PassengerDriverListScreen(
                                passengerEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onDriverSelected = { driver, vehicle ->
                                    // Navigate to create request screen
                                    // Store driver and vehicle data temporarily
                                    selectedDriverForRequest = driver
                                    selectedVehicleForRequest = vehicle
                                    navController.navigate("create_driver_request")
                                }
                            )
                        }

                        // Create Driver Request Screen
                        composable("create_driver_request") {
                            if (selectedDriverForRequest != null && selectedVehicleForRequest != null) {
                                CreateDriverRequestScreen(
                                    passengerEmail = loggedUser ?: "",
                                    driver = selectedDriverForRequest!!,
                                    vehicle = selectedVehicleForRequest!!,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onRequestCreated = { request ->
                                        android.util.Log.d("MainActivity", "✅ Driver request created: ${request.id}")
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Request berhasil dikirim ke driver",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Clear temporary data
                                        selectedDriverForRequest = null
                                        selectedVehicleForRequest = null
                                        // Navigate back to passenger dashboard
                                        navController.navigate("passenger_dashboard") {
                                            popUpTo("passenger_driver_list") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                // Navigate back if data missing
                                navController.popBackStack()
                            }
                        }
                        
                        // ✅ NEW: Rent Driver Screen (independent driver rental)
                        composable("rent_driver") {
                            RentDriverScreen(
                                passengerEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onDriverSelected = { driverEmail, driverName, vehicleType, durationType, durationCount, price, pickupAddress, pickupLat, pickupLon, destinationAddress, destinationLat, destinationLon ->
                                    // Store driver rental data
                                    selectedDriverRentalDriverEmail = driverEmail
                                    selectedDriverRentalDriverName = driverName
                                    selectedDriverRentalVehicleType = vehicleType
                                    selectedDriverRentalDurationType = durationType
                                    selectedDriverRentalDurationCount = durationCount
                                    selectedDriverRentalPrice = price
                                    selectedDriverRentalPickupAddress = pickupAddress
                                    selectedDriverRentalPickupLat = pickupLat
                                    selectedDriverRentalPickupLon = pickupLon
                                    selectedDriverRentalDestinationAddress = destinationAddress
                                    selectedDriverRentalDestinationLat = destinationLat
                                    selectedDriverRentalDestinationLon = destinationLon
                                    
                                    // Navigate to confirmation screen
                                    navController.navigate("driver_rental_confirmation")
                                }
                            )
                        }
                        
                        // ✅ NEW: Driver Rental Confirmation Screen
                        composable("driver_rental_confirmation") {
                            if (selectedDriverRentalDriverEmail != null && 
                                selectedDriverRentalVehicleType != null && 
                                selectedDriverRentalDurationType != null &&
                                selectedDriverRentalPrice > 0 &&
                                selectedDriverRentalPickupAddress != null) {
                                DriverRentalConfirmationScreen(
                                    driverEmail = selectedDriverRentalDriverEmail!!,
                                    driverName = selectedDriverRentalDriverName,
                                    vehicleType = selectedDriverRentalVehicleType!!,
                                    durationType = selectedDriverRentalDurationType!!,
                                    durationCount = selectedDriverRentalDurationCount,
                                    price = selectedDriverRentalPrice,
                                    pickupAddress = selectedDriverRentalPickupAddress!!,
                                    destinationAddress = selectedDriverRentalDestinationAddress,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onConfirmPayment = { driverEmail, driverName, vehicleType, durationType, durationCount, price, paymentMethod, pickupAddress, pickupLat, pickupLon, destinationAddress, destinationLat, destinationLon ->
                                        // ✅ Create driver rental and handle balance
                                        lifecycleScope.launch {
                                            val currentUser = AuthStateManager.getCurrentUser(this@MainActivity)
                                            
                                            if (currentUser == null) {
                                                android.util.Log.e("MainActivity", "❌ No current user found")
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ Silakan login terlebih dahulu",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                return@launch
                                            }
                                            
                                            try {
                                                val now = System.currentTimeMillis()
                                                val rentalId = "DRIVER_RENT_${now}_${(1000..9999).random()}"
                                                val db = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                                                
                                                // ✅ Validate driver is still online
                                                val driverProfile = withContext(Dispatchers.IO) {
                                                    db.driverProfileDao().getByEmail(driverEmail)
                                                }
                                                
                                                if (driverProfile == null || !driverProfile.isOnline) {
                                                    android.util.Log.e("MainActivity", "❌ Driver is offline or not found: $driverEmail")
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "❌ Driver yang dipilih sedang offline. Silakan pilih driver lain.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        navController.popBackStack()
                                                    }
                                                    return@launch
                                                }
                                                
                                                // Calculate duration in milliseconds
                                                val durationMillis = when (durationType.uppercase()) {
                                                    "PER_HOUR" -> durationCount * 60 * 60 * 1000L
                                                    "PER_DAY" -> durationCount * 24 * 60 * 60 * 1000L
                                                    "PER_WEEK" -> durationCount * 7 * 24 * 60 * 60 * 1000L
                                                    else -> durationCount * 60 * 60 * 1000L
                                                }
                                                
                                                // Create DriverRental entity
                                                val driverRental = com.example.app_jalanin.data.local.entity.DriverRental(
                                                    id = rentalId,
                                                    passengerEmail = currentUser.email,
                                                    passengerName = currentUser.fullName,
                                                    driverEmail = driverEmail,
                                                    driverName = driverName,
                                                    vehicleType = vehicleType,
                                                    durationType = durationType,
                                                    durationCount = durationCount,
                                                    price = price,
                                                    paymentMethod = paymentMethod,
                                                    pickupAddress = pickupAddress,
                                                    pickupLat = pickupLat,
                                                    pickupLon = pickupLon,
                                                    destinationAddress = destinationAddress,
                                                    destinationLat = destinationLat,
                                                    destinationLon = destinationLon,
                                                    status = if (paymentMethod == "MBANKING") "CONFIRMED" else "PENDING",
                                                    confirmedAt = if (paymentMethod == "MBANKING") now else null,
                                                    createdAt = now,
                                                    updatedAt = now,
                                                    synced = false
                                                )
                                                
                                                // Save to local database
                                                db.driverRentalDao().insert(driverRental)
                                                android.util.Log.d("MainActivity", "✅ Driver rental created: $rentalId")
                                                
                                                // ✅ Update driver status to IN_RENTAL (if payment is MBANKING and confirmed)
                                                if (paymentMethod == "MBANKING") {
                                                    try {
                                                        val driverProfile = withContext(Dispatchers.IO) {
                                                            db.driverProfileDao().getByEmail(driverEmail)
                                                        }
                                                        if (driverProfile != null) {
                                                            // Note: DriverProfile doesn't have rental status field
                                                            // The rental status is tracked in DriverRental entity
                                                            // Driver remains online but has active rental
                                                            android.util.Log.d("MainActivity", "✅ Driver $driverEmail has active rental: $rentalId")
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "❌ Error updating driver status: ${e.message}", e)
                                                        // Don't fail rental creation if status update fails
                                                    }
                                                }
                                                
                                                // ✅ Handle balance for MBANKING payment
                                                if (paymentMethod == "MBANKING") {
                                                    val balanceRepository = com.example.app_jalanin.data.local.BalanceRepository(this@MainActivity)
                                                    
                                                    // Initialize balances if not exist
                                                    balanceRepository.initializeBalance(currentUser.email)
                                                    balanceRepository.initializeBalance(driverEmail)
                                                    
                                                    // Validate renter balance
                                                    val renterBalance = balanceRepository.getBalance(currentUser.email)
                                                    if (renterBalance == null || renterBalance.balance < price) {
                                                        android.util.Log.e("MainActivity", "❌ Insufficient balance for renter: ${currentUser.email}")
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "❌ Saldo tidak mencukupi. Saldo Anda: Rp ${renterBalance?.balance ?: 0}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                        // Delete the rental record
                                                        db.driverRentalDao().delete(driverRental)
                                                        return@launch
                                                    }
                                                    
                                                    // Deduct from renter balance
                                                    val debitSuccess = balanceRepository.debitBalance(
                                                        userEmail = currentUser.email,
                                                        amount = price,
                                                        source = com.example.app_jalanin.data.model.BalanceTransactionSource.RENTER_PAYMENT,
                                                        description = "Pembayaran sewa driver (${vehicleType})",
                                                        relatedUserEmail = driverEmail,
                                                        serviceType = com.example.app_jalanin.data.model.DriverServiceType.PERSONAL_VEHICLE,
                                                        rentalId = rentalId,
                                                        vehicleId = null
                                                    )
                                                    
                                                    if (!debitSuccess) {
                                                        android.util.Log.e("MainActivity", "❌ Failed to debit balance")
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "❌ Gagal memproses pembayaran. Silakan coba lagi.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                        // Delete the rental record
                                                        db.driverRentalDao().delete(driverRental)
                                                        return@launch
                                                    }
                                                    
                                                    // Credit to driver balance
                                                    balanceRepository.creditBalance(
                                                        userEmail = driverEmail,
                                                        amount = price,
                                                        source = com.example.app_jalanin.data.model.BalanceTransactionSource.DRIVER_SERVICE_FEE,
                                                        description = "Pendapatan dari sewa driver (${vehicleType})",
                                                        relatedUserEmail = currentUser.email,
                                                        serviceType = com.example.app_jalanin.data.model.DriverServiceType.PERSONAL_VEHICLE,
                                                        rentalId = rentalId,
                                                        vehicleId = null
                                                    )
                                                    
                                                    // Sync balances to Firestore
                                                    balanceRepository.syncToFirestore()
                                                    
                                                    android.util.Log.d("MainActivity", "✅ Balance updates completed for driver rental")
                                                }
                                                
                                                // ✅ Sync driver rental to Firestore
                                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                                    try {
                                                        com.example.app_jalanin.data.remote.FirestoreDriverRentalSyncManager.syncSingleRental(
                                                            this@MainActivity,
                                                            rentalId
                                                        )
                                                        android.util.Log.d("MainActivity", "✅ Driver rental synced to Firestore: $rentalId")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "❌ Error syncing driver rental: ${e.message}", e)
                                                    }
                                                }
                                                
                                                // Clear temporary data
                                                selectedDriverRentalDriverEmail = null
                                                selectedDriverRentalDriverName = null
                                                selectedDriverRentalVehicleType = null
                                                selectedDriverRentalDurationType = null
                                                selectedDriverRentalDurationCount = 0
                                                selectedDriverRentalPrice = 0L
                                                selectedDriverRentalPickupAddress = null
                                                selectedDriverRentalPickupLat = 0.0
                                                selectedDriverRentalPickupLon = 0.0
                                                selectedDriverRentalDestinationAddress = null
                                                selectedDriverRentalDestinationLat = null
                                                selectedDriverRentalDestinationLon = null
                                                
                                                // Navigate back to dashboard
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "✅ Driver rental berhasil dibuat!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    navController.navigate("passenger_dashboard") {
                                                        popUpTo("rent_driver") { inclusive = true }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainActivity", "❌ Error creating driver rental: ${e.message}", e)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ Gagal membuat sewa driver: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            } else {
                                // Navigate back if data missing
                                navController.popBackStack()
                            }
                        }

                        // Driver Requests Screen
                        composable("driver_requests") {
                            DriverRequestsScreen(
                                driverEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onRequestSelected = { request ->
                                    // Navigate to detail screen
                                    navController.navigate("driver_request_detail/${request.id}")
                                }
                            )
                        }

                        // Driver Request Detail Screen
                        composable(
                            route = "driver_request_detail/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            DriverRequestDetailScreen(
                                requestId = requestId,
                                driverEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onChatClick = { channelId ->
                                    navController.navigate("chat/$channelId")
                                },
                                onRequestAccepted = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "✅ Request diterima",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                },
                                onRequestRejected = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Request ditolak",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Konfirmasi Sewa Screen
                        composable("konfirmasi_sewa") {
                            if (selectedRentalVehicle != null && selectedRentalDuration != null) {
                                KonfirmasiSewaScreen(
                                    vehicle = selectedRentalVehicle!!,
                                    duration = selectedRentalDuration!!,
                                    withDriver = selectedWithDriver,
                                    selectedDriverEmail = selectedTravelDriverEmail, // ✅ Pass selected driver email
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onConfirmPayment = { confirmation ->
                                        android.util.Log.d("MainActivity", "🚀 onConfirmPayment called!")
                                        android.util.Log.d("MainActivity", "Vehicle: ${confirmation.vehicle.name}")
                                        android.util.Log.d("MainActivity", "Delivery: ${confirmation.deliveryAddress}")

                                        // ✅ FIX: Create rental record immediately with DELIVERING status
                                        lifecycleScope.launch {
                                            val currentUser = AuthStateManager.getCurrentUser(this@MainActivity)

                                            if (currentUser != null) {
                                                val now = System.currentTimeMillis()
                                                val rentalId = "RENT_${now}_${(1000..9999).random()}"

                                                // Calculate duration in milliseconds
                                                val durationMillis = (confirmation.durationDays * 24 * 60 * 60 * 1000L) +
                                                                    (confirmation.durationHours * 60 * 60 * 1000L) +
                                                                    (confirmation.durationMinutes * 60 * 1000L)

                                                // ✅ Get owner email from vehicle - if not set, get from database
                                                val db = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                                                var ownerEmail: String? = confirmation.vehicle.ownerEmail
                                                android.util.Log.d("MainActivity", "🔍 Vehicle ownerEmail from confirmation: $ownerEmail")
                                                
                                                // Always try to get from database to ensure we have the correct ownerId
                                                if (ownerEmail.isNullOrBlank()) {
                                                    // Fallback: Get owner from vehicle in database
                                                    android.util.Log.d("MainActivity", "⚠️ ownerEmail is blank, getting from database...")
                                                    try {
                                                        val vehicleId: Int? = confirmation.vehicle.id.toIntOrNull()
                                                        if (vehicleId != null) {
                                                            val vehicle = withContext(Dispatchers.IO) {
                                                                db.vehicleDao().getVehicleById(vehicleId)
                                                            }
                                                            ownerEmail = vehicle?.ownerId
                                                            android.util.Log.d("MainActivity", "✅ Got ownerEmail from database: $ownerEmail")
                                                        } else {
                                                            android.util.Log.e("MainActivity", "❌ Invalid vehicle ID: ${confirmation.vehicle.id}")
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "❌ Error getting vehicle owner: ${e.message}", e)
                                                    }
                                                } else {
                                                    // Double-check by getting from database to ensure consistency
                                                    try {
                                                        val vehicleId: Int? = confirmation.vehicle.id.toIntOrNull()
                                                        if (vehicleId != null) {
                                                            val vehicle = withContext(Dispatchers.IO) {
                                                                db.vehicleDao().getVehicleById(vehicleId)
                                                            }
                                                            val dbOwnerEmail = vehicle?.ownerId
                                                            if (!dbOwnerEmail.isNullOrBlank() && dbOwnerEmail != ownerEmail) {
                                                                android.util.Log.w("MainActivity", "⚠️ ownerEmail mismatch! confirmation: $ownerEmail, database: $dbOwnerEmail. Using database value.")
                                                                ownerEmail = dbOwnerEmail
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("MainActivity", "⚠️ Could not verify ownerEmail from database: ${e.message}")
                                                    }
                                                }
                                                
                                                // Final check - if still null/blank, this is an error condition
                                                // In this case, we should NOT create the rental as it won't appear in owner history
                                                if (ownerEmail.isNullOrBlank()) {
                                                    android.util.Log.e("MainActivity", "❌ ERROR: ownerEmail is still blank after all fallbacks! Cannot create rental!")
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "❌ Gagal membuat pesanan: Email pemilik tidak ditemukan. Silakan coba lagi.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        navController.popBackStack()
                                                    }
                                                    return@launch
                                                }
                                                
                                                android.util.Log.d("MainActivity", "✅ ownerEmail confirmed: $ownerEmail")

                                                // ✅ FIX: Assign driver correctly based on driver type
                                                // Priority:
                                                // 1. If vehicle has assigned driver with DELIVERY_AND_RENTAL mode: Use vehicle.driverId (assigned driver)
                                                // 2. If selectedTravelDriverEmail is set: Travel Driver (driver mengemudi kendaraan sewa)
                                                // 3. If vehicle.driverId is set: Personal Driver (driver mengemudi kendaraan penumpang)
                                                val vehicleHasAssignedDriver = confirmation.vehicle.driverId != null && 
                                                                                confirmation.vehicle.driverAssignmentMode == "DELIVERY_AND_RENTAL"
                                                
                                                val assignedDriverId = if (confirmation.withDriver) {
                                                    if (vehicleHasAssignedDriver) {
                                                        confirmation.vehicle.driverId // Use assigned driver from vehicle
                                                    } else {
                                                        selectedTravelDriverEmail ?: confirmation.vehicle.driverId
                                                    }
                                                } else {
                                                    null
                                                }
                                                
                                                val assignedTravelDriverId = if (confirmation.withDriver) {
                                                    if (vehicleHasAssignedDriver) {
                                                        confirmation.vehicle.driverId // Assigned driver is also travel driver
                                                    } else if (selectedTravelDriverEmail != null) {
                                                        selectedTravelDriverEmail // Travel driver - driver mengemudi kendaraan sewa
                                                    } else {
                                                        null
                                                    }
                                                } else {
                                                    null
                                                }
                                                
                                                // ✅ NEW: Validate assigned driver is still online before creating rental
                                                if (confirmation.withDriver && assignedDriverId != null) {
                                                    val driverProfile = withContext(Dispatchers.IO) {
                                                        db.driverProfileDao().getByEmail(assignedDriverId)
                                                    }
                                                    
                                                    if (driverProfile == null || !driverProfile.isOnline) {
                                                        android.util.Log.e("MainActivity", "❌ Assigned driver is offline or not found: $assignedDriverId")
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "❌ Driver yang ditugaskan sedang offline. Silakan pilih kendaraan lain atau coba lagi nanti.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            navController.popBackStack()
                                                        }
                                                        return@launch
                                                    }
                                                    android.util.Log.d("MainActivity", "✅ Assigned driver validated: ${assignedDriverId} is online")
                                                }
                                                
                                                android.util.Log.d("MainActivity", "🔍 Driver assignment for rental:")
                                                android.util.Log.d("MainActivity", "   - withDriver: ${confirmation.withDriver}")
                                                android.util.Log.d("MainActivity", "   - selectedTravelDriverEmail: $selectedTravelDriverEmail")
                                                android.util.Log.d("MainActivity", "   - vehicle.driverId: ${confirmation.vehicle.driverId}")
                                                android.util.Log.d("MainActivity", "   - assigned driverId: $assignedDriverId")
                                                android.util.Log.d("MainActivity", "   - assigned travelDriverId: $assignedTravelDriverId")

                                                // ✅ FIX: Create rental with PENDING status (waiting for owner to choose delivery option)
                                                // startDate and endDate will be updated when rental actually starts
                                                val rentalEntity = com.example.app_jalanin.data.local.entity.Rental(
                                                    id = rentalId,
                                                    userId = currentUser.id,
                                                    userEmail = currentUser.email,
                                                    vehicleId = confirmation.vehicle.id,
                                                    vehicleName = confirmation.vehicle.name,
                                                    vehicleType = confirmation.vehicle.type,
                                                    startDate = 0L, // Will be set when rental starts
                                                    endDate = 0L, // Will be set when rental starts
                                                    durationDays = confirmation.durationDays,
                                                    durationHours = confirmation.durationHours,
                                                    durationMinutes = confirmation.durationMinutes,
                                                    durationMillis = durationMillis,
                                                    totalPrice = confirmation.totalPrice,
                                                    status = "PENDING", // ✅ Waiting for owner to choose delivery option
                                                    overtimeFee = 0,
                                                    isWithDriver = confirmation.withDriver,
                                                    deliveryAddress = confirmation.deliveryAddress,
                                                    deliveryLat = confirmation.deliveryLocation!!.latitude,
                                                    deliveryLon = confirmation.deliveryLocation!!.longitude,
                                                    createdAt = now,
                                                    updatedAt = now,
                                                    synced = false,
                                                    driverId = assignedDriverId,
                                                    // ✅ FIX: Set travelDriverId for travel driver (driver mengemudi kendaraan sewa)
                                                    travelDriverId = assignedTravelDriverId,
                                                    driverAvailability = if (confirmation.withDriver) {
                                                        if (vehicleHasAssignedDriver) {
                                                            "AVAILABLE_FULL_RENT" // Assigned driver - driver mengemudi kendaraan sewa
                                                        } else if (selectedTravelDriverEmail != null) {
                                                            "AVAILABLE_FULL_RENT" // Travel driver - driver mengemudi kendaraan sewa
                                                        } else if (confirmation.vehicle.driverId != null) {
                                                            "AVAILABLE_FULL_RENT" // Personal driver - driver mengemudi kendaraan penumpang
                                                        } else {
                                                            confirmation.vehicle.driverAvailability
                                                        }
                                                    } else {
                                                        confirmation.vehicle.driverAvailability
                                                    },
                                                    ownerContacted = confirmation.ownerContacted,
                                                    ownerConfirmed = confirmation.ownerConfirmed,
                                                    // ✅ NEW: Set owner email so rental appears in owner's history
                                                    ownerEmail = ownerEmail
                                                )

                                                try {
                                                    db.rentalDao().insert(rentalEntity)
                                                    android.util.Log.d("MainActivity", "✅ Rental created with PENDING status: $rentalId")
                                                    android.util.Log.d("MainActivity", "   - ownerEmail: ${rentalEntity.ownerEmail}")
                                                    android.util.Log.d("MainActivity", "   - vehicleId: ${rentalEntity.vehicleId}")
                                                    android.util.Log.d("MainActivity", "   - userEmail: ${rentalEntity.userEmail}")

                                                    // ✅ NEW: Create PaymentHistory and IncomeHistory
                                                    try {
                                                        val paymentAmount = if (confirmation.paymentType == "DP") {
                                                            confirmation.totalPrice / 2 // 50% for DP
                                                        } else {
                                                            confirmation.totalPrice // Full payment
                                                        }
                                                        
                                                    // ✅ FIX: Calculate owner and driver income (80% owner, 20% driver if with driver)
                                                    // Priority: assigned driver > travel driver > personal driver
                                                    val driverEmailForIncome = if (vehicleHasAssignedDriver) {
                                                        confirmation.vehicle.driverId // Use assigned driver from vehicle
                                                    } else {
                                                        selectedTravelDriverEmail ?: confirmation.vehicle.driverId
                                                    }
                                                    val ownerIncome = if (confirmation.withDriver && driverEmailForIncome != null) {
                                                        (paymentAmount * 0.8).toInt() // 80% for owner
                                                    } else {
                                                        paymentAmount // 100% for owner if no driver
                                                    }
                                                    
                                                    val driverIncome = if (confirmation.withDriver && driverEmailForIncome != null) {
                                                        (paymentAmount * 0.2).toInt() // 20% for driver
                                                    } else {
                                                        0 // No driver income
                                                    }
                                                        
                                                        // Create PaymentHistory
                                                        val paymentHistory = com.example.app_jalanin.data.local.entity.PaymentHistory(
                                                            userId = currentUser.id,
                                                            userEmail = currentUser.email,
                                                            rentalId = rentalId,
                                                            vehicleName = confirmation.vehicle.name,
                                                            amount = paymentAmount,
                                                            paymentMethod = confirmation.paymentMethod,
                                                            paymentType = confirmation.paymentType,
                                                            ownerEmail = ownerEmail!!,
                                                            driverEmail = if (confirmation.withDriver) {
                                                                // Use same priority as assignedDriverId
                                                                if (vehicleHasAssignedDriver) {
                                                                    confirmation.vehicle.driverId
                                                                } else {
                                                                    selectedTravelDriverEmail ?: confirmation.vehicle.driverId
                                                                }
                                                            } else {
                                                                null
                                                            },
                                                            ownerIncome = ownerIncome,
                                                            driverIncome = driverIncome,
                                                            status = if (confirmation.paymentMethod == "Tunai") "PENDING" else "COMPLETED",
                                                            createdAt = now,
                                                            synced = false
                                                        )
                                                        
                                                        val paymentHistoryId = db.paymentHistoryDao().insert(paymentHistory)
                                                        android.util.Log.d("MainActivity", "✅ PaymentHistory created: $paymentHistoryId")
                                                        
                                                        // Sync PaymentHistory to Firestore
                                                        com.example.app_jalanin.data.remote.FirestorePaymentSyncManager.syncSinglePayment(
                                                            this@MainActivity,
                                                            paymentHistoryId
                                                        )
                                                        
                                                        // Create IncomeHistory for owner
                                                        val ownerIncomeHistory = com.example.app_jalanin.data.local.entity.IncomeHistory(
                                                            recipientEmail = ownerEmail!!,
                                                            recipientRole = "PEMILIK_KENDARAAN",
                                                            rentalId = rentalId,
                                                            paymentHistoryId = paymentHistoryId,
                                                            vehicleName = confirmation.vehicle.name,
                                                            passengerEmail = currentUser.email,
                                                            amount = ownerIncome,
                                                            paymentMethod = confirmation.paymentMethod,
                                                            paymentType = confirmation.paymentType,
                                                            status = if (confirmation.paymentMethod == "Tunai") "PENDING" else "COMPLETED",
                                                            createdAt = now,
                                                            synced = false
                                                        )
                                                        
                                                        val ownerIncomeHistoryId = db.incomeHistoryDao().insert(ownerIncomeHistory)
                                                        android.util.Log.d("MainActivity", "✅ Owner IncomeHistory created: $ownerIncomeHistoryId")
                                                        
                                                        // Sync Owner IncomeHistory to Firestore
                                                        com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.syncSingleIncome(
                                                            this@MainActivity,
                                                            ownerIncomeHistoryId
                                                        )
                                                        
                                                        // ✅ FIX: Create IncomeHistory for driver (if applicable)
                                                        // Driver can be travel driver (selectedTravelDriverEmail) or personal driver (vehicle.driverId)
                                                        // Note: driverEmailForIncome already declared above, reuse it
                                                        if (confirmation.withDriver && driverEmailForIncome != null && driverIncome > 0) {
                                                            val driverIncomeHistory = com.example.app_jalanin.data.local.entity.IncomeHistory(
                                                                recipientEmail = driverEmailForIncome,
                                                                recipientRole = "DRIVER",
                                                                rentalId = rentalId,
                                                                paymentHistoryId = paymentHistoryId,
                                                                vehicleName = confirmation.vehicle.name,
                                                                passengerEmail = currentUser.email,
                                                                amount = driverIncome,
                                                                paymentMethod = confirmation.paymentMethod,
                                                                paymentType = confirmation.paymentType,
                                                                status = if (confirmation.paymentMethod == "Tunai") "PENDING" else "COMPLETED",
                                                                createdAt = now,
                                                                synced = false
                                                            )
                                                            
                                                            val driverIncomeHistoryId = db.incomeHistoryDao().insert(driverIncomeHistory)
                                                            android.util.Log.d("MainActivity", "✅ Driver IncomeHistory created: $driverIncomeHistoryId")
                                                            
                                                            // Sync Driver IncomeHistory to Firestore
                                                            com.example.app_jalanin.data.remote.FirestoreIncomeSyncManager.syncSingleIncome(
                                                                this@MainActivity,
                                                                driverIncomeHistoryId
                                                            )
                                                        }
                                                        
                                                        // ✅ NEW: Update m-banking balances (all payments use m-banking)
                                                        try {
                                                            val balanceRepository = com.example.app_jalanin.data.local.BalanceRepository(this@MainActivity)
                                                            
                                                            // Initialize balances if not exist
                                                            balanceRepository.initializeBalance(currentUser.email)
                                                            balanceRepository.initializeBalance(ownerEmail!!)
                                                            if (driverEmailForIncome != null) {
                                                                balanceRepository.initializeBalance(driverEmailForIncome)
                                                            }
                                                            
                                                            // Deduct from renter balance
                                                            val debitSuccess = balanceRepository.debitBalance(
                                                                userEmail = currentUser.email,
                                                                amount = paymentAmount.toLong(),
                                                                source = com.example.app_jalanin.data.model.BalanceTransactionSource.RENTER_PAYMENT,
                                                                description = "Pembayaran sewa ${confirmation.vehicle.name}",
                                                                relatedUserEmail = ownerEmail,
                                                                rentalId = rentalId,
                                                                vehicleId = confirmation.vehicle.id.toIntOrNull()
                                                            )
                                                            
                                                            if (!debitSuccess) {
                                                                android.util.Log.e("MainActivity", "❌ Insufficient balance for renter: ${currentUser.email}")
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(
                                                                        this@MainActivity,
                                                                        "❌ Saldo tidak mencukupi. Saldo Anda: Rp ${balanceRepository.getBalance(currentUser.email)?.balance ?: 0}",
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                }
                                                                return@launch
                                                            }
                                                            
                                                            // Credit to owner balance
                                                            balanceRepository.creditBalance(
                                                                userEmail = ownerEmail!!,
                                                                amount = ownerIncome.toLong(),
                                                                source = com.example.app_jalanin.data.model.BalanceTransactionSource.OWNER_PAYMENT,
                                                                description = "Pendapatan dari sewa ${confirmation.vehicle.name}",
                                                                relatedUserEmail = currentUser.email,
                                                                rentalId = rentalId,
                                                                vehicleId = confirmation.vehicle.id.toIntOrNull()
                                                            )
                                                            
                                                            // Credit to driver balance (if applicable)
                                                            if (confirmation.withDriver && driverEmailForIncome != null && driverIncome > 0) {
                                                                balanceRepository.creditBalance(
                                                                    userEmail = driverEmailForIncome,
                                                                    amount = driverIncome.toLong(),
                                                                    source = com.example.app_jalanin.data.model.BalanceTransactionSource.DRIVER_SERVICE_FEE,
                                                                    description = "Pendapatan driver dari sewa ${confirmation.vehicle.name}",
                                                                    relatedUserEmail = currentUser.email,
                                                                    serviceType = if (vehicleHasAssignedDriver || selectedTravelDriverEmail != null) {
                                                                        com.example.app_jalanin.data.model.DriverServiceType.RENTAL_DRIVER
                                                                    } else {
                                                                        com.example.app_jalanin.data.model.DriverServiceType.PERSONAL_VEHICLE
                                                                    },
                                                                    rentalId = rentalId,
                                                                    vehicleId = confirmation.vehicle.id.toIntOrNull()
                                                                )
                                                            }
                                                            
                                                            // Sync balances to Firestore
                                                            balanceRepository.syncToFirestore()
                                                            
                                                            android.util.Log.d("MainActivity", "✅ Balance updates completed")
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("MainActivity", "❌ Error updating balances: ${e.message}", e)
                                                            // Don't fail the rental creation if balance update fails
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "❌ Error creating payment/income history: ${e.message}", e)
                                                        // Don't fail the rental creation if payment history fails
                                                    }

                                                    // Store tracking data with rentalId
                                                    vehicleTrackingData = TrackingData(
                                                        vehicle = confirmation.vehicle,
                                                        startLocation = confirmation.vehicle.location,
                                                        deliveryLocation = confirmation.deliveryLocation!!,
                                                        deliveryAddress = confirmation.deliveryAddress,
                                                        totalPrice = confirmation.totalPrice,
                                                        duration = confirmation.duration,
                                                        withDriver = confirmation.withDriver,
                                                        durationDays = confirmation.durationDays,
                                                        durationHours = confirmation.durationHours,
                                                        durationMinutes = confirmation.durationMinutes,
                                                        rentalId = rentalId
                                                    )

                                                    android.util.Log.d("MainActivity", "✅ Tracking data stored with rentalId, navigating...")

                                                    // ✅ FIX: Sync rental to Firestore in background with retry mechanism
                                                    // Use GlobalScope to ensure sync completes even if activity is paused
                                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        try {
                                                            android.util.Log.d("MainActivity", "🔄 Syncing rental to Firestore: $rentalId")
                                                            android.util.Log.d("MainActivity", "   - User: ${currentUser.email}")
                                                            android.util.Log.d("MainActivity", "   - Vehicle: ${confirmation.vehicle.name}")
                                                            android.util.Log.d("MainActivity", "   - Status: PENDING")
                                                            
                                                            // Try sync single rental first
                                                            var syncSuccess = com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                                                this@MainActivity,
                                                                rentalId
                                                            )
                                                            
                                                            if (syncSuccess) {
                                                                android.util.Log.d("MainActivity", "✅ Rental synced to Firestore successfully: $rentalId")
                                                            } else {
                                                                android.util.Log.w("MainActivity", "⚠️ Rental sync to Firestore failed, will retry with syncUnsyncedRentals")
                                                                // Retry with syncUnsyncedRentals as fallback
                                                                kotlinx.coroutines.delay(2000) // Wait 2 seconds before retry
                                                                com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncUnsyncedRentals(this@MainActivity)
                                                            }
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("MainActivity", "❌ Error syncing rental to Firestore: ${e.message}", e)
                                                            e.printStackTrace()
                                                            // Retry with syncUnsyncedRentals as fallback
                                                            try {
                                                                kotlinx.coroutines.delay(3000) // Wait 3 seconds before retry
                                                                android.util.Log.d("MainActivity", "🔄 Retrying sync with syncUnsyncedRentals...")
                                                                com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncUnsyncedRentals(this@MainActivity)
                                                            } catch (e2: Exception) {
                                                                android.util.Log.e("MainActivity", "❌ Retry sync also failed: ${e2.message}", e2)
                                                            }
                                                        }
                                                    }

                                                    // Navigate to tracking screen
                                                    withContext(Dispatchers.Main) {
                                                        navController.navigate("vehicle_tracking")
                                                        android.util.Log.d("MainActivity", "✅ Navigate called!")
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MainActivity", "❌ Error creating rental: ${e.message}", e)
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "❌ Gagal membuat pesanan: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                android.util.Log.e("MainActivity", "❌ No current user found")
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ Silakan login terlebih dahulu",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Vehicle Tracking Screen (real-time delivery tracking)
                        composable("vehicle_tracking") {
                            if (vehicleTrackingData != null) {
                                VehicleTrackingScreen(
                                    trackingData = vehicleTrackingData!!,
                                    onBackClick = {
                                        // Allow user to exit, tracking continues in background
                                        Toast.makeText(
                                            this@MainActivity,
                                            "💡 Tracking tetap berjalan. Buka di Riwayat untuk melihat lagi.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigate("passenger_dashboard") {
                                            popUpTo("passenger_dashboard") { inclusive = false }
                                        }
                                    },
                                    onVehicleArrived = {
                                        android.util.Log.d("MainActivity", "🎉 Vehicle arrived, showing arrival screen")
                                        // Navigate to vehicle arrived screen
                                        navController.navigate("vehicle_arrived")
                                    }
                                )
                            }
                        }

                        // Vehicle Arrived Screen (privacy notice & start rental)
                        composable("vehicle_arrived") {
                            if (vehicleTrackingData != null) {
                                // ✅ FIX: Don't calculate rentalEndTime here - it will be calculated when user clicks "Start Rental"
                                // This prevents the countdown from starting too early
                                VehicleArrivedScreen(
                                    vehicleName = vehicleTrackingData!!.vehicle.name,
                                    duration = vehicleTrackingData!!.duration,
                                    totalPrice = vehicleTrackingData!!.totalPrice,
                                    rentalEndTime = 0L, // Placeholder - will be calculated on button click
                                    onStartRental = {
                                        android.util.Log.d("MainActivity", "🚀 onStartRental callback invoked! User clicked 'Start Rental' button")

                                        // Validate tracking data exists
                                        if (vehicleTrackingData == null) {
                                            android.util.Log.e("MainActivity", "❌ vehicleTrackingData is NULL!")
                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "❌ Data kendaraan tidak ditemukan. Coba lagi.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            return@VehicleArrivedScreen
                                        }

                                        // ✅ FIX: Update existing rental to ACTIVE status with proper timestamps
                                        lifecycleScope.launch {
                                            try {
                                                android.util.Log.d("MainActivity", "🔍 Starting rental process (user confirmed start)...")
                                                val currentUser = AuthStateManager.getCurrentUser(this@MainActivity)

                                                if (currentUser != null && vehicleTrackingData!!.rentalId != null) {
                                                    android.util.Log.d("MainActivity", "✅ User found, updating rental to ACTIVE...")

                                                    // ✅ FIX: Use current timestamp as rental start time (when user clicks "Start Rental")
                                                    val now = System.currentTimeMillis()
                                                    val rentalId = vehicleTrackingData!!.rentalId!!

                                                    // Get duration from TrackingData (already calculated)
                                                    val durationDays = vehicleTrackingData!!.durationDays
                                                    val durationHours = vehicleTrackingData!!.durationHours
                                                    val durationMinutes = vehicleTrackingData!!.durationMinutes

                                                    // Calculate total duration in milliseconds
                                                    val durationMillis = (durationDays * 24 * 60 * 60 * 1000L) +
                                                                        (durationHours * 60 * 60 * 1000L) +
                                                                        (durationMinutes * 60 * 1000L)

                                                    // ✅ FIX: Calculate end time from NOW (when user starts rental)
                                                    val endTime = now + durationMillis

                                                    // Date formatter for logging
                                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                                                    android.util.Log.d("MainActivity", "💾 Updating rental in Room database...")
                                                    android.util.Log.d("MainActivity", "📊 Rental details:")
                                                    android.util.Log.d("MainActivity", "   - ID: $rentalId")
                                                    android.util.Log.d("MainActivity", "   - Start (user clicked button): $now (${dateFormat.format(now)})")
                                                    android.util.Log.d("MainActivity", "   - End (calculated): $endTime (${dateFormat.format(endTime)})")
                                                    android.util.Log.d("MainActivity", "   - Duration: $durationDays days, $durationHours hours, $durationMinutes minutes")

                                                    val db = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)

                                                    try {
                                                        // ✅ FIX: Get existing rental and update it
                                                        val existingRental = db.rentalDao().getRentalById(rentalId)

                                                        if (existingRental != null) {
                                                            // Update rental with ACTIVE status and proper timestamps
                                                            val updatedRental = existingRental.copy(
                                                                status = "ACTIVE",
                                                                startDate = now, // ✅ NOW is when rental actually starts
                                                                endDate = endTime, // ✅ Calculated from actual start time
                                                                updatedAt = now
                                                            )

                                                            db.rentalDao().update(updatedRental)
                                                            android.util.Log.d("MainActivity", "✅ Rental updated to ACTIVE status")
                                                            
                                                            // ✅ FIX: Update vehicle status when rental becomes ACTIVE
                                                            updateVehicleStatusForRental(updatedRental, db)

                                                            // Verify update
                                                            val savedRental = db.rentalDao().getRentalById(rentalId)
                                                            if (savedRental != null) {
                                                                android.util.Log.d("MainActivity", "✅ VERIFIED: Rental updated in database!")
                                                                android.util.Log.d("MainActivity", "   - Status: ${savedRental.status}")
                                                                android.util.Log.d("MainActivity", "   - Start: ${dateFormat.format(savedRental.startDate)}")
                                                                android.util.Log.d("MainActivity", "   - End: ${dateFormat.format(savedRental.endDate)}")
                                                            }

                                                            // 2. SYNC TO FIRESTORE IN BACKGROUND (Non-blocking)
                                                            // ✅ FIX: Use syncSingleRental for consistency
                                                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                                                try {
                                                                    android.util.Log.d("MainActivity", "🔄 Syncing updated rental to Firestore: $rentalId")
                                                                    val syncSuccess = com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                                                        this@MainActivity,
                                                                        rentalId
                                                                    )
                                                                    if (syncSuccess) {
                                                                        android.util.Log.d("MainActivity", "✅ Rental update synced to Firestore successfully")
                                                                    } else {
                                                                        android.util.Log.w("MainActivity", "⚠️ Rental update sync to Firestore failed, will retry")
                                                                        // Retry with syncUnsyncedRentals as fallback
                                                                        kotlinx.coroutines.delay(2000)
                                                                        com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncUnsyncedRentals(this@MainActivity)
                                                                    }
                                                                } catch (e: Exception) {
                                                                    android.util.Log.e("MainActivity", "❌ Error syncing rental update to Firestore: ${e.message}", e)
                                                                    // Retry with syncUnsyncedRentals as fallback
                                                                    try {
                                                                        kotlinx.coroutines.delay(3000)
                                                                        com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncUnsyncedRentals(this@MainActivity)
                                                                    } catch (e2: Exception) {
                                                                        android.util.Log.e("MainActivity", "❌ Retry sync also failed: ${e2.message}", e2)
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            android.util.Log.e("MainActivity", "❌ Rental not found: $rentalId")
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    this@MainActivity,
                                                                    "❌ Data rental tidak ditemukan",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                            return@launch
                                                        }

                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainActivity", "❌ ERROR updating rental: ${e.message}", e)
                                                        e.printStackTrace()

                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "⚠️ Gagal memulai penyewaan: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                        return@launch
                                                    }

                                                    // 3. CLEAR DATA & NAVIGATE
                                                    selectedRentalVehicle = null
                                                    selectedRentalDuration = null
                                                    selectedWithDriver = false
                                                    selectedTravelDriverEmail = null // ✅ Clear selected driver
                                                    vehicleTrackingData = null

                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "🎉 Penyewaan dimulai! Lihat di tab Riwayat.",
                                                            Toast.LENGTH_LONG
                                                        ).show()

                                                        // Navigate to dashboard with history tab selected
                                                        navController.navigate("passenger_dashboard") {
                                                            popUpTo("vehicle_arrived") { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    android.util.Log.e("MainActivity", "❌ Current user is NULL!")
                                                    android.util.Log.e("MainActivity", "🔍 Checking SharedPreferences...")

                                                    val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                                    val userId = prefs.getInt("current_user_id", -1)
                                                    val userEmail = prefs.getString("current_user_email", null)

                                                    android.util.Log.e("MainActivity", "🔍 SharedPrefs userId: $userId, email: $userEmail")

                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "⚠️ User tidak ditemukan. Silakan login ulang.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainActivity", "❌ Exception in onStartRental: ${e.message}", e)
                                                android.util.Log.e("MainActivity", "❌ Exception type: ${e.javaClass.name}")
                                                android.util.Log.e("MainActivity", "❌ Stack trace: ", e)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "⚠️ Error: ${e.message ?: e.javaClass.simpleName}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Rental History Screen
                        composable("passenger_vehicles") {
                            PassengerVehiclesScreen(
                                passengerEmail = loggedUser ?: "",
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Passenger Driver History (Order Driver)
                        composable("passenger_driver_history") {
                            com.example.app_jalanin.ui.passenger.PassengerDriverHistoryScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onChatClick = { channelId ->
                                    navController.navigate("chat/$channelId")
                                }
                            )
                        }
                        
                        composable("rental_history") {
                            RentalHistoryScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onOpenTracking = {
                                    // Navigate back to vehicle tracking if data exists
                                    if (vehicleTrackingData != null) {
                                        navController.navigate("vehicle_tracking") {
                                            popUpTo("rental_history") { inclusive = false }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "⚠️ Tidak ada tracking aktif",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onEarlyReturnClick = { rentalId ->
                                    // Load rental from database and navigate to early return request screen
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val db = AppDatabase.getDatabase(this@MainActivity)
                                        val rental = db.rentalDao().getRentalById(rentalId)
                                        withContext(Dispatchers.Main) {
                                            if (rental != null) {
                                                // Always navigate to request screen first
                                                // If return location is already set, it will navigate to map automatically
                                                selectedRentalForEarlyReturn = rental
                                                navController.navigate("early_return_request")
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "❌ Data rental tidak ditemukan",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                activeTrackingData = vehicleTrackingData // ✅ Pass active tracking data
                            )
                        }
                        
                        // ✅ NEW: Early Return Request Screen (first step - submit request)
                        composable("early_return_request") {
                            if (selectedRentalForEarlyReturn != null) {
                                val rental = selectedRentalForEarlyReturn!!
                                
                                com.example.app_jalanin.ui.passenger.EarlyReturnRequestScreen(
                                    rental = rental,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onChatClick = { channelId ->
                                        navController.navigate("chat/$channelId")
                                    },
                                    onOwnerConfirmed = {
                                        // Owner has confirmed and set return location, navigate to map
                                        val updatedRental = selectedRentalForEarlyReturn!!
                                        if (updatedRental.returnLocationLat != null && 
                                            updatedRental.returnLocationLon != null && 
                                            updatedRental.returnAddress != null) {
                                            navController.navigate("early_return") {
                                                popUpTo("early_return_request") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // ✅ Early Return Map Screen (only shown after owner confirms)
                        composable("early_return") {
                            if (selectedRentalForEarlyReturn != null) {
                                val rental = selectedRentalForEarlyReturn!!
                                var loadedRental by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.Rental?>(null) }
                                var isLoading by remember { mutableStateOf(true) }
                                
                                // Reload rental to get latest return location
                                LaunchedEffect(rental.id) {
                                    withContext(Dispatchers.IO) {
                                        val db = AppDatabase.getDatabase(this@MainActivity)
                                        val updatedRental = db.rentalDao().getRentalById(rental.id)
                                        withContext(Dispatchers.Main) {
                                            loadedRental = updatedRental
                                            isLoading = false
                                        }
                                    }
                                }
                                
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (loadedRental != null && 
                                    loadedRental!!.returnLocationLat != null && 
                                    loadedRental!!.returnLocationLon != null && 
                                    loadedRental!!.returnAddress != null) {
                                    val returnLocation = org.osmdroid.util.GeoPoint(
                                        loadedRental!!.returnLocationLat!!,
                                        loadedRental!!.returnLocationLon!!
                                    )
                                    val returnAddress = loadedRental!!.returnAddress ?: "Lokasi pengembalian"
                                    
                                    EarlyReturnScreen(
                                        rental = loadedRental!!,
                                        returnLocation = returnLocation,
                                        returnAddress = returnAddress,
                                        onBackClick = {
                                            navController.popBackStack()
                                        },
                                        onReturnCompleted = {
                                            // Update rental status to COMPLETED
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                val db = AppDatabase.getDatabase(this@MainActivity)
                                                val completedRental = loadedRental!!.copy(
                                                    status = "COMPLETED",
                                                    earlyReturnStatus = "COMPLETED",
                                                    updatedAt = System.currentTimeMillis(),
                                                    synced = false
                                                )
                                                db.rentalDao().update(completedRental)
                                                
                                                // ✅ FIX: Update vehicle status when rental is completed
                                                updateVehicleStatusForRental(completedRental, db)
                                                
                                                // Sync to Firestore
                                                com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                                    this@MainActivity,
                                                    loadedRental!!.id
                                                )
                                                
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "✅ Kendaraan berhasil dikembalikan",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    navController.popBackStack()
                                                }
                                            }
                                        },
                                        onChatClick = { channelId ->
                                            navController.navigate("chat/$channelId")
                                        }
                                    )
                                } else {
                                    // Return location not set, go back to request screen
                                    LaunchedEffect(Unit) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "⚠️ Lokasi pengembalian belum ditentukan",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigate("early_return_request") {
                                            popUpTo("early_return") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }

                        // Driver Found Screen (after booking confirmed)
                        composable("driver_found") {
                            DriverFoundScreen(
                                driverName = "Budi Santoso",
                                driverRating = 4.8f,
                                vehicleType = "Honda Vario 150",
                                vehiclePlate = "B 1234 ABC",
                                estimatedArrival = "5 menit",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onChatClick = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "💬 Fitur Chat Driver segera hadir!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onCallClick = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "📞 Fitur Call Driver segera hadir!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onCancelClick = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "❌ Pesanan dibatalkan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Navigate back to dashboard
                                    navController.navigate("passenger_dashboard") {
                                        popUpTo("passenger_dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Driver Dashboard
                        composable("driver_dashboard") {
                            DriverDashboardScreen(
                                onRequestsClick = {
                                    navController.navigate("driver_requests")
                                },
                                onRequestSelected = { request ->
                                    navController.navigate("driver_request_detail/${request.id}")
                                },
                                onChatClick = { channelId ->
                                    navController.navigate("chat/$channelId")
                                },
                                username = loggedUser,
                                role = loggedRole,
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

                                    // ✅ FIX: Also clear AuthStateManager
                                    com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)

                                    // Clear user session in memory
                                    loggedUser = null
                                    loggedRole = null

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Berhasil logout",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate back to login
                                    navController.navigate("login") {
                                        popUpTo("driver_dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Owner Dashboard
                        composable("owner_dashboard") {
                            OwnerDashboardScreen(
                                ownerEmail = loggedUser ?: "",
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

                                    // ✅ FIX: Also clear AuthStateManager
                                    com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)

                                    // Clear user session in memory
                                    loggedUser = null
                                    loggedRole = null

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Berhasil logout",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate back to login
                                    navController.navigate("login") {
                                        popUpTo("owner_dashboard") { inclusive = true }
                                    }
                                },
                                onEarlyReturnNotificationClick = { rental ->
                                    // ✅ Navigate to confirmation screen first
                                    selectedRentalForEarlyReturn = rental
                                    navController.navigate("early_return_confirmation")
                                },
                                onRentalHistoryClick = {
                                    // Handled by bottom navigation
                                },
                                onPendingRentalClick = { rental ->
                                    selectedRentalForDelivery = rental
                                    navController.navigate("owner_delivery_option")
                                },
                                onIncomeHistoryClick = {
                                    // Handled by bottom navigation
                                },
                                onAccountClick = {
                                    // Handled by bottom navigation
                                }
                            )
                        }
                        
                        // Owner Rental History
                        composable("owner_rental_history") {
                            OwnerRentalHistoryScreen(
                                ownerEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onRentalSelected = { rental ->
                                    // Navigate to rental detail or delivery option if pending
                                    if (rental.status == "PENDING") {
                                        selectedRentalForDelivery = rental
                                        navController.navigate("owner_delivery_option")
                                    }
                                },
                                onChatClick = { channelId ->
                                    // Navigate to chat
                                    navController.navigate("chat/$channelId")
                                }
                            )
                        }
                        
                        // Owner Delivery Option Screen
                        composable("owner_delivery_option") {
                            if (selectedRentalForDelivery != null) {
                                OwnerDeliveryOptionScreen(
                                    rental = selectedRentalForDelivery!!,
                                    ownerEmail = loggedUser ?: "",
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onOwnerDeliverySelected = {
                                        // Update rental with owner delivery mode
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            val database = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                                            val updatedRental = selectedRentalForDelivery!!.copy(
                                                deliveryMode = "OWNER_DELIVERY",
                                                ownerEmail = loggedUser,
                                                status = "OWNER_DELIVERING",
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            database.rentalDao().update(updatedRental)
                                            
                                            // Create DM channel with passenger
                                            val channel = com.example.app_jalanin.utils.ChatHelper.getOrCreateDMChannel(
                                                database,
                                                loggedUser ?: "",
                                                selectedRentalForDelivery!!.userEmail
                                            )
                                            
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Mode pengantaran: Owner mengantar sendiri",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedRentalForDelivery = null
                                                navController.popBackStack()
                                            }
                                        }
                                    },
                                    onDriverDeliverySelected = {
                                        // Show dialog to select delivery mode (Delivery Only or Delivery + Travel)
                                        // For now, navigate to driver selection with mode selection dialog
                                        // We'll add a dialog in the screen itself
                                        selectedDeliveryMode = "DRIVER_DELIVERY_ONLY" // Default, will be changed in screen
                                        navController.navigate("select_driver_delivery")
                                    }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }
                        
                        // Select Driver For Delivery Screen
                        composable("select_driver_delivery") {
                            if (selectedRentalForDelivery != null) {
                                SelectDriverForDeliveryScreen(
                                    rental = selectedRentalForDelivery!!,
                                    ownerEmail = loggedUser ?: "",
                                    deliveryMode = selectedDeliveryMode ?: "DRIVER_DELIVERY_ONLY",
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onDriverSelected = { driver, finalMode ->
                                        selectedDriverForDelivery = driver
                                        selectedDeliveryMode = finalMode
                                        
                                        // ✅ FIX: Update rental with driver
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            val database = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                                            
                                            val travelDriverId = if (finalMode == "DRIVER_DELIVERY_TRAVEL") {
                                                driver.email // Same driver for both delivery and travel
                                            } else {
                                                selectedRentalForDelivery!!.travelDriverId // Keep existing or null
                                            }
                                            
                                            android.util.Log.d("MainActivity", "🔍 Owner assigning driver for rental:")
                                            android.util.Log.d("MainActivity", "   - Rental ID: ${selectedRentalForDelivery!!.id}")
                                            android.util.Log.d("MainActivity", "   - Driver: ${driver.email} (${driver.fullName})")
                                            android.util.Log.d("MainActivity", "   - Delivery Mode: $finalMode")
                                            android.util.Log.d("MainActivity", "   - deliveryDriverId: ${driver.email}")
                                            android.util.Log.d("MainActivity", "   - travelDriverId: $travelDriverId")
                                            
                                            val updatedRental = selectedRentalForDelivery!!.copy(
                                                deliveryMode = finalMode,
                                                ownerEmail = loggedUser,
                                                deliveryDriverId = driver.email,
                                                // ✅ FIX: For DRIVER_DELIVERY_TRAVEL mode, also set travelDriverId
                                                travelDriverId = travelDriverId,
                                                status = "DRIVER_CONFIRMED",
                                                updatedAt = System.currentTimeMillis(),
                                                synced = false
                                            )
                                            database.rentalDao().update(updatedRental)
                                            
                                            android.util.Log.d("MainActivity", "✅ Rental updated with driver assignment")
                                            
                                            // ✅ Sync rental update to Firestore
                                            try {
                                                com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                                    this@MainActivity,
                                                    selectedRentalForDelivery!!.id
                                                )
                                                android.util.Log.d("MainActivity", "✅ Rental driver assignment synced to Firestore")
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainActivity", "❌ Error syncing rental driver assignment: ${e.message}", e)
                                            }
                                            
                                            // Create group chat channel
                                            val channel = com.example.app_jalanin.utils.ChatHelper.getOrCreateGroupChannel(
                                                database,
                                                loggedUser ?: "",
                                                driver.email,
                                                selectedRentalForDelivery!!.userEmail,
                                                selectedRentalForDelivery!!.id
                                            )
                                            
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Driver dipilih: ${driver.fullName ?: driver.email}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedRentalForDelivery = null
                                                selectedDriverForDelivery = null
                                                selectedDeliveryMode = null
                                                navController.popBackStack("owner_delivery_option", inclusive = false)
                                            }
                                        }
                                    }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }
                        
                        // Chat Screen
                        composable(
                            route = "chat/{channelId}",
                            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                            ChatScreen(
                                channelId = channelId,
                                currentUserEmail = loggedUser ?: "",
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSetReturnLocationClick = { rental ->
                                    // Navigate to set return location screen
                                    selectedRentalForEarlyReturn = rental
                                    navController.navigate("set_return_location")
                                }
                            )
                        }
                        
                        // ✅ Early Return Confirmation Screen (for owner)
                        composable("early_return_confirmation") {
                            if (selectedRentalForEarlyReturn != null) {
                                val rental = selectedRentalForEarlyReturn!!
                                
                                com.example.app_jalanin.ui.owner.EarlyReturnConfirmationScreen(
                                    rental = rental,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onConfirm = {
                                        // Navigate to set return location screen
                                        navController.navigate("set_return_location")
                                    },
                                    onReject = {
                                        // Reject request (optional - can be implemented later)
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val db = AppDatabase.getDatabase(this@MainActivity)
                                            val updatedRental = rental.copy(
                                                earlyReturnStatus = "CANCELLED",
                                                updatedAt = System.currentTimeMillis(),
                                                synced = false
                                            )
                                            db.rentalDao().update(updatedRental)
                                            com.example.app_jalanin.data.remote.FirestoreRentalSyncManager.syncSingleRental(
                                                this@MainActivity,
                                                rental.id
                                            )
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Permintaan ditolak",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.popBackStack()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // ✅ Set Return Location Screen (for owner/driver) - NEW: Uses DriverEarlyReturnViewModel
                        composable("set_return_location") {
                            if (selectedRentalForEarlyReturn != null) {
                                val rental = selectedRentalForEarlyReturn!!
                                com.example.app_jalanin.ui.owner.DriverEarlyReturnConfirmationScreen(
                                    rentalId = rental.id,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onLocationConfirmed = {
                                        // ✅ Location confirmed successfully - this triggers notification to passenger
                                        Toast.makeText(
                                            this@MainActivity,
                                            "✅ Lokasi pengembalian telah ditentukan. Penumpang akan mendapat notifikasi.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.popBackStack(route = "owner_dashboard", inclusive = false)
                                    }
                                )
                            }
                        }

                        // Admin Dashboard
                        composable("admin_dashboard") {
                            com.example.app_jalanin.ui.admin.AdminDashboardScreen(
                                username = loggedUser,
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

                                    // ✅ FIX: Also clear AuthStateManager
                                    com.example.app_jalanin.auth.AuthStateManager.clearCurrentUser(this@MainActivity)

                                    // Clear user session in memory
                                    loggedUser = null
                                    loggedRole = null

                                    Toast.makeText(
                                        this@MainActivity,
                                        "🔐 Admin logout berhasil",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate back to login
                                    navController.navigate("login") {
                                        popUpTo("admin_dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Admin Login (Hidden Access - Tap logo 7x)
                        composable("admin_login") {
                            com.example.app_jalanin.ui.admin.AdminLoginScreen(
                                onLoginSuccess = { adminEmail: String ->
                                    loggedUser = adminEmail
                                    loggedRole = "ADMIN"

                                    Toast.makeText(
                                        this@MainActivity,
                                        "✅ Admin access granted",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    navController.navigate("admin_dashboard") {
                                        popUpTo("admin_login") { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("admin_login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("register_type") {
                            AccountRegistrationTypeScreen { selected ->
                                // Navigate to simple registration form with selected role
                                navController.navigate("register/${selected.role}")
                            }
                        }
                        composable(
                            route = "register/{role}",
                            arguments = listOf(navArgument("role") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val role = backStackEntry.arguments?.getString("role") ?: "PENUMPANG"
                            val roleDisplayName = when (role) {
                                "PENUMPANG" -> "Penumpang"
                                "DRIVER" -> "Driver"
                                "PEMILIK_KENDARAAN" -> "Owner Kendaraan"
                                else -> "User"
                            }
                            SimpleRegistrationFormScreen(
                                role = role,
                                roleDisplayName = roleDisplayName,
                                onBack = { navController.popBackStack() },
                                onSuccess = {
                                    navController.popBackStack(route = "login", inclusive = false)
                                }
                            )
                        }
                    }
                    }
                } // End of if (!isCheckingSession)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop real-time sync listener untuk avoid memory leak
        FirestoreSyncManager.stopRealtimeSync()
    }

    /**
     * ✅ NEW: Update vehicle status based on rental status
     * - If rental is ACTIVE, set vehicle to SEDANG_DISEWA
     * - If rental is COMPLETED, check if there are other active rentals, if not set to TERSEDIA
     */
    private suspend fun updateVehicleStatusForRental(rental: com.example.app_jalanin.data.local.entity.Rental, db: AppDatabase) {
        try {
            val vehicleId = rental.vehicleId.toIntOrNull() ?: return
            val vehicle = db.vehicleDao().getVehicleById(vehicleId) ?: return
            
            android.util.Log.d("MainActivity", "🔄 Updating vehicle status for rental: ${rental.id}, vehicle: ${vehicle.id}, rental status: ${rental.status}")
            
            when (rental.status) {
                "ACTIVE", "DRIVER_TRAVELING", "OVERDUE", "DRIVER_TO_PASSENGER", "ARRIVED" -> {
                    // Rental is active, set vehicle to SEDANG_DISEWA
                    if (vehicle.status != com.example.app_jalanin.data.model.VehicleStatus.SEDANG_DISEWA) {
                        android.util.Log.d("MainActivity", "🔄 Setting vehicle ${vehicle.id} to SEDANG_DISEWA (rental is active)")
                        db.vehicleDao().updateVehicleStatus(
                            vehicleId = vehicle.id,
                            status = com.example.app_jalanin.data.model.VehicleStatus.SEDANG_DISEWA,
                            reason = "Kendaraan sedang disewa",
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        // Sync to Firestore
                        val updatedVehicle = vehicle.copy(
                            status = com.example.app_jalanin.data.model.VehicleStatus.SEDANG_DISEWA,
                            statusReason = "Kendaraan sedang disewa",
                            updatedAt = System.currentTimeMillis()
                        )
                        try {
                            com.example.app_jalanin.data.remote.FirestoreVehicleService.syncVehicle(updatedVehicle)
                            android.util.Log.d("MainActivity", "✅ Synced vehicle status to Firestore")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error syncing vehicle status: ${e.message}", e)
                        }
                    }
                }
                "COMPLETED", "CANCELLED" -> {
                    // Rental is completed, check if there are other active rentals for this vehicle
                    val allRentals = db.rentalDao().getRentalsByOwner(rental.ownerEmail ?: "")
                    val otherActiveRentals = allRentals.filter { r ->
                        r.id != rental.id && 
                        r.vehicleId == rental.vehicleId &&
                        r.status in listOf("ACTIVE", "DRIVER_TRAVELING", "OVERDUE", "DRIVER_TO_PASSENGER", "ARRIVED")
                    }
                    
                    if (otherActiveRentals.isEmpty()) {
                        // ✅ FIX: No other active rentals, set vehicle back to TERSEDIA
                        // Update regardless of current statusReason to fix stale data in Firestore
                        if (vehicle.status == com.example.app_jalanin.data.model.VehicleStatus.SEDANG_DISEWA) {
                            android.util.Log.d("MainActivity", "🔄 Setting vehicle ${vehicle.id} to TERSEDIA (rental COMPLETED, no other active rentals)")
                            db.vehicleDao().updateVehicleStatus(
                                vehicleId = vehicle.id,
                                status = com.example.app_jalanin.data.model.VehicleStatus.TERSEDIA,
                                reason = null,
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            // ✅ FIX: Always sync to Firestore to fix stale data
                            val updatedVehicle = vehicle.copy(
                                status = com.example.app_jalanin.data.model.VehicleStatus.TERSEDIA,
                                statusReason = null,
                                updatedAt = System.currentTimeMillis()
                            )
                            try {
                                com.example.app_jalanin.data.remote.FirestoreVehicleService.syncVehicle(updatedVehicle)
                                android.util.Log.d("MainActivity", "✅ Synced vehicle status to Firestore (fixed from SEDANG_DISEWA to TERSEDIA)")
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error syncing vehicle status: ${e.message}", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error updating vehicle status for rental: ${e.message}", e)
        }
    }

    // calculateRentalEndTime removed - now using DurationUtils.parseUserInput()
}
