package com.example.app_jalanin.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.entity.Rental
import com.example.app_jalanin.data.local.entity.DriverRequest
import com.example.app_jalanin.auth.AuthStateManager
import com.example.app_jalanin.utils.DurationUtils
import com.example.app_jalanin.utils.ChatHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun PassengerDashboardScreen(
    onServiceClick: (String) -> Unit = {},
    onEmergencyClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onVehiclesClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onChatClick: (String) -> Unit = {}, // channelId
    onMessageHistoryClick: () -> Unit = {}, // ✅ NEW: for message history
    onTripHistoryClick: () -> Unit = {}, // ✅ NEW: for trip history
    username: String? = null,
    role: String? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    // Load active rental for countdown
    var userEmail by remember { mutableStateOf<String?>(null) }
    
    // Load user email in coroutine
    LaunchedEffect(Unit) {
        val user = AuthStateManager.getCurrentUser(context)
        userEmail = user?.email ?: AuthStateManager.getCurrentUserEmail(context)
    }
    
    val activeRentalsFlow = remember(userEmail) {
        if (userEmail != null) {
            database.rentalDao().getActiveRentalsByEmailFlow(userEmail!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<Rental>())
        }
    }
    val activeRentalsState = activeRentalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeRental = activeRentalsState.value.firstOrNull { it.status == "ACTIVE" }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> HomeContent(
                    username = username ?: "User",
                    role = role ?: "",
                    onServiceClick = onServiceClick,
                    onEmergencyClick = onEmergencyClick,
                    activeRental = activeRental,
                    onHistoryClick = onHistoryClick
                )
                1 -> HistoryContent(
                    onHistoryClick = onHistoryClick,
                    onTripHistoryClick = onTripHistoryClick
                )
                2 -> PaymentContent()
                3 -> AccountContent(
                    username = username ?: "User",
                    role = role ?: "",
                    onVehiclesClick = onVehiclesClick,
                    onMessageHistoryClick = onMessageHistoryClick,
                    onLogout = onLogout,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    username: String,
    role: String,
    onServiceClick: (String) -> Unit,
    onEmergencyClick: () -> Unit,
    activeRental: Rental? = null,
    onHistoryClick: () -> Unit = {},
    onChatClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    // Get user email for chat
    var userEmail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val user = AuthStateManager.getCurrentUser(context)
        userEmail = user?.email ?: AuthStateManager.getCurrentUserEmail(context)
    }
    
    // ...existing code...
    val activeDriverRequestsFlow = remember(userEmail) {
        if (userEmail != null) {
            database.driverRequestDao().getActiveRequestsByPassenger(userEmail!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<DriverRequest>())
        }
    }
    val activeDriverRequestsState = activeDriverRequestsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeDriverRequest = activeDriverRequestsState.value.firstOrNull()
    
    // Get driver email from rental OR active driver request
    // Priority: 1. Active DriverRequest (Sewa Driver), 2. Rental travelDriverId, 3. Rental deliveryDriverId, 4. Rental driverId
    val driverEmail = activeDriverRequest?.driverEmail
        ?: activeRental?.travelDriverId 
        ?: activeRental?.deliveryDriverId 
        ?: activeRental?.driverId
    
    // Countdown timer for active rental
    var remainingTime by remember { mutableStateOf(0L) }
    var isOvertime by remember { mutableStateOf(false) }

    LaunchedEffect(activeRental) {
        while (activeRental != null && activeRental.status == "ACTIVE") {
            val now = System.currentTimeMillis()
            val diff = activeRental.endDate - now

            if (diff <= 0) {
                remainingTime = Math.abs(diff)
                isOvertime = true
            } else {
                remainingTime = diff
                isOvertime = false
            }

            delay(1000) // Update every second
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Header(username = username, role = role)
        
        // ✅ Balance Card
        if (userEmail != null) {
            val balanceRepository = remember { com.example.app_jalanin.data.local.BalanceRepository(context) }
            
            // Initialize and sync balance on dashboard open
            LaunchedEffect(userEmail) {
                scope.launch {
                    try {
                        // Initialize balance if not exists
                        balanceRepository.initializeBalance(userEmail!!)
                        
                        // Download balance from Firestore
                        com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                            context,
                            userEmail!!
                        )
                        
                        // Sync unsynced balance changes
                        balanceRepository.syncToFirestore()
                    } catch (e: Exception) {
                        android.util.Log.e("PassengerDashboard", "Error initializing/syncing balance: ${e.message}", e)
                    }
                }
            }
            
            com.example.app_jalanin.ui.common.BalanceCard(
                userEmail = userEmail!!,
                balanceRepository = balanceRepository,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // ✅ Active Rental Countdown Card (or Active Driver Request)
        if ((activeRental != null && activeRental.status == "ACTIVE") || activeDriverRequest != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOvertime) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isOvertime -> "⚠️ PERINGATAN KETERLAMBATAN"
                            activeDriverRequest != null -> "🚕 Driver Aktif"
                            else -> "🚗 Sewa Aktif"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOvertime -> Color(0xFFD32F2F)
                            activeDriverRequest != null -> Color(0xFF1976D2)
                            else -> Color(0xFF2E7D32)
                        }
                    )

                    // Display vehicle info if it's a rental, or driver request info
                    if (activeRental != null && activeRental.status == "ACTIVE") {
                        Text(
                            text = activeRental.vehicleName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )

                        Text(
                            text = DurationUtils.formatTime(remainingTime),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOvertime) Color(0xFFEF5350) else Color(0xFF4CAF50)
                        )
                        
                        LinearProgressIndicator(
                            progress = {
                                val totalDuration = activeRental.endDate - activeRental.startDate
                                val elapsed = System.currentTimeMillis() - activeRental.startDate
                                (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isOvertime) Color(0xFFEF5350) else Color(0xFF4CAF50),
                            trackColor = Color(0xFFC8E6C9)
                        )

                        if (isOvertime) {
                            Text(
                                text = "⚠️ Keterlambatan dikenakan Rp 50.000/jam",
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            Text(
                                text = "⚠️ Keterlambatan dikenakan Rp 50.000/jam",
                                fontSize = 11.sp,
                                color = Color(0xFF666666),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    } else if (activeDriverRequest != null) {
                        // Display driver request info
                        Text(
                            text = "Driver: ${activeDriverRequest.driverName ?: "Driver"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "${activeDriverRequest.vehicleBrand} ${activeDriverRequest.vehicleModel}",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        val statusText = when (activeDriverRequest.status) {
                            "ACCEPTED" -> "Driver Diterima"
                            "DRIVER_ARRIVING" -> "Driver Menuju Lokasi"
                            "DRIVER_ARRIVED" -> "Driver Tiba di Lokasi"
                            "IN_PROGRESS" -> "Sedang Berjalan"
                            else -> activeDriverRequest.status
                        }
                        Text(
                            text = "Status: $statusText",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // ✅ Action Buttons Row
                    if (driverEmail != null && userEmail != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Chat dengan Driver Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val channel = ChatHelper.getOrCreateDMChannel(
                                                database,
                                                userEmail!!,
                                                driverEmail
                                            )
                                            onChatClick(channel.id)
                                        } catch (e: Exception) {
                                            android.util.Log.e("PassengerDashboard", "Error creating chat channel: ${e.message}", e)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chat Driver",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            // Lihat Detail Button
                            OutlinedButton(
                                onClick = onHistoryClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(
                                    text = "Detail",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Jika tidak ada driver, hanya tampilkan tombol detail
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onHistoryClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "Lihat Detail",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        
        MainContent(onServiceClick)
        EmergencySection(onEmergencyClick)
    }
}

@Composable
private fun HistoryContent(
    onHistoryClick: () -> Unit,
    onTripHistoryClick: () -> Unit = {} // ✅ NEW: Trip History
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Riwayat",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )

        // ✅ Riwayat Sewa Driver Button (Trip History - Summary)
        Button(
            onClick = onTripHistoryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)
            )
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Riwayat Sewa Driver",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ringkasan sewa driver Anda",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Riwayat Sewa Kendaraan Button
        Button(
            onClick = onHistoryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Riwayat Sewa Kendaraan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Lihat status penyewaan kendaraan",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

    }
}

@Composable
private fun PaymentContent() {
    val context = LocalContext.current
    var userEmail by remember { mutableStateOf<String?>(null) }
    
    // Load user email
    LaunchedEffect(Unit) {
        val user = AuthStateManager.getCurrentUser(context)
        userEmail = user?.email ?: AuthStateManager.getCurrentUserEmail(context)
    }
    
    if (userEmail != null) {
        PaymentHistoryScreen(
            userEmail = userEmail!!,
            onBackClick = { /* No back action needed in tab */ }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun AccountContent(
    username: String,
    role: String,
    onVehiclesClick: () -> Unit = {},
    onMessageHistoryClick: () -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // ✅ ADDED SCROLL!
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Akun",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Menu Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AccountMenuItem(
                    icon = Icons.Filled.Person,
                    title = "Edit Profil",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider()
                AccountMenuItem(
                    icon = Icons.Filled.DirectionsCar,
                    title = "Kendaraan Saya",
                    onClick = onVehiclesClick
                )
                HorizontalDivider()
                AccountMenuItem(
                    icon = Icons.Filled.Message,
                    title = "Riwayat Pesan",
                    onClick = onMessageHistoryClick
                )
                HorizontalDivider()
                AccountMenuItem(
                    icon = Icons.Filled.Settings,
                    title = "Pengaturan",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider()
                AccountMenuItem(
                    icon = Icons.Filled.Info,
                    title = "Tentang Aplikasi",
                    onClick = { /* TODO */ }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Delete Account Button
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hapus Akun",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Akun Permanen",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("⚠️ PERINGATAN: Tindakan ini TIDAK BISA dibatalkan!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Akun Anda akan dihapus PERMANEN dari:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Database Lokal")
                    Text("• Cloud Database (Firestore)")
                    Text("• Firebase Authentication")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Anda harus login dengan akun ini terlebih dahulu untuk menghapusnya secara lengkap.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ya, Hapus Akun")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}


// Header
@Composable
private fun Header(username: String, role: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEAEAEA)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Halo, $username!", fontWeight = FontWeight.SemiBold)
                    if (role.isNotBlank()) Text("Role: ${role.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Icon(Icons.Filled.Notifications, contentDescription = null)
        }
    }
}

// Main Content
@Composable
private fun MainContent(onServiceClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchSection()
        ServicesSection(onServiceClick)
    }
}

@Composable
private fun SearchSection() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text("Cari lokasi atau alamat…") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun ServiceCard(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.width(150.dp).height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEAEAEA)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun ServicesSection(onServiceClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pilih Layanan", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ServiceCard("🚗 Sewa Kendaraan", "Harian/mingguan", icon = { Text("🚗", fontSize = 24.sp) }) { onServiceClick("sewa_kendaraan") }
            ServiceCard("🚕 Sewa Driver", "Per jam/hari/minggu", icon = { Text("🚕", fontSize = 24.sp) }) { onServiceClick("rent_driver") }
        }
    }
}

// Emergency Section
@Composable
private fun EmergencySection(onEmergencyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFECECEC)).clickable { onEmergencyClick() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFFEC1C24)) }
    }
}

// Bottom Navigation
@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
            label = { Text("Riwayat") }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Pembayaran") }
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Akun") }
        )
    }
}

