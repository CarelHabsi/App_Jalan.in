package com.example.app_jalanin.ui.owner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_jalanin.data.model.Vehicle
import com.example.app_jalanin.data.model.VehicleStatus
import com.example.app_jalanin.data.model.VehicleType
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.entity.Rental
import com.example.app_jalanin.utils.DurationUtils
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    ownerEmail: String,
    onLogout: () -> Unit = {},
    onRentalHistoryClick: () -> Unit = {},
    onPendingRentalClick: (com.example.app_jalanin.data.local.entity.Rental) -> Unit = {},
    onIncomeHistoryClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onEarlyReturnNotificationClick: (com.example.app_jalanin.data.local.entity.Rental) -> Unit = {} // Navigate to chat with renter
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            OwnerBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> OwnerDashboardContent(
                ownerEmail = ownerEmail,
                onLogout = onLogout,
                onRentalHistoryClick = onRentalHistoryClick,
                onPendingRentalClick = onPendingRentalClick,
                onEarlyReturnNotificationClick = onEarlyReturnNotificationClick,
                modifier = Modifier.padding(paddingValues)
            )
            1 -> OwnerRentalHistoryContent(
                ownerEmail = ownerEmail,
                onBackClick = { selectedTab = 0 },
                onRentalSelected = { rental ->
                    if (rental.status == "PENDING") {
                        onPendingRentalClick(rental)
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
            2 -> OwnerIncomeHistoryContent(
                ownerEmail = ownerEmail,
                onBackClick = { selectedTab = 0 },
                modifier = Modifier.padding(paddingValues)
            )
            3 -> OwnerAccountContent(
                ownerEmail = ownerEmail,
                onLogout = onLogout,
                onBackClick = { selectedTab = 0 },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerDashboardContent(
    ownerEmail: String,
    onLogout: () -> Unit = {},
    onRentalHistoryClick: () -> Unit = {},
    onPendingRentalClick: (com.example.app_jalanin.data.local.entity.Rental) -> Unit = {},
    onEarlyReturnNotificationClick: (com.example.app_jalanin.data.local.entity.Rental) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: OwnerDashboardViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val database = remember { AppDatabase.getDatabase(context) }

    // Set owner email
    LaunchedEffect(ownerEmail) {
        viewModel.setOwnerEmail(ownerEmail)
    }

    // Collect states
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val countTersedia by viewModel.countTersedia.collectAsStateWithLifecycle()
    val countSedangDisewa by viewModel.countSedangDisewa.collectAsStateWithLifecycle()
    val countTidakTersedia by viewModel.countTidakTersedia.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    // Load pending rentals
    val pendingRentalsFlow = remember(ownerEmail) {
        database.rentalDao().getPendingRentalsByOwnerFlow(ownerEmail)
    }
    val pendingRentalsState = pendingRentalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingCount = pendingRentalsState.value.size
    
    // Load active rentals for countdown
    val activeRentalsFlow = remember(ownerEmail) {
        database.rentalDao().getActiveRentalsByOwnerFlow(ownerEmail)
    }
    val activeRentalsState = activeRentalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeRental = activeRentalsState.value.firstOrNull()
    
    // ✅ Load early return requests
    val earlyReturnRequestsFlow = remember(ownerEmail) {
        database.rentalDao().getEarlyReturnRequestsByOwnerFlow(ownerEmail)
    }
    val earlyReturnRequestsState = earlyReturnRequestsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val earlyReturnCount = earlyReturnRequestsState.value.size

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAssignDriverScreen by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }

    // Show error toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    // Show sync status toast
    LaunchedEffect(syncStatus) {
        syncStatus?.let { status ->
            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            viewModel.clearSyncStatus()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("👔 Dashboard Owner") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            item {
                Text(
                    text = "Selamat datang, Owner! 👋",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // ✅ Balance Card
            item {
                val balanceRepository = remember { com.example.app_jalanin.data.local.BalanceRepository(context) }
                
                // ✅ CRITICAL FIX: Download balance from Firestore (READ-ONLY, no recalculation)
                // Firestore balance is the SINGLE SOURCE OF TRUTH
                // DO NOT recalculate balance from transaction history
                LaunchedEffect(ownerEmail) {
                    try {
                        // Initialize balance if not exists (only creates if missing, never resets)
                        balanceRepository.initializeBalance(ownerEmail)
                        
                        // Download balance from Firestore (READ-ONLY operation)
                        // This is the ONLY balance update during login/dashboard open
                        com.example.app_jalanin.data.remote.FirestoreBalanceSyncManager.downloadUserBalance(
                            context,
                            ownerEmail
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("OwnerDashboard", "Error downloading balance: ${e.message}", e)
                    }
                }
                
                com.example.app_jalanin.ui.common.BalanceCard(
                    userEmail = ownerEmail,
                    balanceRepository = balanceRepository,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Statistics Panel
            item {
                StatisticsPanel(
                    countTersedia = countTersedia,
                    countSedangDisewa = countSedangDisewa,
                    countTidakTersedia = countTidakTersedia
                )
            }
            
            // ✅ Active Rental Countdown Card
            if (activeRental != null) {
                item {
                    ActiveRentalCountdownCard(
                        rental = activeRental,
                        onHistoryClick = onRentalHistoryClick
                    )
                }
            }
            
            // ✅ Early Return Requests Card
            if (earlyReturnCount > 0) {
                items(earlyReturnRequestsState.value.take(3)) { rental ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                // ✅ Navigate directly to chat with renter
                                onEarlyReturnNotificationClick(rental)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE91E63))
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
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pengembalian Lebih Awal",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${rental.userEmail.split("@").firstOrNull() ?: "Penumpang"} ingin mengembalikan ${rental.vehicleName}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE91E63)
                                    )
                                    Text(
                                        text = "Klik untuk chat dan tentukan lokasi pengembalian",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE91E63).copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Pending Rentals Card
            if (pendingCount > 0) {
                items(pendingRentalsState.value.take(3)) { rental ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPendingRentalClick(rental) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
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
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = "Request Sewa Pending",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$pendingCount request menunggu konfirmasi",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
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
                        }
                    }
                }
            }
            
            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRentalHistoryClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Riwayat Sewa")
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Kendaraan Anda",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${vehicles.size} kendaraan",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Empty state
            if (vehicles.isEmpty() && !isLoading) {
                item {
                    EmptyStateCard()
                }
            }

            // Vehicle List
            items(vehicles) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    onEdit = {
                        selectedVehicle = vehicle
                        showEditDialog = true
                    },
                    onDelete = {
                        selectedVehicle = vehicle
                        showDeleteDialog = true
                    },
                    onAssignDriver = {
                        selectedVehicle = vehicle
                        showAssignDriverScreen = true
                    },
                    onStatusChange = { newStatus, reason ->
                        viewModel.updateVehicleStatus(vehicle.id, newStatus, reason)
                    }
                )
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // FloatingActionButton
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Tambah Kendaraan") },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
    }

    // Add Vehicle Dialog
    if (showAddDialog) {
        AddVehicleDialog(
            ownerEmail = ownerEmail,
            onDismiss = { showAddDialog = false },
            onConfirm = { vehicle ->
                viewModel.addVehicle(vehicle)
                showAddDialog = false
                // Toast akan ditampilkan dari syncStatus
            }
        )
    }

    // Edit Vehicle Dialog
    if (showEditDialog && selectedVehicle != null) {
        EditVehicleDialog(
            vehicle = selectedVehicle!!,
            onDismiss = {
                showEditDialog = false
                selectedVehicle = null
            },
            onConfirm = { updatedVehicle ->
                viewModel.updateVehicle(updatedVehicle)
                showEditDialog = false
                selectedVehicle = null
                // Toast akan ditampilkan dari syncStatus
            }
        )
    }

    // Assign Driver Screen
    if (showAssignDriverScreen && selectedVehicle != null) {
        AssignDriverScreen(
            vehicle = selectedVehicle!!,
            ownerEmail = ownerEmail,
            onBackClick = {
                showAssignDriverScreen = false
                selectedVehicle = null
            },
            onAssignmentSaved = {
                showAssignDriverScreen = false
                selectedVehicle = null
                // Refresh vehicle list by re-triggering setOwnerEmail
                viewModel.setOwnerEmail(ownerEmail)
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedVehicle != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedVehicle = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Kendaraan?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${selectedVehicle!!.name}? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(selectedVehicle!!)
                        showDeleteDialog = false
                        selectedVehicle = null
                        // Toast akan ditampilkan dari syncStatus
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedVehicle = null
                }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun StatisticsPanel(
    countTersedia: Int,
    countSedangDisewa: Int,
    countTidakTersedia: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Statistik Kendaraan",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tersedia
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "✅",
                    count = countTersedia,
                    label = "Siap Sewa",
                    backgroundColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32)
                )

                // Sedang Disewa
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🚗",
                    count = countSedangDisewa,
                    label = "Disewa",
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1565C0)
                )

                // Tidak Tersedia
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🔧",
                    count = countTidakTersedia,
                    label = "Off",
                    backgroundColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    count: Int,
    label: String,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssignDriver: () -> Unit,
    onStatusChange: (VehicleStatus, String?) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Name + Status + Dropdown Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (vehicle.type == VehicleType.MOBIL) "🚗" else "🏍️",
                        fontSize = 24.sp
                    )
                    Column {
                        Text(
                            text = vehicle.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = vehicle.licensePlate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        // ✅ Driver Assignment Badge
                        if (vehicle.driverId != null && vehicle.driverAssignmentMode != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val badgeText = when (vehicle.driverAssignmentMode) {
                                "DELIVERY_ONLY" -> "Pengantar"
                                "DELIVERY_AND_RENTAL" -> "Pengantar + Driver"
                                else -> null
                            }
                            if (badgeText != null) {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { 
                                        Text(
                                            badgeText,
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = if (vehicle.driverAssignmentMode == "DELIVERY_ONLY") 
                                            Color(0xFF2196F3) else Color(0xFF4CAF50),
                                        labelColor = Color.White
                                    ),
                                    modifier = Modifier.height(22.dp)
                                )
                            }
                        }
                    }
                }

                // Status Badge
                StatusBadge(status = vehicle.status)
                
                // Dropdown Menu Button
                Box {
                    IconButton(
                        onClick = { showDropdownMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        // Status Change (disabled jika sedang disewa)
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = if (vehicle.status == VehicleStatus.SEDANG_DISEWA) 
                                        "Status (Auto)" 
                                    else 
                                        "Ubah Status"
                                )
                            },
                            onClick = {
                                if (vehicle.status != VehicleStatus.SEDANG_DISEWA) {
                                    showStatusMenu = true
                                    showDropdownMenu = false
                                }
                            },
                            enabled = vehicle.status != VehicleStatus.SEDANG_DISEWA,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        
                        // Edit
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEdit()
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        
                        // Assign Driver
                        DropdownMenuItem(
                            text = { Text("Assign Driver") },
                            onClick = {
                                onAssignDriver()
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        
                        // Delete
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Hapus",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDelete()
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Driver Assignment Badge
            if (vehicle.driverId != null && vehicle.driverAssignmentMode != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { 
                            Text(
                                when (vehicle.driverAssignmentMode) {
                                    "DELIVERY_ONLY" -> "Pengantar"
                                    "DELIVERY_AND_RENTAL" -> "Pengantar + Driver"
                                    else -> "Driver"
                                },
                                fontSize = 10.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = when (vehicle.driverAssignmentMode) {
                                "DELIVERY_ONLY" -> Color(0xFFFF9800)
                                "DELIVERY_AND_RENTAL" -> Color(0xFF2196F3)
                                else -> Color(0xFF9E9E9E)
                            },
                            labelColor = Color.White
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Details
            Text(
                text = "${vehicle.brand} ${vehicle.model} ${vehicle.year}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "💰 ${formatRupiah(vehicle.pricePerDay)}/hari",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (vehicle.statusReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ ${vehicle.statusReason}",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

        }
    }

    // Status Change Menu
    if (showStatusMenu) {
        StatusChangeDialog(
            currentStatus = vehicle.status,
            onDismiss = { showStatusMenu = false },
            onConfirm = { newStatus, reason ->
                onStatusChange(newStatus, reason)
                showStatusMenu = false
            }
        )
    }
}

@Composable
private fun StatusBadge(status: VehicleStatus) {
    val (emoji, text, backgroundColor, textColor) = when (status) {
        VehicleStatus.TERSEDIA -> {
            Tuple4("✅", "Siap Sewa", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        }
        VehicleStatus.SEDANG_DISEWA -> {
            Tuple4("🚗", "Disewa", Color(0xFFE3F2FD), Color(0xFF1565C0))
        }
        VehicleStatus.TIDAK_TERSEDIA -> {
            Tuple4("🔧", "Off", Color(0xFFFFF3E0), Color(0xFFE65100))
        }
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 12.sp)
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚗",
                fontSize = 48.sp
            )
            Text(
                text = "Belum Ada Kendaraan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tambahkan kendaraan pertama Anda untuk mulai menyewakan",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Helper data class
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}

// Status Change Dialog (to be implemented)
@Composable
private fun StatusChangeDialog(
    currentStatus: VehicleStatus,
    onDismiss: () -> Unit,
    onConfirm: (VehicleStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Status Kendaraan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Info jika status saat ini adalah SEDANG_DISEWA
                if (currentStatus == VehicleStatus.SEDANG_DISEWA) {
                    Surface(
                        color = Color(0xFFE3F2FD).copy(alpha = 0.5f), // Blue background
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ℹ️ Status 'Sedang Disewa'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status ini otomatis muncul ketika ada penumpang yang sedang menyewa kendaraan. Anda tidak dapat mengubahnya secara manual.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Status options (hanya TERSEDIA dan TIDAK_TERSEDIA - SEDANG_DISEWA tidak bisa dipilih manual)
                VehicleStatus.entries
                    .filter { it != VehicleStatus.SEDANG_DISEWA } // ✅ Filter out SEDANG_DISEWA
                    .forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatus = status }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (status) {
                                    VehicleStatus.TERSEDIA -> "✅ Siap Disewa"
                                    VehicleStatus.TIDAK_TERSEDIA -> "🔧 Tidak Tersedia"
                                    VehicleStatus.SEDANG_DISEWA -> "🚗 Sedang Disewa" // Tidak akan muncul karena sudah di-filter
                                }
                            )
                        }
                    }

                // Reason field (only for TIDAK_TERSEDIA)
                if (selectedStatus == VehicleStatus.TIDAK_TERSEDIA) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Alasan") },
                        placeholder = { Text("Contoh: Sedang maintenance") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedStatus,
                        if (selectedStatus == VehicleStatus.TIDAK_TERSEDIA && reason.isNotBlank()) reason else null
                    )
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Add/Edit Vehicle Dialogs will be in separate files for better organization

/**
 * Active Rental Countdown Card - shows countdown for active rental
 */
@Composable
private fun ActiveRentalCountdownCard(
    rental: Rental,
    onHistoryClick: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(0L) }
    var isOvertime by remember { mutableStateOf(false) }
    
    LaunchedEffect(rental) {
        while (rental.status == "ACTIVE") {
            val now = System.currentTimeMillis()
            val diff = rental.endDate - now
            
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onHistoryClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOvertime) "⚠️ PERINGATAN KETERLAMBATAN" else "🚗 Sewa Aktif",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOvertime) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                )
                Text(
                    text = "Lihat Detail →",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
            
            Text(
                text = rental.vehicleName,
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
                    val totalDuration = rental.endDate - rental.startDate
                    val elapsed = System.currentTimeMillis() - rental.startDate
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
        }
    }
}

@Composable
private fun OwnerRentalHistoryContent(
    ownerEmail: String,
    onBackClick: () -> Unit,
    onRentalSelected: (Rental) -> Unit,
    modifier: Modifier = Modifier
) {
    OwnerRentalHistoryScreen(
        ownerEmail = ownerEmail,
        onBackClick = onBackClick,
        onRentalSelected = onRentalSelected
    )
}

@Composable
private fun OwnerIncomeHistoryContent(
    ownerEmail: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IncomeHistoryScreen(
        recipientEmail = ownerEmail,
        recipientRole = "PEMILIK_KENDARAAN",
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerAccountContent(
    ownerEmail: String,
    onLogout: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    var user by remember { mutableStateOf<com.example.app_jalanin.data.local.entity.User?>(null) }
    
    LaunchedEffect(ownerEmail) {
        user = database.userDao().getUserByEmail(ownerEmail)
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Akun") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = user?.fullName ?: "Owner",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ownerEmail,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
private fun OwnerBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            label = {
                Text(
                    "Dashboard",
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            label = {
                Text(
                    "Riwayat Sewa",
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            label = {
                Text(
                    "Pendapatan",
                    color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 2) FontWeight.Medium else FontWeight.Normal
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            label = {
                Text(
                    "Akun",
                    color = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == 3) FontWeight.Medium else FontWeight.Normal
                )
            }
        )
    }
}

