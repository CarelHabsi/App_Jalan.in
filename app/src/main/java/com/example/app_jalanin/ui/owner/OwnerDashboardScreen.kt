package com.example.app_jalanin.ui.owner

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    ownerEmail: String,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: OwnerDashboardViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )

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

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }

    // Show error toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👔 Dashboard Owner") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah Kendaraan") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            // Statistics Panel
            item {
                StatisticsPanel(
                    countTersedia = countTersedia,
                    countSedangDisewa = countSedangDisewa,
                    countTidakTersedia = countTidakTersedia
                )
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
    }

    // Add Vehicle Dialog
    if (showAddDialog) {
        AddVehicleDialog(
            ownerEmail = ownerEmail,
            onDismiss = { showAddDialog = false },
            onConfirm = { vehicle ->
                viewModel.addVehicle(vehicle)
                showAddDialog = false
                Toast.makeText(context, "✅ Kendaraan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "✅ Kendaraan berhasil diubah", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "🗑️ Kendaraan berhasil dihapus", Toast.LENGTH_SHORT).show()
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
    onStatusChange: (VehicleStatus, String?) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

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
            // Header: Name + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    }
                }

                // Status Badge
                StatusBadge(status = vehicle.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Change
                OutlinedButton(
                    onClick = { showStatusMenu = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Status", fontSize = 12.sp)
                }

                // Edit
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                // Delete
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus", fontSize = 12.sp)
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
                // Status options
                VehicleStatus.entries.forEach { status ->
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
                                VehicleStatus.SEDANG_DISEWA -> "🚗 Sedang Disewa"
                                VehicleStatus.TIDAK_TERSEDIA -> "🔧 Tidak Tersedia"
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

