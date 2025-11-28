package com.example.app_jalanin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_jalanin.data.auth.UserRole

@Composable
fun DashboardScreen(
    onServiceClick: (String) -> Unit = {},
    onEmergencyClick: () -> Unit = {},
    username: String? = null,
    role: String? = null
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StatusBar()
        Header(username = username ?: "User", role = role ?: "")
        MainContent(onServiceClick)
        EmergencySection(onEmergencyClick)
        BottomNavigationBar()
        HomeIndicator()
    }
}

// Status Bar
@Composable
private fun StatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(12.dp).background(Color.Gray, RoundedCornerShape(2.dp)))
            Box(Modifier.size(12.dp).background(Color.Gray, RoundedCornerShape(2.dp)))
            Box(Modifier.size(12.dp).background(Color.Gray, RoundedCornerShape(2.dp)))
        }
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
private fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }, label = { Text("Riwayat") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }, label = { Text("Pembayaran") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }, label = { Text("Akun") })
    }
}

// Home Indicator
@Composable
private fun HomeIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).padding(6.dp).background(Color(0xFFEAEAEA), RoundedCornerShape(3.dp)))
}
