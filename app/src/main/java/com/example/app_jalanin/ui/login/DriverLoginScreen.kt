package com.example.app_jalanin.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_jalanin.R
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_jalanin.data.auth.UserRole

@Composable
fun DriverLoginScreen(
    onRegisterClick: () -> Unit,
    onLoginSuccess: (String, String) -> Unit,
    onDebugScreenClick: () -> Unit = {}, // Tambah parameter untuk navigasi ke Debug Screen
    onAdminLoginClick: () -> Unit = {}, // ✅ NEW: Admin login navigation
    vm: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    var loginTriggered by remember { mutableStateOf(false) }
    var dummyAccountIndex by remember { mutableStateOf(0) } // Track which dummy account to use

    // Ensure dummy passenger and dummy owner exists and default role
    LaunchedEffect(Unit) {
        vm.selectedRole.value = UserRole.PENUMPANG
        vm.ensureDummyPassenger() // Force check on every screen open
        vm.ensureDummyOwner() // ✅ Ensure dummy owner exists too
        vm.ensureDummyDriver() // ✅ Ensure dummy driver exists too
    }

    val success by vm.loginSuccess.collectAsStateWithLifecycle(initialValue = null)
    val lastUser by vm.lastEmail.collectAsStateWithLifecycle()
    val lastRole by vm.lastRole.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle() // ✅ NEW: Collect error message
    val showResendButton by vm.showResendButton.collectAsStateWithLifecycle() // ✅ NEW: Show resend button
    val resendCooldownSeconds: Int by vm.resendCooldownSeconds.collectAsStateWithLifecycle() // ✅ NEW: Cooldown timer

    // Define dummy accounts list
    data class DummyAccount(
        val email: String,
        val password: String,
        val role: UserRole,
        val label: String
    )

    val dummyAccounts = remember {
        listOf(
            DummyAccount(
                email = "user123@jalanin.com",
                password = "jalanin_aja_dulu",
                role = UserRole.PENUMPANG,
                label = "Dummy Penumpang"
            ),
            DummyAccount(
                email = "owner123@jalanin.com",
                password = "owner_rental_2024",
                role = UserRole.PEMILIK_KENDARAAN,
                label = "Dummy Owner Rental"
            ),
            DummyAccount(
                email = "driver123@jalanin.com",
                password = "driver_jalan_2024",
                role = UserRole.DRIVER_PENGGANTI,
                label = "Dummy Driver"
            )
        )
    }

    // ✅ NEW: Show toast for error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(success, loginTriggered) {
        if (loginTriggered && success == true && lastUser != null && lastRole != null) {
            val roleName = lastRole!!.name.lowercase().replace('_', ' ')
            Toast.makeText(context, "Login berhasil: ${lastUser} sebagai $roleName", Toast.LENGTH_SHORT).show()
            onLoginSuccess(lastUser!!, roleName)
            loginTriggered = false
        } else if (loginTriggered && success == false) {
            // Error message now handled by errorMessage StateFlow
            loginTriggered = false
        }
    }

    DriverLoginContent(
        modifier = Modifier,
        onRegisterClick = onRegisterClick,
        onLoginClick = {
            loginTriggered = true
            vm.login()
        },
        onEmailChanged = { vm.email.value = it },
        onPasswordChanged = { vm.password.value = it },
        onRoleChanged = { roleStr ->
            vm.selectedRole.value = when (roleStr) {
                "penumpang" -> UserRole.PENUMPANG
                "driver" -> UserRole.DRIVER_PENGGANTI // Changed to DRIVER_PENGGANTI to match dummy account
                "pemilik" -> UserRole.PEMILIK_KENDARAAN
                else -> UserRole.PENUMPANG
            }
        },
        onDebugAutoFill = {
            // Cycle through dummy accounts
            val account = dummyAccounts[dummyAccountIndex]
            vm.email.value = account.email
            vm.password.value = account.password
            vm.selectedRole.value = account.role

            Toast.makeText(
                context,
                "✅ Auto-filled: ${account.label}\n📧 ${account.email}",
                Toast.LENGTH_SHORT
            ).show()

            // Move to next account for next click
            dummyAccountIndex = (dummyAccountIndex + 1) % dummyAccounts.size
        },
        onDebugRecreateDummy = {
            vm.forceRecreateDummyUser()
            Toast.makeText(context, "🔄 Recreating dummy accounts...", Toast.LENGTH_SHORT).show()
        },
        onDebugScreenClick = onDebugScreenClick, // Pass parameter
        onAdminLoginClick = onAdminLoginClick, // ✅ NEW: Pass admin login callback
        emailFromVm = vm.email.collectAsStateWithLifecycle().value,
        passwordFromVm = vm.password.collectAsStateWithLifecycle().value,
        roleFromVm = vm.selectedRole.collectAsStateWithLifecycle().value,
        showResendButton = showResendButton, // ✅ NEW: Pass resend button state
        resendCooldownSeconds = resendCooldownSeconds, // ✅ NEW: Pass cooldown timer
        onResendVerification = { vm.resendVerificationEmail() }, // ✅ NEW: Resend function
        currentDummyAccountLabel = dummyAccounts[dummyAccountIndex].label // Pass current dummy account info
    )
}

@Composable
private fun DriverLoginContent(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit = {},
    onLoginClick: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onRoleChanged: (String) -> Unit = {},
    onDebugAutoFill: () -> Unit = {},
    onDebugRecreateDummy: () -> Unit = {},
    onDebugScreenClick: () -> Unit = {}, // Tambah parameter
    onAdminLoginClick: () -> Unit = {}, // ✅ NEW: Admin login navigation
    emailFromVm: String = "",
    passwordFromVm: String = "",
    roleFromVm: UserRole = UserRole.PENUMPANG,
    showResendButton: Boolean = false, // ✅ NEW: Show resend button
    resendCooldownSeconds: Int = 0, // ✅ NEW: Cooldown timer in seconds
    onResendVerification: () -> Unit = {}, // ✅ NEW: Resend verification callback
    currentDummyAccountLabel: String = "Dummy Penumpang" // ✅ NEW: Current dummy account label
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("penumpang") } // default penumpang
    var adminTapCount by remember { mutableStateOf(0) } // ✅ NEW: Hidden admin tap counter

    // Sync with ViewModel when values change
    LaunchedEffect(emailFromVm) {
        if (emailFromVm.isNotEmpty()) email = emailFromVm
    }
    LaunchedEffect(passwordFromVm) {
        if (passwordFromVm.isNotEmpty()) password = passwordFromVm
    }
    LaunchedEffect(roleFromVm) {
        role = when (roleFromVm) {
            UserRole.PENUMPANG -> "penumpang"
            UserRole.DRIVER_MOTOR, UserRole.DRIVER_MOBIL, UserRole.DRIVER_PENGGANTI -> "driver"
            UserRole.PEMILIK_KENDARAAN -> "pemilik"
            UserRole.ADMIN -> "admin"
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = "Login Akun",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Illustration (TAP 7x FOR ADMIN ACCESS)
        Image(
            painter = painterResource(id = R.drawable.driver_icon),
            contentDescription = "User Illustration",
            modifier = Modifier
                .size(80.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    adminTapCount++
                    if (adminTapCount >= 7) {
                        Toast.makeText(
                            context,
                            "🔐 Admin Access Unlocked",
                            Toast.LENGTH_SHORT
                        ).show()
                        onAdminLoginClick()
                        adminTapCount = 0
                    } else if (adminTapCount >= 4) {
                        Toast.makeText(
                            context,
                            "🤫 ${7 - adminTapCount} more taps...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Masuk sebagai Driver, Penumpang, atau Pemilik Kendaraan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role selector (vertical, using RadioButton to avoid SegmentedButton scope errors)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoleOptionItem(
                title = "Driver",
                selected = role == "driver",
                onClick = { role = "driver"; onRoleChanged("driver") },
                iconRes = R.drawable.ic_launcher_background
            )
            RoleOptionItem(
                title = "Penumpang",
                selected = role == "penumpang",
                onClick = { role = "penumpang"; onRoleChanged("penumpang") },
                iconRes = R.drawable.ic_launcher_background
            )
            RoleOptionItem(
                title = "Pemilik Kendaraan",
                selected = role == "pemilik",
                onClick = { role = "pemilik"; onRoleChanged("pemilik") },
                iconRes = R.drawable.ic_launcher_background
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onEmailChanged(it) },
            label = { Text("Email") },
            placeholder = { Text("masukkan email, contoh: user@example.com") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onPasswordChanged(it) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DEBUG SECTION - Remove in production
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = onDebugAutoFill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔧 Auto-fill: $currentDummyAccountLabel", fontSize = 12.sp)
            }
            Text(
                text = "Klik beberapa kali untuk ganti akun dummy",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDebugRecreateDummy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 DEBUG: Recreate Dummy User", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = { onLoginClick(role) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Masuk")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Register link
        TextButton(onClick = onRegisterClick) {
            Text(
                text = "Belum punya akun? Daftar sekarang",
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ✅ NEW: Resend Verification Email button (only show when needed)
        if (showResendButton) {
            Spacer(modifier = Modifier.height(8.dp))

            val isOnCooldown = resendCooldownSeconds > 0

            OutlinedButton(
                onClick = onResendVerification,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOnCooldown, // ✅ Disable button during cooldown
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isOnCooldown)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "Resend",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnCooldown) {
                        "⏱️ Tunggu ${resendCooldownSeconds}s untuk kirim ulang"
                    } else {
                        "📧 Kirim Ulang Email Verifikasi"
                    },
                    fontSize = 14.sp
                )
            }

            // ✅ Helpful message
            if (!isOnCooldown) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cek folder spam jika email tidak masuk ke inbox",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Debug Screen Button
        OutlinedButton(
            onClick = onDebugScreenClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Debug",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🔧 Debug Screen",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp)) // extra bottom padding
    }
}

@Composable
private fun RoleOptionItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
