package com.example.app_jalanin.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen: Pemilihan tipe registrasi akun.
 * Grup:
 * 1. Driver (Driver Motor, Driver Mobil, Driver Pengganti)
 * 2. Penyewa Kendaraan (Pemilik Kendaraan Sewa)
 * 3. Penumpang
 */
@Composable
fun AccountRegistrationTypeScreen(
    modifier: Modifier = Modifier,
    onTypeSelected: (RegistrationAccountType) -> Unit = {}
) {
    var selected by remember { mutableStateOf<RegistrationAccountType?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Registrasi Akun",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))

        // DRIVER GROUP
        SectionHeader(title = "Driver")
        RegistrationTypeCard(
            title = "Driver Motor",
            description = "Mengantarkan penumpang dengan sepeda motor",
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            selected = selected == RegistrationAccountType.DriverMotor,
            onClick = { selected = RegistrationAccountType.DriverMotor; onTypeSelected(RegistrationAccountType.DriverMotor) }
        )
        RegistrationTypeCard(
            title = "Driver Mobil",
            description = "Mengantarkan penumpang dengan mobil",
            icon = Icons.Filled.DirectionsCar,
            selected = selected == RegistrationAccountType.DriverMobil,
            onClick = { selected = RegistrationAccountType.DriverMobil; onTypeSelected(RegistrationAccountType.DriverMobil) }
        )
        RegistrationTypeCard(
            title = "Driver Pengganti",
            description = "Supir pengganti untuk kendaraan milik pengguna",
            icon = Icons.Filled.SwapHoriz,
            selected = selected == RegistrationAccountType.DriverPengganti,
            onClick = { selected = RegistrationAccountType.DriverPengganti; onTypeSelected(RegistrationAccountType.DriverPengganti) }
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader(title = "Penyewa Kendaraan")
        RegistrationTypeCard(
            title = "Pemilik Kendaraan",
            description = "Sewakan kendaraan Anda untuk rental",
            icon = Icons.Filled.Home,
            selected = selected == RegistrationAccountType.PemilikKendaraan,
            onClick = { selected = RegistrationAccountType.PemilikKendaraan; onTypeSelected(RegistrationAccountType.PemilikKendaraan) }
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader(title = "Penumpang")
        RegistrationTypeCard(
            title = "Akun Penumpang",
            description = "Gunakan layanan perjalanan sebagai penumpang",
            icon = Icons.Filled.Person,
            selected = selected == RegistrationAccountType.Penumpang,
            onClick = { selected = RegistrationAccountType.Penumpang; onTypeSelected(RegistrationAccountType.Penumpang) }
        )
    }
}

// Data / Enum tipe akun dengan id untuk navigasi
sealed class RegistrationAccountType(val id: Int?) {
    object DriverMotor : RegistrationAccountType(1)
    object DriverMobil : RegistrationAccountType(2)
    object DriverPengganti : RegistrationAccountType(3)
    object PemilikKendaraan : RegistrationAccountType(4)
    object Penumpang : RegistrationAccountType(null) // belum ada form khusus
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    )
}

@Composable
private fun RegistrationTypeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 4.dp else 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFE0E0E0),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.Gray)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Person, // simple check substitute (could use Check icon if added)
                    contentDescription = "selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
