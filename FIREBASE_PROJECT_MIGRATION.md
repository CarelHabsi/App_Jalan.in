# Firebase Project Migration: jalanin-database → jalanin-app

## ✅ Konfigurasi yang Sudah Benar

### 1. File Konfigurasi
- ✅ `.firebaserc` → Project: `jalanin-app`
- ✅ `app/google-services.json` → Project ID: `jalanin-app`, Project Number: `373300597326`
- ✅ `firebase.json` → Tidak ada referensi project ID (hanya hosting config)

### 2. Build Configuration
- ✅ `app/build.gradle.kts` → Plugin `google-services` sudah terpasang
- ✅ `build.gradle.kts` → Google Services plugin version 4.4.4

### 3. Code Configuration
- ✅ Tidak ada hardcoded Firebase project ID di kode
- ✅ Semua menggunakan `FirebaseApp.getInstance()` (default instance)
- ✅ AndroidManifest.xml → Domain: `jalanin-app.web.app`

## 🔧 Langkah untuk Memastikan Aplikasi Menggunakan Project yang Benar

### Step 1: Clean Build
```bash
# Di Android Studio:
Build → Clean Project
Build → Rebuild Project

# Atau via command line:
./gradlew clean
./gradlew build
```

### Step 2: Hapus Cache Build (SUDAH DILAKUKAN)
- ✅ Folder `app/build/` sudah dihapus
- ✅ Folder `.gradle/` sudah dihapus

### Step 3: Verifikasi google-services.json
File `app/google-services.json` harus berisi:
```json
{
  "project_info": {
    "project_id": "jalanin-app",
    "project_number": "373300597326"
  }
}
```

### Step 4: Uninstall Aplikasi dari Device/Emulator
**PENTING**: Cache Firebase di device/emulator mungkin masih menyimpan session dari project lama.

1. Uninstall aplikasi dari device/emulator:
   ```
   Settings → Apps → App_Jalan.In → Uninstall
   ```

2. Atau via ADB:
   ```bash
   adb uninstall com.example.app_jalanin
   ```

### Step 5: Rebuild dan Install Ulang
1. Rebuild aplikasi di Android Studio
2. Install ulang ke device/emulator
3. Cek logcat untuk melihat project ID yang digunakan:
   ```
   Filter: "Firebase Project Info"
   ```

### Step 6: Verifikasi di Firebase Console
1. Buka Firebase Console → Project: `jalanin-app`
2. Authentication → Users
3. Coba registrasi/login dengan aplikasi
4. Pastikan user muncul di project `jalanin-app`, bukan `jalanin-database`

## 🐛 Troubleshooting

### ❌ Error: "API key not valid. Please pass a valid API key."

**Penyebab**: API key di `google-services.json` tidak valid atau aplikasi Android belum terdaftar dengan benar di Firebase Console.

**Solusi**:

1. **Verifikasi Aplikasi Android di Firebase Console**:
   - Buka [Firebase Console](https://console.firebase.google.com/)
   - Pilih project **jalanin-app**
   - Pergi ke **Project Settings** (⚙️) → **Your apps**
   - Pastikan ada aplikasi Android dengan package name: `com.example.app_jalanin`
   - Jika tidak ada, klik **Add app** → **Android** → Masukkan package name: `com.example.app_jalanin`
   - Download file `google-services.json` yang baru
   - **Ganti** file `app/google-services.json` dengan file yang baru didownload

2. **Verifikasi API Key Restrictions** (jika masih error):
   - Buka [Google Cloud Console](https://console.cloud.google.com/)
   - Pilih project **jalanin-app**
   - Pergi ke **APIs & Services** → **Credentials**
   - Cari API key yang dimulai dengan `AIzaSy...` (dari `google-services.json`)
   - Klik API key tersebut
   - Di bagian **Application restrictions**, pastikan:
     - Pilih **Android apps**
     - Tambahkan package name: `com.example.app_jalanin`
     - Tambahkan SHA-1 certificate fingerprint (dapatkan dari Android Studio atau `keytool`)
   - Klik **Save**

3. **Dapatkan SHA-1 Fingerprint**:
   ```bash
   # Untuk debug keystore (default):
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   
   # Atau di Android Studio:
   # Gradle → app → Tasks → android → signingReport
   ```

4. **Setelah Update google-services.json**:
   - Clean build: `Build → Clean Project`
   - Rebuild: `Build → Rebuild Project`
   - Uninstall aplikasi dari device/emulator
   - Install ulang aplikasi

### Jika Masih Terhubung ke Project Lama:

1. **Cek Logcat**:
   - Filter: "Firebase Project Info"
   - Pastikan Project ID: `jalanin-app`
   - Jika masih `jalanin-database`, ada masalah dengan konfigurasi

2. **Cek File google-services.json yang Digenerate**:
   - Lokasi: `app/build/generated/res/processDebugGoogleServices/google-services.json`
   - Pastikan project_id: `jalanin-app`

3. **Clear App Data** (jika uninstall tidak membantu):
   ```bash
   adb shell pm clear com.example.app_jalanin
   ```

4. **Hapus Cache Firebase di Device**:
   - Uninstall aplikasi
   - Clear data aplikasi
   - Reinstall

### ⚠️ Error: "This operation is restricted to administrators only" (Anonymous Auth)

**Penyebab**: Anonymous Authentication belum diaktifkan di Firebase Console.

**Solusi**:
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih project **jalanin-app**
3. Pergi ke **Authentication** → **Sign-in method**
4. Klik **Anonymous** → **Enable** → **Save**
5. Aplikasi akan otomatis menggunakan anonymous auth setelah diaktifkan

**Catatan**: Aplikasi tetap berjalan tanpa anonymous auth (hanya warning di log). Anonymous auth digunakan untuk sync Firestore, tapi tidak wajib untuk fungsi utama aplikasi.

### ℹ️ Warning: "Firebase in offline mode"

**Ini adalah WARNING NORMAL**, bukan error!

- Firestore akan menggunakan cache lokal jika tidak ada koneksi internet
- Data akan otomatis sync ke cloud saat koneksi kembali
- Ini adalah fitur **offline persistence** yang built-in di Firestore
- Tidak perlu melakukan apa-apa, aplikasi tetap berfungsi normal

## 📝 Catatan Penting

- Nama "jalanin_database" di `AppDatabase.kt` adalah nama file database lokal Room (SQLite), **BUKAN** project Firebase. Ini normal dan tidak perlu diubah.
- Project "jalanin-database" di Firebase Console adalah project terpisah yang tidak terhubung ke aplikasi ini.
- Aplikasi ini sekarang sepenuhnya menggunakan project Firebase "jalanin-app".
- **Firebase Project Info** di logcat menunjukkan project yang benar: `jalanin-app` dengan Project Number `373300597326`.

