package com.example.app_jalanin.ui.passenger

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_jalanin.auth.AuthStateManager
import com.example.app_jalanin.data.remote.FirestoreRentalService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RentalHistory(
    val id: String,
    val vehicleName: String,
    val vehicleType: String,
    val startDate: Long,
    val endDate: Long,
    val totalPrice: Int,
    val status: RentalStatus,
    val overtimeFee: Int = 0,
    val isWithDriver: Boolean = false,
    val hasActiveTracking: Boolean = false // ✅ Added for delivery tracking
)

enum class RentalStatus {
    ACTIVE,      // Sedang disewa
    OVERDUE,     // Terlambat
    COMPLETED,   // Selesai
    CANCELLED,   // Dibatalkan
    DELIVERING   // ✅ Sedang diantar (kendaraan belum sampai)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalHistoryScreen(
    onBackClick: () -> Unit,
    onOpenTracking: () -> Unit = {},
    activeTrackingData: TrackingData? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State untuk rental histories dari Firestore
    var rentalHistories by remember { mutableStateOf<List<RentalHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch rental histories dari Room database & Firestore
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // ✅ FIX: Initialize prefs at the beginning
                val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)

                // Get current user
                var currentUser = AuthStateManager.getCurrentUser(context)

                // Fallback: Try to get userId from SharedPreferences if user not found
                var userId: Int? = currentUser?.id

                if (currentUser == null) {
                    android.util.Log.w("RentalHistory", "⚠️ getCurrentUser returned null, trying fallback...")
                    val userIdInt = prefs.getInt("current_user_id", -1)
                    val userEmail = prefs.getString("current_user_email", null)

                    android.util.Log.d("RentalHistory", "📍 Fallback - userId: $userIdInt, email: $userEmail")

                    if (userIdInt != -1) {
                        userId = userIdInt
                        android.util.Log.d("RentalHistory", "✅ Using fallback userId: $userId")
                    }
                }

                // ✅ FIX: Use email instead of userId for lookup (email doesn't change between restarts)
                val userEmail = currentUser?.email ?: prefs.getString("current_user_email", null)

                if (userEmail != null) {
                    android.util.Log.d("RentalHistory", "📥 Loading rentals from Room database for email: $userEmail")

                    // 1. Load from Room database FIRST (instant, offline-capable)
                    try {
                        val db = com.example.app_jalanin.data.AppDatabase.getDatabase(context)
                        android.util.Log.d("RentalHistory", "🔍 Database instance obtained")

                        // ✅ FIX: Query by email instead of userId
                        val roomRentals = db.rentalDao().getRentalsByEmail(userEmail)
                        android.util.Log.d("RentalHistory", "✅ Loaded ${roomRentals.size} rentals from Room")

                        if (roomRentals.isEmpty()) {
                            android.util.Log.w("RentalHistory", "⚠️ No rentals found for email: $userEmail")

                            // Debug: Check all rentals in database
                            val allRentals = db.rentalDao().getAllRentals()
                            android.util.Log.d("RentalHistory", "📊 Total rentals in database: ${allRentals.size}")
                            allRentals.forEach { rental ->
                                android.util.Log.d("RentalHistory", "   - Rental: ${rental.id} for email ${rental.userEmail} (${rental.vehicleName})")
                            }
                        } else {
                            android.util.Log.d("RentalHistory", "📋 Rental details:")
                            roomRentals.forEach { rental ->
                                android.util.Log.d("RentalHistory", "   - ${rental.id}: ${rental.vehicleName} (${rental.status})")
                                android.util.Log.d("RentalHistory", "     Duration: ${rental.durationHours}h ${rental.durationMinutes}m")
                                android.util.Log.d("RentalHistory", "     Start: ${rental.startDate}, End: ${rental.endDate}")
                            }
                        }

                        // Convert Room entities to UI model
                        val roomHistories = roomRentals.map { rental ->
                            RentalHistory(
                                id = rental.id,
                                vehicleName = rental.vehicleName,
                                vehicleType = rental.vehicleType,
                                startDate = rental.startDate,
                                endDate = rental.endDate,
                                totalPrice = rental.totalPrice,
                                status = when (rental.status) {
                                    "ACTIVE" -> if (rental.isOverdue()) RentalStatus.OVERDUE else RentalStatus.ACTIVE
                                    "OVERDUE" -> RentalStatus.OVERDUE
                                    "COMPLETED" -> RentalStatus.COMPLETED
                                    "CANCELLED" -> RentalStatus.CANCELLED
                                    "DELIVERING" -> RentalStatus.DELIVERING
                                    else -> RentalStatus.COMPLETED
                                },
                                overtimeFee = rental.overtimeFee,
                                isWithDriver = rental.isWithDriver,
                                hasActiveTracking = rental.status == "DELIVERING"
                            )
                        }

                        // ✅ FIX: Add active tracking if exists, and filter out DELIVERING from Room to avoid duplication
                        val finalHistories = buildList {
                            if (activeTrackingData != null) {
                                add(
                                    RentalHistory(
                                        id = "TRACKING_ACTIVE",
                                        vehicleName = activeTrackingData.vehicle.name,
                                        vehicleType = activeTrackingData.vehicle.type,
                                        startDate = System.currentTimeMillis(),
                                        endDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000),
                                        totalPrice = activeTrackingData.totalPrice,
                                        status = RentalStatus.DELIVERING,
                                        isWithDriver = activeTrackingData.withDriver,
                                        hasActiveTracking = true
                                    )
                                )
                                // ✅ Only add Room rentals that are NOT DELIVERING (to avoid duplication)
                                addAll(roomHistories.filter { it.status != RentalStatus.DELIVERING })
                                android.util.Log.d("RentalHistory", "✅ Filtered out DELIVERING rentals from Room to avoid duplication")
                            } else {
                                // No active tracking, add all Room rentals
                                addAll(roomHistories)
                            }
                        }

                        rentalHistories = finalHistories
                        android.util.Log.d("RentalHistory", "✅ Loaded ${finalHistories.size} total rentals (including tracking)")

                    } catch (e: Exception) {
                        android.util.Log.e("RentalHistory", "❌ Error loading from Room: ${e.message}", e)
                        e.printStackTrace()
                        errorMessage = "Error loading rentals: ${e.message}"

                        // Still show active tracking if exists
                        if (activeTrackingData != null) {
                            rentalHistories = listOf(
                                RentalHistory(
                                    id = "TRACKING_ACTIVE",
                                    vehicleName = activeTrackingData.vehicle.name,
                                    vehicleType = activeTrackingData.vehicle.type,
                                    startDate = System.currentTimeMillis(),
                                    endDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000),
                                    totalPrice = activeTrackingData.totalPrice,
                                    status = RentalStatus.DELIVERING,
                                    isWithDriver = activeTrackingData.withDriver,
                                    hasActiveTracking = true
                                )
                            )
                        }
                    }

                    // 2. Sync with Firestore in background (optional, untuk backup)
                    launch(Dispatchers.IO) {
                        try {
                            android.util.Log.d("RentalHistory", "🔄 Syncing with Firestore in background...")
                            val result = FirestoreRentalService.getUserRentals(userId.toString())

                            result.onSuccess { firestoreRentals ->
                                android.util.Log.d("RentalHistory", "✅ Fetched ${firestoreRentals.size} rentals from Firestore")

                                // TODO: Merge Firestore data with Room if needed
                                // For now, Room is source of truth
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("RentalHistory", "⚠️ Firestore sync failed (non-critical): ${e.message}")
                        }
                    }
                } else {
                    android.util.Log.e("RentalHistory", "❌ No userId available")


                    // Still show active tracking if exists
                    if (activeTrackingData != null) {
                        rentalHistories = listOf(
                            RentalHistory(
                                id = "TRACKING_ACTIVE",
                                vehicleName = activeTrackingData.vehicle.name,
                                vehicleType = activeTrackingData.vehicle.type,
                                startDate = System.currentTimeMillis(),
                                endDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000),
                                totalPrice = activeTrackingData.totalPrice,
                                status = RentalStatus.DELIVERING,
                                isWithDriver = activeTrackingData.withDriver,
                                hasActiveTracking = true
                            )
                        )
                    } else {
                        errorMessage = "Silakan login untuk melihat riwayat"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Terjadi kesalahan: ${e.message}"
                android.util.Log.e("RentalHistory", "❌ Exception: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Riwayat Penyewaan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2196F3)
                    )
                }
                errorMessage != null -> {
                    // Error message with retry
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 48.sp
                        )
                        Text(
                            text = errorMessage!!,
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    // Retry loading
                                    isLoading = true
                                    errorMessage = null
                                    delay(100)
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                rentalHistories.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 64.sp
                        )
                        Text(
                            text = "Belum Ada Riwayat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "Riwayat penyewaan kendaraan akan muncul di sini",
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Rental list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rentalHistories) { rental ->
                            RentalHistoryCard(
                                rental = rental,
                                onOpenTracking = onOpenTracking
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RentalHistoryCard(
    rental: RentalHistory,
    onOpenTracking: () -> Unit = {}
) {
    // Countdown timer for active rentals
    var remainingTime by remember { mutableStateOf(0L) }
    var isOvertime by remember { mutableStateOf(false) }

    // Calculate remaining time
    LaunchedEffect(Unit) {
        while (rental.status == RentalStatus.ACTIVE) {
            val now = System.currentTimeMillis()
            val diff = rental.endDate - now

            if (diff <= 0) {
                // Overtime!
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
        RentalStatus.ACTIVE -> if (isOvertime) Color(0xFFEF5350) else Color(0xFF4CAF50)
        RentalStatus.OVERDUE -> Color(0xFFEF5350)
        RentalStatus.COMPLETED -> Color(0xFF9E9E9E)
        RentalStatus.CANCELLED -> Color(0xFFFF9800)
        RentalStatus.DELIVERING -> Color(0xFF2196F3) // ✅ Blue for delivering
    }

    val statusText = when (rental.status) {
        RentalStatus.ACTIVE -> if (isOvertime) "⚠️ TERLAMBAT" else "🚗 SEDANG DISEWA"
        RentalStatus.OVERDUE -> "⚠️ TERLAMBAT"
        RentalStatus.COMPLETED -> "✅ SELESAI"
        RentalStatus.CANCELLED -> "❌ DIBATALKAN"
        RentalStatus.DELIVERING -> "🚚 SEDANG DIANTAR" // ✅ Delivering status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Text(
                    text = "ID: ${rental.id}",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }

            // Vehicle Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (rental.vehicleType == "Motor") "🏍️" else "🚗",
                        fontSize = 32.sp
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = rental.vehicleName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = rental.vehicleType + if (rental.isWithDriver) " • Dengan Driver" else "",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Rp ${String.format(Locale.forLanguageTag("id-ID"), "%,d", rental.totalPrice).replace(',', '.')}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // Status-specific content
            when (rental.status) {
                RentalStatus.ACTIVE -> {
                    if (isOvertime) {
                        // Overdue warning
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ PERINGATAN KETERLAMBATAN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )

                                Text(
                                    text = "Anda terlambat: ${formatTime(remainingTime)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF5350)
                                )

                                val overtimeFeePerHour = 50000
                                val overtimeHours = TimeUnit.MILLISECONDS.toHours(remainingTime) + 1
                                val estimatedFee = overtimeFeePerHour * overtimeHours

                                Text(
                                    text = "Biaya tambahan: ~Rp ${String.format(Locale.forLanguageTag("id-ID"), "%,d", estimatedFee).replace(',', '.')}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD32F2F)
                                )

                                Text(
                                    text = "⚡ Rp 50.000/jam untuk keterlambatan",
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )

                                Text(
                                    text = "💡 Harap segera kembalikan kendaraan untuk menghindari biaya tambahan lebih lanjut.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else {
                        // Active rental - countdown
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⏱️ Waktu Sewa Tersisa:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )

                                Text(
                                    text = formatTime(remainingTime),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
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
                                    color = Color(0xFF4CAF50),
                                    trackColor = Color(0xFFC8E6C9)
                                )

                                Text(
                                    text = "⚠️ Keterlambatan dikenakan Rp 50.000/jam",
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )

                                Text(
                                    text = "📍 Perjalanan Anda di-tracking untuk keperluan pengembalian kendaraan",
                                    fontSize = 10.sp,
                                    color = Color(0xFF999999),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
                RentalStatus.DELIVERING -> {
                    // Delivery in progress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚚",
                                    fontSize = 20.sp
                                )
                                Column {
                                    Text(
                                        text = "Kendaraan Sedang Diantar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                    Text(
                                        text = "Durasi sewa akan dimulai saat kendaraan tiba",
                                        fontSize = 11.sp,
                                        color = Color(0xFF666666),
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (rental.hasActiveTracking) {
                                Button(
                                    onClick = onOpenTracking,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2196F3)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "📍",
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Buka Tracking Real-Time",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "💡 Lihat lokasi kendaraan secara real-time di peta",
                                    fontSize = 10.sp,
                                    color = Color(0xFF999999),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
                RentalStatus.COMPLETED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✅ Penyewaan kendaraan telah selesai",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
                            )

                            if (rental.overtimeFee > 0) {
                                Text(
                                    text = "Biaya keterlambatan: Rp ${String.format(Locale.forLanguageTag("id-ID"), "%,d", rental.overtimeFee).replace(',', '.')}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF5350)
                                )
                            }

                            Text(
                                text = "Terima kasih telah menggunakan layanan kami! 🙏",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(milliseconds)
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/**
 * Format timestamp menjadi tanggal yang mudah dibaca
 * Format: "7 Des 2025, 14:30"
 */
fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
    return dateFormat.format(java.util.Date(timestamp))
}

/**
 * Format timestamp menjadi waktu saja
 * Format: "14:30"
 */
fun formatTimeOnly(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    return timeFormat.format(java.util.Date(timestamp))
}

/**
 * Format timestamp menjadi tanggal saja
 * Format: "7 Desember 2025"
 */
fun formatDateOnly(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
    return dateFormat.format(java.util.Date(timestamp))
}

