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

@Composable
fun PassengerDashboardScreen(
    onServiceClick: (String) -> Unit = {},
    onEmergencyClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    username: String? = null,
    role: String? = null
) {
    var selectedTab by remember { mutableStateOf(0) }

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
                    onEmergencyClick = onEmergencyClick
                )
                1 -> HistoryContent(
                    onHistoryClick = onHistoryClick
                )
                2 -> PaymentContent()
                3 -> AccountContent(
                    username = username ?: "User",
                    role = role ?: "",
                    onLogout = onLogout,
                    onDeleteAccount = onDeleteAccount // Pass parameter
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
    onEmergencyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Header(username = username, role = role)
        MainContent(onServiceClick)
        EmergencySection(onEmergencyClick)
    }
}

@Composable
private fun HistoryContent(onHistoryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📜 Riwayat Penyewaan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )

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
            Text(
                text = "Lihat Riwayat Penyewaan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Lihat status penyewaan kendaraan Anda\ndan countdown durasi sewa",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun PaymentContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Halaman Pembayaran\n(Coming Soon)", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun AccountContent(
    username: String,
    role: String,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit = {} // Add parameter
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
            ServiceCard("Ojek Motor", "Cepat & hemat", icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }) { onServiceClick("ojek_motor") }
            ServiceCard("Ojek Mobil", "Nyaman & aman", icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }) { onServiceClick("ojek_mobil") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ServiceCard("Cari Driver", "Supir pengganti", icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }) { onServiceClick("cari_driver") }
            ServiceCard("Sewa Kendaraan", "Harian/mingguan", icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }) { onServiceClick("sewa_kendaraan") }
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

