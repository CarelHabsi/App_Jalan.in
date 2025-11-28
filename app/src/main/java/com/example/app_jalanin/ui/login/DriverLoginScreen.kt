package com.example.app_jalanin.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_jalanin.R

@Composable
fun DriverLoginScreen(
    onRegisterClick: () -> Unit,
    onLoginSuccess: (String, String) -> Unit,
    viewModel: DriverLoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("penumpang") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
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

        // Illustration
        Image(
            painter = painterResource(id = R.drawable.driver_icon),
            contentDescription = "User Illustration",
            modifier = Modifier.size(80.dp)
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

        // Role selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoleOptionItem(
                title = "Driver",
                selected = selectedRole == "driver",
                onClick = { selectedRole = "driver" },
                iconRes = R.drawable.ic_launcher_background
            )
            RoleOptionItem(
                title = "Penumpang",
                selected = selectedRole == "penumpang",
                onClick = { selectedRole = "penumpang" },
                iconRes = R.drawable.ic_launcher_background
            )
            RoleOptionItem(
                title = "Pemilik Kendaraan",
                selected = selectedRole == "pemilik_kendaraan",
                onClick = { selectedRole = "pemilik_kendaraan" },
                iconRes = R.drawable.ic_launcher_background
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            placeholder = { Text("masukkan username, contoh: user123") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.login(username, password, selectedRole)
                } else {
                    Toast.makeText(
                        context,
                        "Harap isi username dan password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = loginState !is LoginState.Loading
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Masuk")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Register link
        TextButton(onClick = onRegisterClick) {
            Text(
                text = "Belum punya akun? Daftar sekarang",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Handle login state
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                val roleDisplay = when (state.role) {
                    "penumpang" -> "Penumpang"
                    "driver" -> "Driver"
                    "pemilik_kendaraan" -> "Pemilik Kendaraan"
                    else -> state.role
                }
                Toast.makeText(
                    context,
                    "Berhasil login: ${state.username} ($roleDisplay)",
                    Toast.LENGTH_SHORT
                ).show()
                onLoginSuccess(state.username, state.role)
                viewModel.resetState()
            }
            is LoginState.Error -> {
                Toast.makeText(
                    context,
                    "Silahkan masukkan kombinasi role, username, dan password yang tepat",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }
            else -> {}
        }
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
