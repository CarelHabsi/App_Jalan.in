package com.example.app_jalanin.ui.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun DriverDashboardScreen(
    username: String? = null,
    role: String? = null,
    onLogout: () -> Unit = {}
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
                0 -> DriverHomeContent(username = username ?: "Driver")
                1 -> DriverOrdersContent()
                2 -> DriverEarningsContent()
                3 -> DriverAccountContent(
                    username = username ?: "Driver",
                    role = role ?: "",
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun DriverHomeContent(username: String) {
    var isOnline by remember { mutableStateOf(true) }

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
                            onCheckedChange = { isOnline = it },
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

            // Order Aktif Card
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
                    Text(
                        text = "Order Aktif",
                        color = Color(0xFF424242),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Route info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF06A870))
                                )
                                Text(
                                    text = "Auditorium UNP",
                                    color = Color(0xFF424242),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .width(2.dp)
                                    .height(16.dp)
                                    .background(Color(0xFFE0E0E0))
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53E3E))
                                )
                                Text(
                                    text = "Basko City Mall",
                                    color = Color(0xFF424242),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tarif Perjalanan",
                                color = Color(0xFF757575),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Rp ---",
                                color = Color(0xFF424242),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF424242)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Lihat Detail Order",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Lihat Pesanan Masuk Button
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06A870)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Lihat Pesanan Masuk",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

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
private fun DriverAccountContent(
    username: String,
    role: String,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

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

        // Menu Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
private fun DriverAccountMenuItem(
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
            tint = Color(0xFF757575),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9E9E9E)
        )
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

