package com.example.app_jalanin

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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
import com.example.app_jalanin.ui.passenger.OjekMotorBookingScreen
import com.example.app_jalanin.ui.passenger.OjekMobilBookingScreen
import com.example.app_jalanin.ui.passenger.SewaKendaraanScreen
import com.example.app_jalanin.ui.passenger.KonfirmasiSewaScreen
import com.example.app_jalanin.ui.passenger.VehicleTrackingScreen
import com.example.app_jalanin.ui.passenger.VehicleArrivedScreen
import com.example.app_jalanin.ui.passenger.RentalHistoryScreen
import com.example.app_jalanin.ui.passenger.TrackingData
import com.example.app_jalanin.ui.passenger.DriverFoundScreen
import com.example.app_jalanin.ui.driver.DriverDashboardScreen
import com.example.app_jalanin.ui.owner.OwnerDashboardScreen
import com.example.app_jalanin.ui.login.AccountLoginScreen as LoginScreenWithVm
import com.example.app_jalanin.ui.register.RegistrationFormScreen
import com.example.app_jalanin.ui.register.RegistrationFormViewModel
import com.example.app_jalanin.ui.register.AccountRegistrationTypeScreen
import com.example.app_jalanin.ui.register.MotorDriverRegistrationFormScreen
import com.example.app_jalanin.ui.register.CarDriverRegistrationFormScreen
import com.example.app_jalanin.ui.register.ReplacementDriverRegistrationFormScreen
import com.example.app_jalanin.ui.register.OwnerVehicleRegistrationFormScreen
import com.example.app_jalanin.ui.register.PassengerRegistrationFormScreen
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
                val db = AppDatabase.getDatabase(this@MainActivity)
                val userDao = db.userDao()

                val existingUsers = userDao.getAllUsers()
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
                        val insertedId = userDao.insert(user)
                        android.util.Log.d("MainActivity", "✅ SEEDED - ID: $insertedId, Email: ${user.email}, Password: ${user.password}, Role: ${user.role}")
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
                val allUsers = userDao.getAllUsers()
                android.util.Log.d("MainActivity", "✅ Total users in database: ${allUsers.size}")
                allUsers.forEach {
                    android.util.Log.d("MainActivity", "  📋 DB: ${it.email} | Role: '${it.role}' | ID: ${it.id}")
                }

                // ✅ RENTAL HISTORY DEBUG: Show rental count
                val rentalDao = db.rentalDao()
                val allRentals = rentalDao.getAllRentals()
                android.util.Log.d("MainActivity", "📦 Total rentals in database: ${allRentals.size}")
                allRentals.forEach { rental ->
                    android.util.Log.d("MainActivity", "  🚗 Rental: ${rental.vehicleName} | User ID: ${rental.userId} | Status: ${rental.status}")
                }

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error seeding dummy users: ${e.message}", e)
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
                // Login anonim (sekali per install, otomatis reuse sesi)
                Firebase.auth.signInAnonymously().await()
                android.util.Log.d("MainActivity", "✅ Firebase Auth berhasil")

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
                var vehicleTrackingData by remember { mutableStateOf<TrackingData?>(null) }

                // Check for saved session on app start
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                    val savedSession = sessionManager.getSavedSession()

                    if (savedSession != null) {
                        android.util.Log.d("MainActivity", "✅ Found saved session: ${savedSession.email}, role: ${savedSession.role}")
                        loggedUser = savedSession.email
                        loggedRole = savedSession.role

                        // Route ke dashboard sesuai role
                        initialRoute = when (savedSession.role.uppercase()) {
                            "PENUMPANG" -> "passenger_dashboard"
                            "DRIVER_MOTOR", "DRIVER_MOBIL", "DRIVER_PENGGANTI" -> "driver_dashboard"
                            "PEMILIK_KENDARAAN" -> "owner_dashboard"
                            else -> "passenger_dashboard" // fallback ke passenger
                        }

                        Toast.makeText(
                            this@MainActivity,
                            "Selamat datang kembali, ${savedSession.fullName ?: savedSession.email}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.util.Log.d("MainActivity", "ℹ️ No saved session found")
                        initialRoute = "login"
                    }

                    isCheckingSession = false
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

                                    // Navigate ke dashboard sesuai role
                                    val destination = when (role.uppercase()) {
                                        "PENUMPANG" -> "passenger_dashboard"
                                        "DRIVER MOTOR", "DRIVER MOBIL", "DRIVER PENGGANTI" -> "driver_dashboard"
                                        "DRIVER_MOTOR", "DRIVER_MOBIL", "DRIVER_PENGGANTI" -> "driver_dashboard"
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
                                        "ojek_motor" -> navController.navigate("ojek_motor_booking")
                                        "ojek_mobil" -> navController.navigate("ojek_mobil_booking")
                                        "cari_driver" -> {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Fitur Cari Driver segera hadir!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        "sewa_kendaraan" -> navController.navigate("sewa_kendaraan")
                                    }
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

                        // Ojek Motor Booking Screen
                        composable("ojek_motor_booking") {
                            OjekMotorBookingScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onBookingConfirmed = {
                                    // Langsung navigate ke driver found screen
                                    navController.navigate("driver_found")
                                }
                            )
                        }

                        // Ojek Mobil Booking Screen
                        composable("ojek_mobil_booking") {
                            OjekMobilBookingScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onBookingConfirmed = {
                                    // Langsung navigate ke driver found screen
                                    navController.navigate("driver_found")
                                }
                            )
                        }

                        // Sewa Kendaraan Screen
                        composable("sewa_kendaraan") {
                            SewaKendaraanScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onVehicleSelected = { vehicle, duration, withDriver ->
                                    // Store vehicle data in memory for confirmation screen
                                    selectedRentalVehicle = vehicle
                                    selectedRentalDuration = duration
                                    selectedWithDriver = withDriver
                                    navController.navigate("konfirmasi_sewa")
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

                                                // ✅ FIX: Create rental with DELIVERING status
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
                                                    status = "DELIVERING", // ✅ Vehicle is being delivered
                                                    overtimeFee = 0,
                                                    isWithDriver = confirmation.withDriver,
                                                    deliveryAddress = confirmation.deliveryAddress,
                                                    deliveryLat = confirmation.deliveryLocation!!.latitude,
                                                    deliveryLon = confirmation.deliveryLocation!!.longitude,
                                                    createdAt = now,
                                                    updatedAt = now,
                                                    synced = false
                                                )

                                                try {
                                                    val db = com.example.app_jalanin.data.AppDatabase.getDatabase(this@MainActivity)
                                                    db.rentalDao().insert(rentalEntity)
                                                    android.util.Log.d("MainActivity", "✅ Rental created with DELIVERING status: $rentalId")

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

                                                            // Verify update
                                                            val savedRental = db.rentalDao().getRentalById(rentalId)
                                                            if (savedRental != null) {
                                                                android.util.Log.d("MainActivity", "✅ VERIFIED: Rental updated in database!")
                                                                android.util.Log.d("MainActivity", "   - Status: ${savedRental.status}")
                                                                android.util.Log.d("MainActivity", "   - Start: ${dateFormat.format(savedRental.startDate)}")
                                                                android.util.Log.d("MainActivity", "   - End: ${dateFormat.format(savedRental.endDate)}")
                                                            }

                                                            // 2. SYNC TO FIRESTORE IN BACKGROUND (Non-blocking)
                                                            launch(Dispatchers.IO) {
                                                                try {
                                                                    android.util.Log.d("MainActivity", "🔄 Syncing to Firestore...")
                                                                    val firestoreData = FirestoreRentalService.RentalData(
                                                                        userId = currentUser.id.toString(),
                                                                        userEmail = currentUser.email,
                                                                        vehicleId = vehicleTrackingData!!.vehicle.id,
                                                                        vehicleName = vehicleTrackingData!!.vehicle.name,
                                                                        vehicleType = vehicleTrackingData!!.vehicle.type,
                                                                        startDate = now,
                                                                        endDate = endTime,
                                                                        totalPrice = vehicleTrackingData!!.totalPrice,
                                                                        status = "ACTIVE",
                                                                        isWithDriver = vehicleTrackingData!!.withDriver,
                                                                        deliveryAddress = vehicleTrackingData!!.deliveryAddress,
                                                                        deliveryLat = vehicleTrackingData!!.deliveryLocation.latitude,
                                                                        deliveryLon = vehicleTrackingData!!.deliveryLocation.longitude,
                                                                        duration = vehicleTrackingData!!.duration
                                                                    )

                                                                    val result = FirestoreRentalService.createRental(firestoreData)

                                                                    result.onSuccess {
                                                                        android.util.Log.d("MainActivity", "✅ Firestore sync successful")
                                                                        db.rentalDao().updateSyncStatus(rentalId, true)
                                                                    }.onFailure { error ->
                                                                        android.util.Log.w("MainActivity", "⚠️ Firestore sync failed (non-critical): ${error.message}")
                                                                    }
                                                                } catch (e: Exception) {
                                                                    android.util.Log.w("MainActivity", "⚠️ Firestore sync error: ${e.message}")
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
                                activeTrackingData = vehicleTrackingData // ✅ Pass active tracking data
                            )
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
                                username = loggedUser,
                                role = loggedRole,
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

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
                                }
                            )
                        }

                        // Admin Dashboard
                        composable("admin_dashboard") {
                            com.example.app_jalanin.ui.admin.AdminDashboardScreen(
                                username = loggedUser,
                                onLogout = {
                                    // Clear session from SessionManager
                                    val sessionManager = com.example.app_jalanin.data.auth.SessionManager(this@MainActivity)
                                    sessionManager.clearSession()

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
                                val id = selected.id
                                if (id != null) {
                                    navController.navigate("register/$id")
                                } else {
                                    // Penumpang → navigate ke form registrasi penumpang
                                    navController.navigate("register/passenger")
                                }
                            }
                        }
                        composable("register/passenger") {
                            val formVm: RegistrationFormViewModel = viewModel()
                            PassengerRegistrationFormScreen(
                                viewModel = formVm,
                                onBack = { navController.popBackStack() },
                                onSubmit = {
                                    navController.popBackStack(route = "login", inclusive = false)
                                }
                            )
                        }
                        composable(
                            route = "register/{typeId}",
                            arguments = listOf(navArgument("typeId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val typeId = backStackEntry.arguments?.getInt("typeId") ?: -1
                            val formVm: RegistrationFormViewModel = viewModel()
                            when (typeId) {
                                1 -> MotorDriverRegistrationFormScreen(
                                    viewModel = formVm,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { navController.popBackStack(route = "login", inclusive = false) }
                                )
                                2 -> CarDriverRegistrationFormScreen(
                                    viewModel = formVm,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { navController.popBackStack(route = "login", inclusive = false) }
                                )
                                3 -> ReplacementDriverRegistrationFormScreen(
                                    viewModel = formVm,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { navController.popBackStack(route = "login", inclusive = false) }
                                )
                                4 -> OwnerVehicleRegistrationFormScreen(
                                    viewModel = formVm,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { navController.popBackStack(route = "login", inclusive = false) }
                                )
                                else -> RegistrationFormScreen(
                                    typeId = typeId,
                                    viewModel = formVm,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { navController.popBackStack(route = "login", inclusive = false) }
                                )
                            }
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

    // calculateRentalEndTime removed - now using DurationUtils.parseUserInput()
}
