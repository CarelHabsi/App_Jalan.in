package com.example.app_jalanin

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
import com.example.app_jalanin.ui.passenger.PassengerDashboardScreen
import com.example.app_jalanin.ui.passenger.OjekMotorBookingScreen
import com.example.app_jalanin.ui.passenger.DriverFoundScreen
import com.example.app_jalanin.ui.driver.DriverDashboardScreen
import com.example.app_jalanin.ui.owner.OwnerDashboardScreen
import com.example.app_jalanin.ui.login.DriverLoginScreen as LoginScreenWithVm
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seed dummy users ke local database
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val userDao = db.userDao()

                // FORCE DELETE ALL USERS FIRST untuk fresh start (development only)
                val existingUsers = userDao.getAllUsers()
                android.util.Log.d("MainActivity", "⚠️ Found ${existingUsers.size} existing users, deleting all for fresh seed...")
                userDao.deleteAll()
                android.util.Log.d("MainActivity", "✅ All users deleted")

                // Seed dummy users dengan data FRESH
                val dummyUsers = listOf(
                    User(
                        id = 0,
                        email = "user123@jalanin.com",
                        password = "jalanin_aja_dulu",
                        role = "penumpang",
                        fullName = "Dummy User 123",
                        phoneNumber = "081234567890",
                        createdAt = System.currentTimeMillis(),
                        synced = false
                    ),
                    User(
                        id = 0,
                        email = "test@jalanin.com",
                        password = "password123",
                        role = "penumpang",
                        fullName = "Test User",
                        phoneNumber = "081234567891",
                        createdAt = System.currentTimeMillis(),
                        synced = false
                    ),
                    User(
                        id = 0,
                        email = "driver@jalanin.com",
                        password = "password123",
                        role = "driver",
                        fullName = "Driver Test",
                        phoneNumber = "081234567892",
                        createdAt = System.currentTimeMillis(),
                        synced = false
                    ),
                    User(
                        id = 0,
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

                // Verify semua dummy users ada
                val allUsers = userDao.getAllUsers()
                android.util.Log.d("MainActivity", "✅ Total users in database after seed: ${allUsers.size}")
                allUsers.forEach {
                    android.util.Log.d("MainActivity", "  📋 DB: ${it.email} | Role: '${it.role}' | Password: '${it.password}'")
                }

                android.util.Log.d("MainActivity", "✅ ✅ ✅ Seeding dummy users COMPLETE ✅ ✅ ✅")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error seeding dummy users: ${e.message}", e)
            }
        }

        // Firebase connection & sync database lokal ke cloud
        lifecycleScope.launch {
            // Wait untuk memastikan seeding selesai
            kotlinx.coroutines.delay(1000)

            try {
                // Login anonim (sekali per install, otomatis reuse sesi)
                Firebase.auth.signInAnonymously().await()
                android.util.Log.d("MainActivity", "✅ Firebase Auth berhasil")

                // Sync database lokal ke Firestore
                try {
                    FirestoreUserService.ping()
                    FirestoreSyncManager.syncAllLocalUsers(this@MainActivity)

                    // Start real-time sync listener (listen perubahan dari Firestore)
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
                                        "ojek_mobil" -> {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Fitur Ojek Mobil segera hadir!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        "cari_driver" -> {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Fitur Cari Driver segera hadir!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        "sewa_kendaraan" -> {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Fitur Sewa Kendaraan segera hadir!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onEmergencyClick = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "🚨 Fitur Emergency Call segera hadir!",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
}
