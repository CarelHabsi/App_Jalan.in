package com.example.app_jalanin.ui.passenger

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.auth.UserRole
import com.example.app_jalanin.data.local.entity.User
import com.example.app_jalanin.data.model.DriverRoleHelper
import com.example.app_jalanin.data.model.DriverRentalPricing
import com.example.app_jalanin.data.model.VehicleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen for passenger to rent a driver independently
 * ✅ NEW FEATURE: Independent driver rental (not tied to vehicle rental)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentDriverScreen(
    passengerEmail: String,
    onBackClick: () -> Unit = {},
    onDriverSelected: (
        driverEmail: String,
        driverName: String?,
        vehicleType: String,
        durationType: String,
        durationCount: Int,
        price: Long,
        pickupAddress: String,
        pickupLat: Double,
        pickupLon: Double,
        destinationAddress: String?,
        destinationLat: Double?,
        destinationLon: Double?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    // Vehicle type selection
    var selectedVehicleType by remember { mutableStateOf<String?>(null) } // "MOBIL" or "MOTOR"
    
    // Driver selection
    var selectedDriver by remember { mutableStateOf<User?>(null) }
    var availableDrivers by remember { mutableStateOf<List<User>>(emptyList()) }
    var driverProfilesMap by remember { mutableStateOf<Map<String, com.example.app_jalanin.data.local.entity.DriverProfile>>(emptyMap()) }
    var isLoadingDrivers by remember { mutableStateOf(false) }
    
    // Duration selection
    var durationType by remember { mutableStateOf<String?>(null) } // "PER_HOUR", "PER_DAY", "PER_WEEK"
    var durationCount by remember { mutableStateOf("1") }
    var durationCountInt by remember { mutableStateOf(1) }
    
    // Location
    var pickupAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    var pickupLat by remember { mutableStateOf(0.0) }
    var pickupLon by remember { mutableStateOf(0.0) }
    var destinationLat by remember { mutableStateOf<Double?>(null) }
    var destinationLon by remember { mutableStateOf<Double?>(null) }
    
    // Calculate price
    val calculatedPrice = remember(selectedVehicleType, durationType, durationCountInt) {
        if (selectedVehicleType != null && durationType != null && durationCountInt > 0) {
            DriverRentalPricing.calculatePrice(
                selectedVehicleType!!,
                durationType!!,
                durationCountInt
            )
        } else {
            0L
        }
    }
    
    // Format price
    val formattedPrice = remember(calculatedPrice) {
        if (calculatedPrice > 0) {
            NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(calculatedPrice)
        } else {
            "Rp 0"
        }
    }
    
    // Load drivers when vehicle type is selected
    LaunchedEffect(selectedVehicleType) {
        val vehicleType = selectedVehicleType
        if (vehicleType == null) {
            availableDrivers = emptyList()
            selectedDriver = null
            return@LaunchedEffect
        }
        
        scope.launch {
            try {
                isLoadingDrivers = true
                val vehicleTypeEnum = when (vehicleType.uppercase()) {
                    "MOBIL" -> VehicleType.MOBIL
                    "MOTOR" -> VehicleType.MOTOR
                    else -> null
                }
                
                if (vehicleTypeEnum == null) {
                    availableDrivers = emptyList()
                    isLoadingDrivers = false
                    return@launch
                }
                
                val allDrivers = withContext(Dispatchers.IO) {
                    database.userDao().getUsersByRole(UserRole.DRIVER.name)
                }
                
                val profiles = withContext(Dispatchers.IO) {
                    database.driverProfileDao().getAll()
                }
                val profilesMap = profiles.associateBy { it.driverEmail }
                driverProfilesMap = profilesMap
                
                // Filter: Only online drivers with matching SIM
                availableDrivers = DriverRoleHelper.filterAvailableDrivers(
                    allDrivers,
                    vehicleTypeEnum,
                    profilesMap
                )
                
                android.util.Log.d("RentDriverScreen", "✅ Loaded ${availableDrivers.size} available drivers for $vehicleType")
            } catch (e: Exception) {
                android.util.Log.e("RentDriverScreen", "Error loading drivers: ${e.message}", e)
                Toast.makeText(context, "Error loading drivers: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingDrivers = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚕 Sewa Driver") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Type Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Pilih Tipe Kendaraan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // MOBIL option
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedVehicleType = "MOBIL" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedVehicleType == "MOBIL")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (selectedVehicleType == "MOBIL")
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🚗", fontSize = 32.sp)
                                    Text(
                                        text = "Mobil",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            // MOTOR option
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedVehicleType = "MOTOR" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedVehicleType == "MOTOR")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (selectedVehicleType == "MOTOR")
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🏍️", fontSize = 32.sp)
                                    Text(
                                        text = "Motor",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Driver Selection (only if vehicle type selected)
            if (selectedVehicleType != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Pilih Driver",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (isLoadingDrivers) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (availableDrivers.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PersonOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Tidak Ada Driver Tersedia",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Tidak ada driver online dengan SIM yang sesuai untuk $selectedVehicleType",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    availableDrivers.forEach { driver ->
                                        val driverProfile = driverProfilesMap[driver.email]
                                        DriverSelectionCard(
                                            driver = driver,
                                            driverProfile = driverProfile,
                                            isSelected = driver.email == selectedDriver?.email,
                                            onClick = { selectedDriver = driver }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Duration Selection (only if driver selected)
            if (selectedDriver != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Durasi Sewa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Duration type selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // PER_HOUR
                                FilterChip(
                                    selected = durationType == "PER_HOUR",
                                    onClick = { durationType = "PER_HOUR" },
                                    label = { Text("Per Jam") },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // PER_DAY
                                FilterChip(
                                    selected = durationType == "PER_DAY",
                                    onClick = { durationType = "PER_DAY" },
                                    label = { Text("Per Hari") },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // PER_WEEK
                                FilterChip(
                                    selected = durationType == "PER_WEEK",
                                    onClick = { durationType = "PER_WEEK" },
                                    label = { Text("Per Minggu") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // Duration count input
                            if (durationType != null) {
                                OutlinedTextField(
                                    value = durationCount,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            durationCount = newValue
                                            durationCountInt = newValue.toIntOrNull() ?: 1
                                        }
                                    },
                                    label = { 
                                        Text(
                                            when (durationType) {
                                                "PER_HOUR" -> "Jumlah Jam"
                                                "PER_DAY" -> "Jumlah Hari"
                                                "PER_WEEK" -> "Jumlah Minggu"
                                                else -> "Jumlah"
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                                
                                // Price preview
                                if (calculatedPrice > 0) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Total Harga",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = formattedPrice,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Location Input (only if duration selected)
            if (durationType != null && durationCountInt > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Lokasi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            OutlinedTextField(
                                value = pickupAddress,
                                onValueChange = { pickupAddress = it },
                                label = { Text("Alamat Penjemputan") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                },
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = destinationAddress,
                                onValueChange = { destinationAddress = it },
                                label = { Text("Alamat Tujuan (Opsional)") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null)
                                },
                                singleLine = true
                            )
                            
                            Text(
                                text = "💡 Lokasi akan digunakan untuk navigasi driver",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Continue Button
            item {
                Button(
                    onClick = {
                        // Validation
                        if (selectedVehicleType == null) {
                            Toast.makeText(context, "Pilih tipe kendaraan terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedDriver == null) {
                            Toast.makeText(context, "Pilih driver terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (durationType == null) {
                            Toast.makeText(context, "Pilih durasi sewa terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (durationCountInt <= 0) {
                            Toast.makeText(context, "Masukkan jumlah durasi yang valid", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (pickupAddress.isBlank()) {
                            Toast.makeText(context, "Masukkan alamat penjemputan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (calculatedPrice <= 0) {
                            Toast.makeText(context, "Harga tidak valid", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Validate driver is still online
                        val driverProfile = driverProfilesMap[selectedDriver!!.email]
                        if (driverProfile == null || !driverProfile.isOnline) {
                            Toast.makeText(context, "Driver yang dipilih sedang offline. Pilih driver lain.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        // Call callback
                        onDriverSelected(
                            selectedDriver!!.email,
                            selectedDriver!!.fullName,
                            selectedVehicleType!!,
                            durationType!!,
                            durationCountInt,
                            calculatedPrice,
                            pickupAddress,
                            pickupLat,
                            pickupLon,
                            if (destinationAddress.isNotBlank()) destinationAddress else null,
                            destinationLat,
                            destinationLon
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedVehicleType != null &&
                             selectedDriver != null &&
                             durationType != null &&
                             durationCountInt > 0 &&
                             pickupAddress.isNotBlank() &&
                             calculatedPrice > 0
                ) {
                    Text("Lanjut ke Konfirmasi")
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DriverSelectionCard(
    driver: User,
    driverProfile: com.example.app_jalanin.data.local.entity.DriverProfile?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val simTypes = driverProfile?.simCertifications?.split(",")?.mapNotNull {
        try { com.example.app_jalanin.data.model.SimType.valueOf(it.trim()) } catch (e: Exception) { null }
    } ?: emptyList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Online status indicator
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape),
                color = Color(0xFF4CAF50)
            ) {}
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver.fullName ?: driver.email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = driver.email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // SIM badges
                if (simTypes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        simTypes.forEach { simType ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = simType.name.replace("SIM_", "SIM "),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

