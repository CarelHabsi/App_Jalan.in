# Dokumentasi Room Database - App JalanIn

## ✅ Yang Sudah Dibuat

### 1. **Entity (Tabel Database)**
File: `data/local/entity/User.kt`
- Tabel `users` dengan kolom: id, username, password, role, fullName, phoneNumber, email, createdAt
- Role yang tersedia: "penumpang", "driver_motor", "driver_mobil", "driver_pengganti", "pemilik_kendaraan"

### 2. **DAO (Data Access Object)**
File: `data/local/dao/UserDao.kt`
- `insertUser()` - Tambah user baru
- `login()` - Login dengan validasi role
- `getUserByUsername()` - Cari user by username
- `getUsersByRole()` - Ambil semua user dengan role tertentu
- `getAllUsers()` - Ambil semua user
- `deleteUser()` - Hapus user
- `updatePassword()` - Update password user

### 3. **Database**
File: `data/AppDatabase.kt`
- Singleton pattern untuk Room Database
- Database name: `jalanin_database`
- Version: 1
- Fallback to destructive migration (untuk development)

### 4. **Repository**
File: `data/local/UserRepository.kt`
- `registerUser()` - Daftar user baru (cek duplikat username)
- `login()` - Login dengan validasi role
- `getUserByUsername()` - Get user info
- `getAllUsers()` - List semua user
- `getUsersByRole()` - Filter by role
- `deleteUser()` - Hapus user
- `updatePassword()` - Ganti password

### 5. **ViewModel**
File: `ui/login/DriverLoginViewModel.kt`
- State management untuk login
- LoginState: Idle, Loading, Success, Error

### 6. **UI Login**
File: `ui/login/DriverLoginScreen.kt`
- UI login dengan pilihan role (Driver, Penumpang, Pemilik Kendaraan)
- Integrasi dengan ViewModel
- Toast notification untuk success/error

### 7. **MainActivity**
File: `MainActivity.kt`
- Auto-create dummy user sekali saja
- Username: `user123`
- Password: `jalanin_aja_dulu`
- Role: `penumpang`
- Menggunakan SharedPreferences untuk flag `dummy_user_created`

---

## 🧪 Cara Testing

### 1. **Build Project**
Buka terminal di Android Studio atau PowerShell:
```bash
cd C:\Users\LENOVO\AndroidStudioProjects\App_JalanIn
.\gradlew.bat assembleDebug
```

### 2. **Run di Emulator**
- Jalankan emulator Android
- Klik tombol Run (▶️) di Android Studio
- Atau via terminal:
```bash
.\gradlew.bat installDebug
```

### 3. **Test Login**
Login dengan kredensial:
- **Username**: `user123`
- **Password**: `jalanin_aja_dulu`
- **Role**: Pilih **Penumpang**

**Expected Result:**
- ✅ Toast muncul: "Berhasil login: user123 (Penumpang)"
- ✅ Navigasi ke Dashboard

**Jika salah role:**
- ❌ Toast muncul: "Silahkan masukkan kombinasi role, username, dan password yang tepat"

---

## 📋 Cara Menambah User Baru

### Opsi 1: Via Kode (Testing)
Edit `MainActivity.kt`, tambahkan di `onCreate()`:

```kotlin
lifecycleScope.launch {
    val db = AppDatabase.getDatabase(this@MainActivity)
    val repo = UserRepository(db.userDao())
    
    repo.registerUser(
        username = "driver01",
        password = "password123",
        role = "driver_motor",
        fullName = "Driver Motor Test",
        phoneNumber = "082123456789"
    )
}
```

### Opsi 2: Via Fitur Registrasi
Klik "Belum punya akun? Daftar sekarang" di layar login (sudah ada flow registrasi)

---

## 🗂️ Struktur Database

### Tabel: `users`
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key, auto-increment |
| username | TEXT | Username unik |
| password | TEXT | Password (plaintext, TODO: hash) |
| role | TEXT | Role user |
| fullName | TEXT | Nama lengkap (optional) |
| phoneNumber | TEXT | Nomor HP (optional) |
| email | TEXT | Email (optional) |
| createdAt | INTEGER | Timestamp created |

---

## 🔒 Security Notes (untuk Production)

⚠️ **PENTING**: Kode ini untuk learning/development. Untuk production:

1. **Hash Password**
   - Jangan simpan password plain text
   - Gunakan BCrypt atau library hash lainnya
   
2. **Validation**
   - Validasi input (username format, password strength)
   - Sanitize input untuk prevent SQL injection (Room sudah handle ini)

3. **Security Rules**
   - Implement proper authentication token
   - Secure session management

---

## 🔄 Cara Sync ke Cloud (Future)

Struktur sudah siap untuk sync:

```kotlin
// Contoh flow sync
suspend fun syncToCloud() {
    val localUsers = repository.getAllUsers()
    
    localUsers.forEach { user ->
        // Upload ke server
        api.uploadUser(user)
    }
}
```

Nanti tinggal tambahkan:
- Retrofit/Ktor untuk HTTP client
- API endpoint untuk sync
- Background worker (WorkManager) untuk auto-sync

---

## 📱 Fitur yang Tersedia

### ✅ Sudah Implementasi:
- [x] Login dengan validasi role
- [x] Persistent local database (Room)
- [x] Auto-create dummy user
- [x] State management (ViewModel)
- [x] Toast notification
- [x] Navigation

### 🚧 TODO (untuk Registrasi):
- [ ] Integrasikan form registrasi dengan Room Database
- [ ] Hash password sebelum simpan
- [ ] Validasi input (username min length, password strength)
- [ ] Upload foto profile
- [ ] Email verification (optional)

---

## 🐛 Troubleshooting

### Database tidak terbuat?
Cek Logcat untuk log:
```
✅ Dummy user created successfully
```
atau
```
ℹ️ Dummy user already exists
```

### Login gagal terus?
1. Cek kombinasi username, password, dan role
2. Pastikan dummy user sudah dibuat (cek Logcat)
3. Cek database via Device File Explorer:
   `/data/data/com.example.app_jalanin/databases/jalanin_database`

### Clear database untuk testing ulang:
```kotlin
// Di MainActivity.onCreate(), tambah ini:
val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
sharedPref.edit().putBoolean("dummy_user_created", false).apply()
// Lalu uninstall app dan install lagi
```

Atau uninstall app dari device/emulator, lalu run lagi.

---

## 📞 Support

Jika ada error:
1. Cek Logcat untuk error message
2. Clean & Rebuild project
3. Invalidate Caches & Restart Android Studio

---

**Database sudah siap digunakan! 🎉**

Data akan persistent (tidak hilang) walau app restart atau device restart.
Nanti kalau mau sync ke cloud, struktur sudah siap tinggal tambah API call.

