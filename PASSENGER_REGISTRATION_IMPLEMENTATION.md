# Implementasi Registrasi Penumpang - App JalanIn

## ✅ Yang Sudah Diimplementasikan

### **1. File Baru: PassengerRegistrationFormScreen.kt**
Lokasi: `app/src/main/java/com/example/app_jalanin/ui/register/PassengerRegistrationFormScreen.kt`

**Fitur:**
- ✅ Form registrasi lengkap untuk penumpang
- ✅ Field: Username, Password, Konfirmasi Password, Nama Lengkap, Nomor Telepon
- ✅ Validasi input (min length, required fields, password match)
- ✅ Loading state saat submit
- ✅ Error handling dengan pesan yang jelas
- ✅ Toast notification setelah registrasi berhasil
- ✅ Integrasi dengan database Room

### **2. Update: RegistrationFormViewModel.kt**
**Perubahan:**
- ✅ Ubah dari `ViewModel` → `AndroidViewModel` untuk akses Application context
- ✅ Tambahkan dependency `UserRepository` untuk akses database
- ✅ Tambahkan fungsi `registerUser()` untuk menyimpan data ke database

**Fungsi Baru:**
```kotlin
fun registerUser(
    username: String,
    password: String,
    role: String,
    fullName: String,
    phoneNumber: String,
    email: String? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)
```

### **3. Update: MainActivity.kt**
**Perubahan:**
- ✅ Tambahkan import `PassengerRegistrationFormScreen`
- ✅ Update route `register_type` untuk navigate ke form penumpang
- ✅ Tambahkan route baru: `register/passenger`

**Route Baru:**
```kotlin
composable("register/passenger") {
    val formVm: RegistrationFormViewModel = viewModel()
    PassengerRegistrationFormScreen(
        viewModel = formVm,
        onBack = { navController.popBackStack() },
        onSubmit = { 
            navController.popBackStack(route = "login", inclusive = false) 
        }
    )
}
```

---

## 📱 Flow Registrasi Penumpang

```
Login Screen
    ↓ [klik "Daftar sekarang"]
Account Registration Type Screen
    ↓ [pilih "Penumpang"]
Passenger Registration Form
    ↓ [isi form & submit]
Database Room (tersimpan)
    ↓ [redirect]
Login Screen (bisa login dengan akun baru)
```

---

## 🧪 Cara Testing

### **1. Run Aplikasi**
```bash
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

### **2. Test Flow Registrasi**
1. **Buka aplikasi** → muncul Login Screen
2. **Klik "Belum punya akun? Daftar sekarang"**
3. **Pilih "Penumpang"** dari list tipe akun
4. **Isi form registrasi:**
   - Username: `passenger01` (min 3 karakter)
   - Password: `password123` (min 6 karakter)
   - Konfirmasi Password: `password123` (harus sama)
   - Nama Lengkap: `Penumpang Test`
   - Nomor Telepon: `081234567890` (min 10 digit)
5. **Klik "Daftar"**
6. **Toast muncul:** "Registrasi berhasil! Silahkan login dengan akun baru."
7. **Redirect ke Login Screen**
8. **Login dengan:**
   - Username: `passenger01`
   - Password: `password123`
   - Role: Pilih **Penumpang**
9. **Hasil:** Login berhasil → masuk Dashboard

---

## ✅ Validasi Form

| Field | Validasi |
|-------|----------|
| Username | ≥ 3 karakter, tidak boleh kosong |
| Password | ≥ 6 karakter, tidak boleh kosong |
| Konfirmasi Password | Harus sama dengan password |
| Nama Lengkap | Tidak boleh kosong |
| Nomor Telepon | ≥ 10 digit, tidak boleh kosong |

**Error Message:**
- Username tidak boleh kosong
- Username minimal 3 karakter
- Password tidak boleh kosong
- Password minimal 6 karakter
- Password tidak cocok
- Nama lengkap tidak boleh kosong
- Nomor telepon tidak boleh kosong
- Nomor telepon tidak valid
- Username sudah terdaftar (dari database)

---

## 🗄️ Database Schema

Data disimpan ke tabel `users` dengan struktur:

| Column | Value |
|--------|-------|
| id | Auto-increment |
| username | `passenger01` |
| password | `password123` (plain text - TODO: hash untuk production) |
| role | `penumpang` |
| fullName | `Penumpang Test` |
| phoneNumber | `081234567890` |
| email | `null` (optional) |
| createdAt | Timestamp |

---

## 🔐 Security Notes

⚠️ **PENTING untuk Production:**

1. **Hash Password**
   - Gunakan BCrypt atau library lain
   - Jangan simpan plain text password
   
2. **Email Verification**
   - Tambahkan field `isVerified` di tabel
   - Kirim email verifikasi setelah registrasi
   
3. **Username Validation**
   - Cek karakter khusus
   - Cegah SQL injection (sudah handled oleh Room)
   
4. **Rate Limiting**
   - Batasi jumlah percobaan registrasi
   - Implementasi CAPTCHA untuk anti-bot

---

## 🚀 Next Steps

### **Untuk Driver Registration:**
Saat ini form driver (motor/mobil/pengganti/owner) **belum terintegrasi dengan database**. 
Perlu tambahkan:
1. Fungsi `registerDriver()` di ViewModel
2. Tabel baru atau extend tabel `users` dengan field tambahan
3. Upload file (foto SIM, STNK, dll) - pakai storage lokal atau cloud

### **Untuk Login Multi-Role:**
✅ Sudah berfungsi - validasi role di database Room

### **Untuk Forgot Password:**
- Tambahkan screen forgot password
- Kirim OTP via SMS/Email
- Update password di database

---

## 📞 Troubleshooting

### **Error: "Username sudah terdaftar"**
**Penyebab:** Username sudah ada di database
**Solusi:** Gunakan username lain atau hapus user dari database

### **Error: "Registrasi gagal"**
**Penyebab:** Error saat menyimpan ke database
**Solusi:** Cek Logcat untuk detail error

### **Form tidak muncul**
**Penyebab:** Navigation route salah
**Solusi:** Cek di `MainActivity.kt` apakah route `register/passenger` sudah ditambahkan

### **Login gagal setelah registrasi**
**Penyebab:** Role tidak match atau password salah
**Solusi:** Pastikan pilih role "Penumpang" saat login

---

## ✅ Testing Checklist

- [x] Form registrasi muncul saat klik "Daftar"
- [x] Pilih "Penumpang" navigate ke form yang benar
- [x] Validasi username (min 3 karakter)
- [x] Validasi password (min 6 karakter)
- [x] Validasi konfirmasi password (harus sama)
- [x] Validasi nama lengkap (tidak boleh kosong)
- [x] Validasi nomor telepon (min 10 digit)
- [x] Error message muncul jika validasi gagal
- [x] Loading indicator saat submit
- [x] Toast notification saat berhasil
- [x] Redirect ke login screen setelah berhasil
- [x] Data tersimpan di database (persistent)
- [x] Bisa login dengan akun yang baru didaftarkan
- [x] Username tidak bisa duplicate

---

**Registrasi Penumpang sudah berhasil diimplementasikan dan terintegrasi dengan database Room!** 🎉

