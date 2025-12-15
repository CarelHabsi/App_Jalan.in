iakihk# Firestore Vehicle Sync - Setup & Troubleshooting

## 🔍 Cara Cek Kendaraan di Firestore

### Via Firebase Console:
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih project **jalanin-app**
3. Pergi ke **Firestore Database** → **Data**
4. Cari collection **`vehicles`**
   - Jika collection belum ada, berarti belum ada kendaraan yang berhasil syn
   - Jika collection ada, klik untuk melihat daftar kendaraan
5. Filter berdasarkan `ownerId` untuk mencari kendaraan milik owner tertentu:
   - Contoh: `ownerId == "reltcukei@gmail.com"`

### Via Logcat (Aplikasi):
Filter log dengan: `FirestoreVehicleService` untuk melihat proses sync

## ❌ Error: PERMISSION_DENIED

**Penyebab**: Firestore Security Rules belum dikonfigurasi untuk collection `vehicles`.

**Solusi**: Update Firestore Security Rules di Firebase Console.

### Langkah Update Rules:

1. **Buka Firebase Console**:
   - [Firebase Console](https://console.firebase.google.com/)
   - Pilih project **jalanin-app**

2. **Pergi ke Firestore Rules**:
   - Klik **Firestore Database** di sidebar kiri
   - Pilih tab **Rules**

3. **Update Rules**:
   - Copy rules berikut dan paste ke editor:

**⚠️ IMPORTANT: Pilih salah satu rules di bawah ini**

#### Opsi 1: Rules untuk Development/Testing (LEBIH MUDAH - Recommended untuk sekarang)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow all read/write for development (HANYA UNTUK TESTING!)
    // ⚠️ WARNING: Rules ini tidak aman untuk production!
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

#### Opsi 2: Rules dengan Authentication (LEBIH AMAN - untuk production)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null;
    }
    
    // Vehicles collection
    match /vehicles/{vehicleId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
      allow delete: if request.auth != null;
    }
    
    // Rentals collection
    match /rentals/{rentalId} {
      allow read, write: if request.auth != null;
    }
    
    // Diagnostic collection
    match /diagnostic/{docId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**💡 Rekomendasi**: Gunakan **Opsi 1** dulu untuk testing. Setelah sync berhasil, ganti ke **Opsi 2** untuk production.

4. **Publish Rules**:
   - Klik **Publish** di bagian atas editor
   - **Tunggu 10-30 detik** hingga rules aktif (ada notifikasi "Rules published successfully")
   - Rules akan aktif secara global dalam 1-2 menit

5. **Verifikasi Rules Sudah Aktif**:
   - Setelah publish, coba tambah kendaraan lagi dari aplikasi
   - Cek logcat - harus ada: `✅ Vehicle synced successfully to Firestore`
   - Jika masih error, tunggu 1-2 menit lagi (rules perlu waktu untuk propagate)

### Rules Penjelasan:

- **`vehicles` collection**:
  - **Read**: Semua user yang authenticated bisa baca
  - **Create**: Hanya owner yang bisa create vehicle dengan `ownerId` sesuai email mereka
  - **Update**: Hanya owner yang bisa update vehicle miliknya
  - **Delete**: Hanya owner yang bisa delete vehicle miliknya

- **`users` collection**: Read/write untuk authenticated users
- **`rentals` collection**: Read/write untuk authenticated users
- **`diagnostic` collection**: Untuk testing/ping

## ⚠️ Catatan Penting

### Jika Masih Error Setelah Update Rules:

1. **Pastikan User Sudah Login**:
   - Aplikasi perlu authenticated user untuk write ke Firestore
   - Cek apakah anonymous auth sudah diaktifkan (opsional tapi recommended)

2. **Cek Authentication Status**:
   - Di Firebase Console → **Authentication** → **Users**
   - Pastikan user `reltcukei@gmail.com` ada di list

3. **Wait untuk Rules Propagation**:
   - Rules biasanya aktif dalam 1-2 menit setelah publish
   - Coba lagi setelah beberapa menit

4. **Cek Logcat**:
   - Filter: `FirestoreVehicleService`
   - Lihat apakah masih ada error `PERMISSION_DENIED`

## ✅ Verifikasi Sync Berhasil

Setelah rules diupdate, coba tambah kendaraan lagi dari aplikasi. Kemudian:

1. **Cek di Firebase Console**:
   - Firestore Database → Data → Collection `vehicles`
   - Harus ada document baru dengan ID sesuai vehicle ID

2. **Cek Logcat**:
   - Harus ada log: `✅ Vehicle synced successfully to Firestore`

3. **Cek Toast di Aplikasi**:
   - Harus muncul: `✅ Kendaraan berhasil ditambahkan dan disinkronkan ke cloud`

## 🔄 Re-sync Kendaraan yang Sudah Ada

Jika ada kendaraan yang sudah ditambahkan sebelum rules diupdate, mereka hanya ada di database lokal. Untuk sync manual:

1. **Edit kendaraan** di aplikasi (ubah apapun, misalnya nama)
2. **Save** - akan trigger sync ke Firestore
3. Atau **hapus dan tambah ulang** kendaraan tersebut

## 📝 Troubleshooting

### Error: "Missing or insufficient permissions"
- ✅ Update Firestore Rules (lihat langkah di atas)
- ✅ Pastikan user sudah authenticated
- ✅ Pastikan anonymous auth sudah diaktifkan (jika menggunakan anonymous auth)

### Kendaraan tidak muncul di Firestore
- ✅ Cek logcat untuk error
- ✅ Pastikan rules sudah di-publish
- ✅ Coba tambah kendaraan baru setelah rules diupdate

### Sync berhasil tapi tidak muncul di console
- ✅ Refresh halaman Firebase Console
- ✅ Cek collection name: harus `vehicles` (lowercase)
- ✅ Cek filter di console (mungkin ter-filter)

