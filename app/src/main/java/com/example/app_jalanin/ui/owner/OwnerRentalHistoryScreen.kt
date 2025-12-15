package com.example.app_jalanin.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.entity.Rental
import com.example.app_jalanin.utils.DurationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen untuk owner melihat riwayat sewa kendaraan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRentalHistoryScreen(
    ownerEmail: String,
    onBackClick: () -> Unit = {},
    onRentalSelected: (Rental) -> Unit = {},
    onChatClick: (String) -> Unit = {} // channelId
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    // Load rentals
    val rentalsFlow = remember(ownerEmail) {
        android.util.Log.d("OwnerRentalHistory", "🔍 Loading rentals for owner: $ownerEmail")
        database.rentalDao().getRentalsByOwnerFlow(ownerEmail)
    }
    val rentalsState = rentalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Debug logging
    LaunchedEffect(rentalsState.value) {
        android.util.Log.d("OwnerRentalHistory", "📊 Found ${rentalsState.value.size} rentals for owner: $ownerEmail")
        rentalsState.value.forEachIndexed { index, rental ->
            android.util.Log.d("OwnerRentalHistory", "   ${index + 1}. Rental ID: ${rental.id}, ownerEmail: ${rental.ownerEmail}, status: ${rental.status}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Sewa Kendaraan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (rentalsState.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Belum Ada Riwayat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Riwayat sewa kendaraan akan muncul di sini",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rentalsState.value) { rental ->
                    OwnerRentalHistoryCard(
                        rental = rental,
                        onClick = { onRentalSelected(rental) },
                        onChatClick = { 
                            // Get or create chat channel with passenger
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    val channel = com.example.app_jalanin.utils.ChatHelper.getOrCreateDMChannel(
                                        database,
                                        ownerEmail,
                                        rental.userEmail
                                    )
                                    onChatClick(channel.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("OwnerRentalHistory", "Error creating chat channel: ${e.message}", e)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerRentalHistoryCard(
    rental: Rental,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    
    // Username for passenger (renter)
    var passengerUsername by remember { mutableStateOf<String?>(null) }
    
    // Load passenger username from database
    LaunchedEffect(rental.userEmail) {
        try {
            val user = withContext(Dispatchers.IO) {
                database.userDao().getUserByEmail(rental.userEmail)
            }
            passengerUsername = user?.fullName ?: rental.userEmail.split("@").firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("OwnerRentalHistory", "Error loading passenger username: ${e.message}")
            passengerUsername = rental.userEmail.split("@").firstOrNull()
        }
    }
    
    // Countdown timer for active rental
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
    
    val statusColor = when (rental.status) {
        "PENDING" -> Color(0xFFFF9800)
        "OWNER_DELIVERING" -> Color(0xFF2196F3)
        "DRIVER_CONFIRMED" -> Color(0xFF2196F3)
        "DRIVER_TO_OWNER" -> Color(0xFF9C27B0)
        "DRIVER_PICKUP" -> Color(0xFF9C27B0)
        "DRIVER_TO_PASSENGER" -> Color(0xFF00BCD4)
        "ARRIVED" -> Color(0xFF4CAF50)
        "ACTIVE" -> Color(0xFF4CAF50)
        "DRIVER_TRAVELING" -> Color(0xFF00BCD4)
        "COMPLETED" -> Color(0xFF9E9E9E)
        "CANCELLED" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    
    val statusText = when (rental.status) {
        "PENDING" -> "Menunggu Konfirmasi"
        "OWNER_DELIVERING" -> "Owner Mengantar"
        "DRIVER_CONFIRMED" -> "Driver Dikonfirmasi"
        "DRIVER_TO_OWNER" -> "Driver Menuju Owner"
        "DRIVER_PICKUP" -> "Driver Menjemput"
        "DRIVER_TO_PASSENGER" -> "Dalam Perjalanan"
        "ARRIVED" -> "Tiba di Lokasi"
        "ACTIVE" -> "Sedang Disewa"
        "DRIVER_TRAVELING" -> "Driver Mengantar Penumpang"
        "COMPLETED" -> "Selesai"
        "CANCELLED" -> "Dibatalkan"
        else -> rental.status
    }
    
    val deliveryModeText = when (rental.deliveryMode) {
        "OWNER_DELIVERY" -> "Owner"
        "DRIVER_DELIVERY_ONLY" -> "Driver (Pengantaran)"
        "DRIVER_DELIVERY_TRAVEL" -> "Driver (Pengantaran + Travel)"
        else -> "-"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rental.vehicleName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(rental.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
            
            HorizontalDivider()
            
            // ✅ Early Return Request Badge
            if (rental.earlyReturnRequested && (rental.earlyReturnStatus == "REQUESTED" || rental.earlyReturnStatus == "IN_PROGRESS")) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE91E63))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Pengembalian Lebih Awal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63)
                                )
                                Text(
                                    text = when (rental.earlyReturnStatus) {
                                        "REQUESTED" -> "Penumpang ingin mengembalikan kendaraan"
                                        "IN_PROGRESS" -> "Sedang dalam proses pengembalian"
                                        else -> "Status: ${rental.earlyReturnStatus}"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFFE91E63)
                                )
                                if (rental.returnAddress != null) {
                                    Text(
                                        text = "📍 ${rental.returnAddress}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE91E63).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // ✅ Countdown for ACTIVE rental
            if (rental.status == "ACTIVE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOvertime) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isOvertime) "⚠️ PERINGATAN KETERLAMBATAN" else "⏱️ Waktu Sewa Tersisa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOvertime) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                        
                        Text(
                            text = DurationUtils.formatTime(remainingTime),
                            fontSize = 20.sp,
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
                                .height(6.dp),
                            color = if (isOvertime) Color(0xFFEF5350) else Color(0xFF4CAF50),
                            trackColor = Color(0xFFC8E6C9)
                        )
                        
                        if (isOvertime) {
                            Text(
                                text = "⚠️ Keterlambatan dikenakan Rp 50.000/jam",
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Details
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Penumpang: ${passengerUsername ?: rental.userEmail}",
                    fontSize = 14.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF2196F3)
                )
                Text(
                    text = "Pengantar: $deliveryModeText",
                    fontSize = 14.sp
                )
            }
            
            if (rental.deliveryDriverId != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Driver: ${rental.deliveryDriverId}",
                        fontSize = 14.sp
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: Rp ${String.format(Locale("id", "ID"), "%,d", rental.totalPrice).replace(',', '.')}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onChatClick) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
