package com.example.app_jalanin.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OwnerDashboardScreen(
    username: String? = null,
    role: String? = null,
    onLogout: () -> Unit = {}
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> OwnerHomeContent(username = username ?: "Owner")
                1 -> OwnerFleetContent()
                2 -> OwnerReportsContent()
                3 -> OwnerAccountContent(
                    username = username ?: "Owner",
                    role = role ?: "",
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun OwnerHomeContent(username: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RectangleShape)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dashboard Rental",
                color = Color(0xFF1F2937),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stats Cards Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Kendaraan Aktif & Permintaan Sewa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardCompact(
                        title = "Kendaraan Aktif",
                        value = "3",
                        subtitle = "sedang disewa",
                        valueColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    StatCardCompact(
                        title = "Permintaan Sewa",
                        value = "5",
                        subtitle = "menunggu",
                        valueColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Pendapatan Bulanan (Full width)
                StatCardFull(
                    title = "Pendapatan Bulanan",
                    value = "Rp 2.400.000",
                    subtitle = "November 2024",
                    valueColor = Color(0xFF10B981)
                )
            }

            // Vehicle List Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Daftar Kendaraan Anda",
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VehicleItem(
                        name = "Toyota Avanza 2022",
                        status = "Sedang Disewa",
                        statusColor = Color(0xFFFEE2E2),
                        statusTextColor = Color(0xFF991B1B)
                    )
                    VehicleItem(
                        name = "Honda Beat 2021",
                        status = "Tersedia",
                        statusColor = Color(0xFFDCFCE7),
                        statusTextColor = Color(0xFF166534)
                    )
                    VehicleItem(
                        name = "Yamaha NMAX 2023",
                        status = "Menunggu Pengembalian",
                        statusColor = Color(0xFFFEF3C7),
                        statusTextColor = Color(0xFF92400E)
                    )
                }
            }

            // Add Vehicle Button
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Tambah Kendaraan",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatCardCompact(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatCardFull(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VehicleItem(
    name: String,
    status: String,
    statusColor: Color,
    statusTextColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Vehicle Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = name,
                    color = Color(0xFF1F2937),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = status,
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Detail Button
            Surface(
                modifier = Modifier.size(width = 60.dp, height = 32.dp),
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(6.dp),
                onClick = { /* TODO */ }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Detail",
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerFleetContent() {
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
                Icons.Filled.History,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Riwayat Sewa",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "(Coming Soon)",
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun OwnerReportsContent() {
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
                Icons.Filled.Message,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Pesan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "(Coming Soon)",
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun OwnerAccountContent(
    username: String,
    role: String,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                        Icons.Filled.Business,
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
                        text = "Pemilik Kendaraan",
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
                OwnerAccountMenuItem(
                    icon = Icons.Filled.Person,
                    title = "Edit Profil",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider()
                OwnerAccountMenuItem(
                    icon = Icons.Filled.Business,
                    title = "Info Perusahaan",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider()
                OwnerAccountMenuItem(
                    icon = Icons.Filled.Settings,
                    title = "Pengaturan",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider()
                OwnerAccountMenuItem(
                    icon = Icons.Filled.Info,
                    title = "Bantuan",
                    onClick = { /* TODO */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
private fun OwnerAccountMenuItem(
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
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OwnerBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RectangleShape)
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = if (selectedTab == 0) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                )
            },
            label = {
                Text(
                    "Home",
                    color = if (selectedTab == 0) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = if (selectedTab == 1) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                )
            },
            label = {
                Text(
                    "Riwayat Sewa",
                    color = if (selectedTab == 1) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    Icons.Filled.Message,
                    contentDescription = null,
                    tint = if (selectedTab == 2) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                )
            },
            label = {
                Text(
                    "Pesan",
                    color = if (selectedTab == 2) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
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
                    tint = if (selectedTab == 3) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                )
            },
            label = {
                Text(
                    "Akun",
                    color = if (selectedTab == 3) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color.Transparent
            )
        )
    }
}

