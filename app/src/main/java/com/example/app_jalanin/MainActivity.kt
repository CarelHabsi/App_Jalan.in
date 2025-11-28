package com.example.app_jalanin

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.UserRepository
import com.example.app_jalanin.ui.dashboard.DashboardScreen
import com.example.app_jalanin.ui.login.DriverLoginScreen as LoginScreenWithVm
import com.example.app_jalanin.ui.register.RegistrationFormScreen
import com.example.app_jalanin.ui.register.RegistrationFormViewModel
import com.example.app_jalanin.ui.register.AccountRegistrationTypeScreen
import com.example.app_jalanin.ui.register.RegistrationAccountType
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ========== CREATE DUMMY USER - HANYA SEKALI ==========
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDummyCreated = sharedPref.getBoolean("dummy_user_created", false)

/*        if (!isDummyCreated) {
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(this@MainActivity)
                    val repo = UserRepository(db.userDao())

                    val result = repo.registerUser(
                        username = "user123",
                        password = "jalanin_aja_dulu",
                        role = "penumpang",
                        fullName = "User Test",
                        phoneNumber = "081234567890"
                    )

                    if (result.isSuccess) {
                        sharedPref.edit().putBoolean("dummy_user_created", true).apply()
                        Log.d("MainActivity", "✅ Dummy user created successfully")
                    } else {
                        Log.e("MainActivity", "❌ Failed to create dummy user: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Exception creating dummy user", e)
                }
            }
        } else {
            Log.d("MainActivity", "ℹ️ Dummy user already exists")
      }
*/        // ========================================================

        enableEdgeToEdge()
        setContent {
            App_JalanInTheme {
                val navController = rememberNavController()
                var loggedUser by remember { mutableStateOf<String?>(null) }
                var loggedRole by remember { mutableStateOf<String?>(null) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreenWithVm(
                                onRegisterClick = { navController.navigate("register_type") },
                                onLoginSuccess = { user, role ->
                                    loggedUser = user
                                    loggedRole = role
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                username = loggedUser,
                                role = loggedRole,
                                onServiceClick = { /* TODO route by service */ },
                                onEmergencyClick = { /* TODO call emergency */ }
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
            }
        }
    }
}
