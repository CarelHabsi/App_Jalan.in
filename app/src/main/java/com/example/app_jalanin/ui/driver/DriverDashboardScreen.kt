package com.example.app_jalanin.ui.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.example.app_jalanin.data.local.entity.Rental
import com.example.app_jalanin.data.local.entity.DriverRequest
import com.example.app_jalanin.utils.ChatHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DriverDashboardScreen(
    username: String? = null,
    role: String? = null,
    onLogout: () -> Unit = {},
    onRequestsClick: () -> Unit = {},
    onRequestSelected: (DriverRequest) -> Unit = {},
    onChatClick: (String) -> Unit = {} // channelId
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            DriverBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            when (selectedTab) {
                0 -> DriverHomeContent(
                    driverEmail = username ?: "",
                    username = username ?: "Driver",
                    onRequestsClick = onRequestsClick,
                    onRequestSelected = { request ->
                        // Navigate to detail will be handled by MainActivity
                        onRequestsClick()
                    },
                    onChatClick = onChatClick
                )
                1 -> DriverOrdersContent()
                2 -> DriverEarningsContent()
                3 -> DriverAccountContent(
                    username = username ?: "Driver",
                    role = role ?: "",
                    onLogout = onLogout,
                    onRequestsClick = onRequestsClick
                )
            }
        }
    }
}

@Composable
private fun DriverHomeContent(
    driverEmail: String, 
    username: String,
    onRequestsClick: () -> Unit = {},
    onRequestSelected: (DriverRequest) -> Unit = {},
    onChatClick: (String) -> Unit = {} // channelId
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { com.example.app_jalanin.data.AppDatabase.getDatabase(context) }
    
    // Load initial online status and SIM from database
    var isOnline by remember { mutableStateOf(false) }
    var driverSimCertifications by remember { mutableStateOf<String?>(null) }
    var showSimInputDialog by remember { mutableStateOf(false) }
    
    // Load pending requests count
    val pendingRequestsFlow = remember(driverEmail) {
        if (driverEmail.isNotEmpty()) {
            database.driverRequestDao().getPendingRequestsByDriver(driverEmail)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<DriverRequest>())
        }
    }
    val pendingRequestsState = pendingRequestsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingCount = pendingRequestsState.value.size
    
    // Show notification dialog for new requests
    var showNotificationDialog by remember { mutableStateOf(false) }
    var newRequestNotification by remember { mutableStateOf<DriverRequest?>(null) }
    var previousPendingCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(pendingCount) {
        if (pendingCount > previousPendingCount && pendingRequestsState.value.isNotEmpty()) {
            // New request detected
            val newestRequest = pendingRequestsState.value.firstOrNull()
            if (newestRequest != null && newestRequest.status == "PENDING") {
                newRequestNotification = newestRequest
                showNotificationDialog = true
            }
        }
        previousPendingCount = pendingCount
    }
    
    LaunchedEffect(driverEmail) {
        if (driverEmail.isNotEmpty()) {
            scope.launch {
                try {
                    // Load driver profile from driver_profiles table
                    var profile = database.driverProfileDao().getByEmail(driverEmail)
                    
                    // If profile doesn't exist, create one
                    if (profile == null) {
                        profile = com.example.app_jalanin.data.local.entity.DriverProfile(
                            driverEmail = driverEmail,
                            simCertifications = null,
                            isOnline = false,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            synced = false
                        )
                        val profileId = database.driverProfileDao().insert(profile)
                        profile = profile.copy(id = profileId)
                        android.util.Log.d("DriverDashboard", "✅ Created new driver profile for: $driverEmail")
                    }
                    
                    isOnline = profile.isOnline
                    driverSimCertifications = profile.simCertifications
                } catch (e: Exception) {
                    android.util.Log.e("DriverDashboard", "Error loading driver profile: ${e.message}", e)
                }
            }
        }
    }
    
    // Update SIM certifications
    fun updateSimCertifications(simTypes: List<com.example.app_jalanin.data.model.SimType>) {
        if (driverEmail.isEmpty()) return
        scope.launch {
            try {
                val simString = simTypes.joinToString(",") { it.name }
                val now = System.currentTimeMillis()
                
                // Get or create driver profile
                var profile = database.driverProfileDao().getByEmail(driverEmail)
                if (profile == null) {
                    profile = com.example.app_jalanin.data.local.entity.DriverProfile(
                        driverEmail = driverEmail,
                        simCertifications = simString,
                        isOnline = false,
                        createdAt = now,
                        updatedAt = now,
                        synced = false
                    )
                    val profileId = database.driverProfileDao().insert(profile)
                    profile = profile.copy(id = profileId)
                } else {
                    // Update existing profile
                    profile = profile.copy(
                        simCertifications = simString,
                        updatedAt = now,
                        synced = false
                    )
                    database.driverProfileDao().update(profile)
                }
                
                driverSimCertifications = simString
                android.util.Log.d("DriverDashboard", "✅ Updated SIM certifications: $simString")
                
                // Sync to Firestore
                try {
                    com.example.app_jalanin.data.remote.FirestoreDriverProfileSyncManager.syncSingleProfile(
                        context,
                        profile.id
                    )
                    android.util.Log.d("DriverDashboard", "✅ SIM certifications synced to Firestore")
                } catch (e: Exception) {
                    android.util.Log.e("DriverDashboard", "❌ Error syncing SIM to Firestore: ${e.message}", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverDashboard", "❌ Error updating SIM: ${e.message}", e)
            }
        }
    }
    
    // Save online status to database when changed
    fun updateOnlineStatus(newStatus: Boolean) {
        if (driverEmail.isEmpty()) return
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                
                // Get or create driver profile
                var profile = database.driverProfileDao().getByEmail(driverEmail)
                if (profile == null) {
                    profile = com.example.app_jalanin.data.local.entity.DriverProfile(
                        driverEmail = driverEmail,
                        simCertifications = null,
                        isOnline = newStatus,
                        createdAt = now,
                        updatedAt = now,
                        synced = false
                    )
                    val profileId = database.driverProfileDao().insert(profile)
                    profile = profile.copy(id = profileId)
                } else {
                    // Update existing profile
                    profile = profile.copy(
                        isOnline = newStatus,
                        updatedAt = now,
                        synced = false
                    )
                    database.driverProfileDao().update(profile)
                }
                
                isOnline = newStatus
                android.util.Log.d("DriverDashboard", "✅ Updated online status: $newStatus")
                
                // Sync to Firestore
                try {
                    com.example.app_jalanin.data.remote.FirestoreDriverProfileSyncManager.syncSingleProfile(
                        context,
                        profile.id
                    )
                    android.util.Log.d("DriverDashboard", "✅ Online status synced to Firestore")
                } catch (e: Exception) {
                    android.util.Log.e("DriverDashboard", "❌ Error syncing online status to Firestore: ${e.message}", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverDashboard", "❌ Error updating online status: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dashboard Driver",
                color = Color(0xFF424242),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ✅ Balance Card
            if (driverEmail.isNotEmpty()) {
                val balanceRepository = remember { com.example.app_jalanin.data.local.BalanceRepository(context) }
                
                // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                // Firestore balance is the SINGLE SOURCE OF TRUTH
                // DO NOT recalculate balance from transaction history
                LaunchedEffect(driverEmail) {
                    scope.launch {
                        try {
                            // Initialize balance if not exists (only creates if missing, never resets)
                            balanceRepository.initializeBalance(driverEmail)
                            
                            // Download balance from Firestore (READ-ONLY operation)
                            // This is the ONLY balance update during login/dashboard open
                            com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                                context,
                                driverEmail
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("DriverDashboard", "Error downloading balance: ${e.message}", e)
                        }
                    }
                }
                
                com.example.app_jalanin.ui.common.BalanceCard(
                    userEmail = driverEmail,
                    balanceRepository = balanceRepository,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Status Driver Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Status Driver",
                        color = Color(0xFF424242),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Toggle Switch
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { newStatus -> updateOnlineStatus(newStatus) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF22C55E),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF9E9E9E)
                            )
                        )

                        Text(
                            text = "ONLINE",
                            color = Color(0xFF06A870),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Pendapatan Hari Ini",
                    value = "Rp ---",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Order Selesai",
                    value = "--- trip",
                    modifier = Modifier.weight(1f)
                )
            }

            // SIM Info Card
            if (driverSimCertifications.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    border = BorderStroke(1.dp, Color(0xFFFFC107))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFF856404)
                            )
                    Text(
                                text = "SIM Belum Diinput",
                                color = Color(0xFF856404),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                        }
                        Text(
                            text = "Anda perlu menginput SIM untuk dapat menerima order. Klik tombol di bawah untuk input SIM.",
                            color = Color(0xFF856404),
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { showSimInputDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF856404)
                            )
                        ) {
                            Icon(Icons.Default.DriveEta, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Input SIM")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Column {
                            Text(
                                text = "SIM Anda",
                                color = Color(0xFF757575),
                                fontSize = 12.sp
                                )
                                Text(
                                text = driverSimCertifications?.replace("SIM_", "SIM ") ?: "-",
                                    color = Color(0xFF424242),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                                )
                            }
                        TextButton(onClick = { showSimInputDialog = true }) {
                            Text("Edit")
                        }
                    }
                }
            }

            // Pending Requests Card
            if (driverEmail.isNotEmpty() && !driverSimCertifications.isNullOrBlank()) {
                Card(
                                modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestsClick),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingCount > 0) Color(0xFFFFF3E0) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (pendingCount > 0) Color(0xFFFF9800) else Color(0xFFE0E0E0)
                    )
                ) {
                            Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (pendingCount > 0) Color(0xFFFF9800) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                            Text(
                                    text = "Request Driver",
                                color = Color(0xFF424242),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                                Text(
                                    text = if (pendingCount > 0) {
                                        "$pendingCount request menunggu konfirmasi"
                                    } else {
                                        "Tidak ada request pending"
                                    },
                                    color = if (pendingCount > 0) Color(0xFFFF9800) else Color(0xFF757575),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (pendingCount > 0) {
                            Badge(
                                containerColor = Color(0xFFFF9800)
                    ) {
                        Text(
                                    text = pendingCount.toString(),
                            color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                        )
                            }
                        } else {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
                
                // ✅ Pending Requests Summary (Ringkas)
                if (pendingCount > 0 && pendingRequestsState.value.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Request Pending",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                                TextButton(onClick = onRequestsClick) {
                                    Text("Lihat Semua", fontSize = 12.sp)
                                }
                            }
                            
                            // Show first 3 requests only (summary)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(pendingRequestsState.value.take(3)) { request ->
                                    DriverRequestSummaryCard(
                                        request = request,
                                        onClick = { onRequestSelected(request) }
                                    )
                                }
                            }
                            
                            if (pendingCount > 3) {
                                Text(
                                    text = "+${pendingCount - 3} request lainnya",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Order Aktif Card
            DriverActiveOrdersCard(
                driverEmail = driverEmail,
                database = database,
                onChatClick = onChatClick,
                onRequestSelected = onRequestSelected
            )

            // SIM Input Dialog
            if (showSimInputDialog) {
                DriverSimInputDialog(
                    currentSimCertifications = driverSimCertifications,
                    onDismiss = { showSimInputDialog = false },
                    onConfirm = { simTypes ->
                        updateSimCertifications(simTypes)
                        showSimInputDialog = false
                    }
                )
            }
            
            // Notification Dialog for new request
            if (showNotificationDialog && newRequestNotification != null) {
                AlertDialog(
                    onDismissRequest = { showNotificationDialog = false },
                    icon = {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                    },
                    title = {
                        Text("Request Baru!")
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                                text = "Anda mendapat request dari:",
                                fontSize = 14.sp
                            )
                            Text(
                                text = newRequestNotification!!.passengerName,
                    fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kendaraan: ${newRequestNotification!!.vehicleBrand} ${newRequestNotification!!.vehicleModel}",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                            Text(
                                text = "Lokasi: ${newRequestNotification!!.pickupAddress}",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNotificationDialog = false
                                onRequestsClick()
                            }
                        ) {
                            Text("Lihat Detail")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationDialog = false }) {
                            Text("Nanti")
            }
        }
                )
    }
}
    }
}

/**
 * Card untuk menampilkan order aktif driver
 */
@Composable
private fun DriverActiveOrdersCard(
    driverEmail: String,
    database: com.example.app_jalanin.data.AppDatabase,
    onChatClick: (String) -> Unit = {},
    onRequestSelected: (DriverRequest) -> Unit = {}
) {
    val activeRequestsFlow = remember(driverEmail) {
        if (driverEmail.isNotEmpty()) {
            database.driverRequestDao().getActiveRequestsByDriver(driverEmail)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<DriverRequest>())
        }
    }
    val activeRequests by activeRequestsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Aktif",
                    color = Color(0xFF424242),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${activeRequests.size} order",
                    color = Color(0xFF757575),
                    fontSize = 12.sp
                )
            }
            
            if (activeRequests.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Tidak ada order aktif",
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pastikan status Anda ONLINE untuk menerima order",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp
                    )
                }
            } else {
                activeRequests.forEach { request ->
                    DriverActiveOrderItem(
                        request = request,
                        driverEmail = driverEmail,
                        database = database,
                        onChatClick = onChatClick,
                        onRequestClick = { onRequestSelected(request) }
                    )
                    if (request != activeRequests.last()) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Item untuk menampilkan detail order aktif dari DriverRequest
 */
@Composable
private fun DriverActiveOrderItem(
    request: DriverRequest,
    driverEmail: String,
    database: com.example.app_jalanin.data.AppDatabase,
    onChatClick: (String) -> Unit,
    onRequestClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    
    val statusColor = when (request.status) {
        "ACCEPTED" -> Color(0xFF2196F3)
        "DRIVER_ARRIVING" -> Color(0xFF9C27B0)
        "DRIVER_ARRIVED" -> Color(0xFF4CAF50)
        "IN_PROGRESS" -> Color(0xFF00BCD4)
        else -> Color(0xFF9E9E9E)
    }
    
    val statusText = when (request.status) {
        "ACCEPTED" -> "Diterima"
        "DRIVER_ARRIVING" -> "Menuju Lokasi"
        "DRIVER_ARRIVED" -> "Tiba di Lokasi"
        "IN_PROGRESS" -> "Sedang Berjalan"
        else -> request.status
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRequestClick),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.passengerName,
                    color = Color(0xFF424242),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        if (request.vehicleType == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF757575)
                    )
                    Text(
                        text = "${request.vehicleBrand} ${request.vehicleModel}",
                        color = Color(0xFF757575),
                        fontSize = 12.sp
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4CAF50)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.pickupAddress,
                    color = Color(0xFF424242),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (request.destinationAddress != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Text(
                            text = request.destinationAddress ?: "",
                            color = Color(0xFF757575),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dibuat: ${dateFormat.format(request.createdAt)}",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chat Button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val channel = com.example.app_jalanin.utils.ChatHelper.getOrCreateDMChannel(
                                    database,
                                    driverEmail,
                                    request.passengerEmail
                                )
                                onChatClick(channel.id)
                            } catch (e: Exception) {
                                android.util.Log.e("DriverActiveOrder", "Error creating chat: ${e.message}", e)
                            }
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chat",
                        fontSize = 11.sp
                    )
                }
                // View Detail Button
                OutlinedButton(
                    onClick = onRequestClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Detail",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Item untuk menampilkan detail order (LEGACY - untuk Rental)
 */
@Composable
private fun DriverOrderItem(rental: Rental) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = rental.vehicleName,
                color = Color(0xFF424242),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (rental.status) {
                    "DELIVERING" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    "ACTIVE" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = when (rental.status) {
                        "DELIVERING" -> "Mengantar"
                        "ACTIVE" -> "Aktif"
                        else -> rental.status
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (rental.status) {
                        "DELIVERING" -> Color(0xFFE65100)
                        "ACTIVE" -> Color(0xFF2E7D32)
                        else -> Color(0xFF616161)
                    }
                )
            }
        }
        
        if (rental.deliveryAddress.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    Icons.Default.LocationOn,
                contentDescription = null,
                    tint = Color(0xFF06A870),
                    modifier = Modifier.size(16.dp)
            )
            Text(
                    text = rental.deliveryAddress,
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
            )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total: ${currencyFormat.format(rental.totalPrice)}",
                color = Color(0xFF424242),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = rental.getShortDuration(),
                color = Color(0xFF757575),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DriverAccountContent(
    onRequestsClick: () -> Unit = {},
    username: String,
    role: String,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSimInputDialog by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { com.example.app_jalanin.data.AppDatabase.getDatabase(context) }
    
    var driverSimCertifications by remember { mutableStateOf<String?>(null) }
    var driverEmail by remember { mutableStateOf(username) }
    
    LaunchedEffect(driverEmail) {
        if (driverEmail.isNotEmpty()) {
            scope.launch {
                try {
                    // Load driver profile from driver_profiles table
                    val profile = database.driverProfileDao().getByEmail(driverEmail)
                    driverSimCertifications = profile?.simCertifications
                } catch (e: Exception) {
                    android.util.Log.e("DriverAccount", "Error loading SIM: ${e.message}", e)
                }
            }
        }
    }
    
    fun updateSimCertifications(simTypes: List<com.example.app_jalanin.data.model.SimType>) {
        if (driverEmail.isEmpty()) return
        scope.launch {
            try {
                val simString = simTypes.joinToString(",") { it.name }
                val now = System.currentTimeMillis()
                
                // Get or create driver profile
                var profile = database.driverProfileDao().getByEmail(driverEmail)
                if (profile == null) {
                    profile = com.example.app_jalanin.data.local.entity.DriverProfile(
                        driverEmail = driverEmail,
                        simCertifications = simString,
                        isOnline = false,
                        createdAt = now,
                        updatedAt = now,
                        synced = false
                    )
                    val profileId = database.driverProfileDao().insert(profile)
                    profile = profile.copy(id = profileId)
                } else {
                    // Update existing profile
                    profile = profile.copy(
                        simCertifications = simString,
                        updatedAt = now,
                        synced = false
                    )
                    database.driverProfileDao().update(profile)
                }
                
                driverSimCertifications = simString
                android.util.Log.d("DriverAccount", "✅ Updated SIM certifications: $simString")
                
                // Sync to Firestore
                try {
                    com.example.app_jalanin.data.remote.FirestoreDriverProfileSyncManager.syncSingleProfile(
                        context,
                        profile.id
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DriverAccount", "❌ Error syncing SIM to Firestore: ${e.message}", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverAccount", "Error updating SIM: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Akun",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
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
                        .background(Color(0xFF06A870)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text(
                        text = username,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Text(
                        text = "Driver",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }

        // SIM Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIM Certification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242)
                    )
                    TextButton(onClick = { showSimInputDialog = true }) {
                        Text(if (driverSimCertifications.isNullOrBlank()) "Input" else "Edit")
                    }
                }
                
                if (driverSimCertifications.isNullOrBlank()) {
                    Text(
                        text = "Belum ada SIM diinput",
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Input SIM Anda untuk dapat menerima order",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp
                    )
                } else {
                    val simText = driverSimCertifications ?: ""
                    Text(
                        text = simText.replace("SIM_", "SIM "),
                        color = Color(0xFF424242),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Menu Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DriverAccountMenuItem(
                    icon = Icons.Filled.Notifications,
                    title = "Request Driver",
                    onClick = onRequestsClick
                )
                HorizontalDivider(color = Color(0xFFE0E0E0))
                DriverAccountMenuItem(
                    icon = Icons.Filled.Person,
                    title = "Edit Profil",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = Color(0xFFE0E0E0))
                DriverAccountMenuItem(
                    icon = Icons.Filled.DirectionsCar,
                    title = "Info Kendaraan",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = Color(0xFFE0E0E0))
                DriverAccountMenuItem(
                    icon = Icons.Filled.Settings,
                    title = "Pengaturan",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = Color(0xFFE0E0E0))
                DriverAccountMenuItem(
                    icon = Icons.Filled.Info,
                    title = "Bantuan",
                    onClick = { /* TODO */ }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53E3E)
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

    // SIM Input Dialog
    if (showSimInputDialog) {
        DriverSimInputDialog(
            currentSimCertifications = driverSimCertifications,
            onDismiss = { showSimInputDialog = false },
            onConfirm = { simTypes ->
                updateSimCertifications(simTypes)
                showSimInputDialog = false
            }
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Apakah Anda yakin ingin logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color(0xFFE53E3E))
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
private fun DriverAccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF424242)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color(0xFF424242)
        )
    }
}

// Summary Card Component
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color(0xFF757575),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
            color = Color(0xFF424242),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DriverOrdersContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Icon(
                Icons.Filled.ShoppingBag,
            contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Pesanan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            Text(
                text = "(Coming Soon)",
                color = Color(0xFF757575),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DriverEarningsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.AttachMoney,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Pendapatan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            Text(
                text = "(Coming Soon)",
                color = Color(0xFF757575),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DriverBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFE0E0E0)))
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    Icons.Filled.Dashboard,
                    contentDescription = null,
                    tint = if (selectedTab == 0) Color(0xFF424242) else Color(0xFF9E9E9E)
                )
            },
            label = {
                Text(
                    "Dashboard",
                    color = if (selectedTab == 0) Color(0xFF424242) else Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = if (selectedTab == 1) Color(0xFF424242) else Color(0xFF9E9E9E)
                )
            },
            label = {
                Text(
                    "Pesanan",
                    color = if (selectedTab == 1) Color(0xFF424242) else Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = if (selectedTab == 2) Color(0xFF424242) else Color(0xFF9E9E9E)
                )
            },
            label = {
                Text(
                    "Pendapatan",
                    color = if (selectedTab == 2) Color(0xFF424242) else Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == 2) FontWeight.Medium else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (selectedTab == 3) Color(0xFF424242) else Color(0xFF9E9E9E)
                )
            },
            label = {
                Text(
                    "Akun",
                    color = if (selectedTab == 3) Color(0xFF424242) else Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == 3) FontWeight.Medium else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
        )
    }
}

/**
 * Compact card untuk menampilkan summary request di dashboard
 */
@Composable
private fun DriverRequestSummaryCard(
    request: DriverRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.passengerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "PENDING",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (request.vehicleType == "MOBIL") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF757575)
                )
                Text(
                    text = "${request.vehicleBrand} ${request.vehicleModel}",
                    fontSize = 11.sp,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF4CAF50)
                )
                Text(
                    text = request.pickupAddress,
                    fontSize = 10.sp,
                    color = Color(0xFF999999),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

