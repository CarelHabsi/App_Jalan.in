package com.example.app_jalanin.ui.register

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerRegistrationFormScreen(
    viewModel: RegistrationFormViewModel,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrasi Penumpang") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daftar sebagai Penumpang",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                label = { Text("Username") },
                placeholder = { Text("Masukkan username") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                placeholder = { Text("Masukkan password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Konfirmasi Password") },
                placeholder = { Text("Masukkan ulang password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = "Password tidak cocok",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                label = { Text("Nama Lengkap") },
                placeholder = { Text("Masukkan nama lengkap") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Phone Number
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    errorMessage = null
                },
                label = { Text("Nomor Telepon") },
                placeholder = { Text("Contoh: 081234567890") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Error Message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    when {
                        username.isBlank() -> errorMessage = "Username tidak boleh kosong"
                        username.length < 3 -> errorMessage = "Username minimal 3 karakter"
                        password.isBlank() -> errorMessage = "Password tidak boleh kosong"
                        password.length < 6 -> errorMessage = "Password minimal 6 karakter"
                        password != confirmPassword -> errorMessage = "Password tidak cocok"
                        fullName.isBlank() -> errorMessage = "Nama lengkap tidak boleh kosong"
                        phoneNumber.isBlank() -> errorMessage = "Nomor telepon tidak boleh kosong"
                        phoneNumber.length < 10 -> errorMessage = "Nomor telepon tidak valid"
                        else -> {
                            isLoading = true
                            errorMessage = null
                            viewModel.registerUser(
                                username = username.trim(),
                                password = password,
                                role = "penumpang",
                                fullName = fullName.trim(),
                                phoneNumber = phoneNumber.trim(),
                                onSuccess = {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Registrasi berhasil! Silahkan login dengan akun baru.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onSubmit()
                                },
                                onError = { error: String ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mendaftar...")
                } else {
                    Text("Daftar", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Info text
            Text(
                text = "Dengan mendaftar, Anda menyetujui syarat dan ketentuan yang berlaku.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

