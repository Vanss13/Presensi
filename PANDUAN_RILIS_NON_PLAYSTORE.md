# Panduan Rilis APK Publik HADIRIN (Tanpa Google Play Store)

Dokumen ini berisi langkah-langkah lengkap untuk membangun, menandatangani, mengonfigurasi Login Google publik, dan menyebarkan aplikasi **HADIRIN** (`com.aistudio.hadirin.kmpqzx`) ke pengguna umum tanpa melalui Google Play Store.

---

## Informasi Proyek Saat Ini
- **Application ID (Package Name):** `com.aistudio.hadirin.kmpqzx`
- **Firebase Project ID:** `vanss-clipper`
- **Firebase Project Number:** `1053328944339`
- **Firestore Database ID:** `ai-studio-android-hadirin-88f34d95-db61-44e2-a7cb-c60572f635a0`
- **Super Admin Awal:** `halimawan13@gmail.com`

---

## Langkah 1 — Siapkan Release Keystore Permanen

> Simpan file keystore dan password-nya di tempat yang aman. Jangan hilang, karena pembaruan APK di masa depan harus ditandatangani dengan keystore yang sama.

1. Ekspor proyek ini dari AI Studio (ZIP atau Push to GitHub).
2. Jalankan perintah berikut di terminal komputer Anda pada folder root proyek untuk membuat `my-upload-key.jks`:

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias upload
```

3. Cek dan salin sidik jari **SHA-1** dan **SHA-256** dari keystore tersebut:

```bash
keytool -list -v -keystore my-upload-key.jks -alias upload
```

---

## Langkah 2 — Daftarkan SHA-1 & SHA-256 Release ke Firebase

Agar **Masuk dengan Google** berfungsi pada APK *release* di semua HP Android:

1. Buka [Firebase Console](https://console.firebase.google.com/) → pilih proyek **`vanss-clipper`**.
2. Buka **Project Settings** (ikon gerigi) → bagian **Your apps** → pilih aplikasi Android **`com.aistudio.hadirin.kmpqzx`**.
3. Klik **Add fingerprint**, lalu masukkan **SHA-1** dan **SHA-256** dari `my-upload-key.jks`. *(Biarkan SHA-1 debug tetap terdaftar untuk pengujian lokal).*
4. Unduh file **`google-services.json`** terbaru dari Firebase Console, lalu timpa file `app/google-services.json` di proyek Anda sebelum melakukan build APK *release*.

---

## Langkah 3 — Ubah Google OAuth Consent Screen ke "In Production"

Jika status OAuth masih *Testing*, hanya email yang didaftarkan manual sebagai *Test User* yang bisa login. Untuk penggunaan umum oleh banyak pengguna:

1. Buka [Google Cloud Console](https://console.cloud.google.com/) dan pastikan memilih proyek **`vanss-clipper`** (`1053328944339`).
2. Masuk ke **APIs & Services → OAuth consent screen**.
3. Pastikan nama aplikasi (**HADIRIN**), email dukungan pengguna, dan informasi kontak developer sudah terisi.
4. Klik **Publish App** untuk mengubah status dari **Testing** menjadi **In Production**.

---

## Langkah 4 — Bangun File APK Release

Anda dapat langsung menggunakan menu **Generate APK** di AI Studio (jika menggunakan signing bawaan AI Studio), atau membangun APK *release* yang ditandatangani dengan keystore Anda sendiri melalui terminal:

```bash
export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="PASSWORD_KEYSTORE_ANDA"
export KEY_PASSWORD="PASSWORD_KEY_ANDA"

gradle assembleRelease
```

File APK siap sebar akan tersedia di:
`app/build/outputs/apk/release/app-release.apk`

---

## Langkah 5 — Sebarkan ke Pengguna Tanpa Play Store

1. **Uji di Perangkat Fisik**: Pasang `app-release.apk` di minimal 2 HP Android berbeda dan pastikan **Masuk dengan Google** berjalan normal dengan akun selain `halimawan13@gmail.com`.
2. **Bagikan APK**:
   - **Tautan Unduh Publik**: Unggah `app-release.apk` ke Google Drive, website resmi organisasi, atau GitHub Releases, lalu bagikan link-nya.
   - **WhatsApp / Grup Organisasi**: Kirim file APK atau link unduh beserta **Kode Gabung Organisasi** (`WRK...` / `EDU...`).
   - **Firebase App Distribution** (opsional): Unggah APK melalui tab *Release & Monitor → App Distribution* di Firebase Console untuk distribusi privat via tautan undangan email.
3. **Panduan Pemasangan untuk Pengguna**:
   - Ketuk file `HADIRIN.apk` yang sudah diunduh.
   - Jika muncul peringatan Android, pilih **Setelan → Izinkan dari sumber ini**, lalu ketuk **Pasang / Install**.
   - Buka aplikasi dan pilih **Masuk dengan Google** (atau **Mode Uji Coba** untuk simulasi offline).

---

## Catatan Skalabilitas Multi-Tenant
- Database Firestore dan `firestore.rules` sudah memisahkan data setiap sekolah/perusahaan berdasarkan `organizationId`.
- Jika jumlah pengguna aktif harian meningkat besar, pastikan billing proyek **`vanss-clipper`** di Firebase Console berada pada paket **Blaze** (dengan *Budget Alert*) agar jam sibuk presensi pagi/pulang tidak terbentur limit harian paket gratis.
