# Ecoceipt — Kelompok 9 PAPB

Referensi ide: [Ecoceipt di Devpost](https://devpost.com/software/ecoceipt). Referensi UI: [Figma kelompok](https://www.figma.com/design/C5VDJqCqyTIE0QLUmkivXz/PROJECT-AKHIR-PRAKTIKUM?node-id=0-1).

## Status pengerjaan

- 20 pengujian domain JUnit sudah dijalankan langsung menggunakan compiler Kotlin lokal: seluruhnya lulus.
- Parser Kotlin memeriksa 16 berkas sumber/pengujian: tidak ditemukan kesalahan sintaks. Ini belum memverifikasi seluruh resolusi API Android dan dependensi.
- 4 pengujian parser JSON sudah ditulis, belum dijalankan karena dependensi JSON JVM belum tersedia.
- Firebase belum dibuat oleh kelompok. Login, penyimpanan Firestore, dan Gemini belum diuji terhadap layanan nyata.
- Mode tamu menggunakan penyimpanan lokal; tidak membutuhkan akun Firebase. OCR tetap menggunakan ML Kit asli. Tanpa Firebase, penataan Gemini menampilkan kegagalan yang dapat dilanjutkan dengan koreksi/input manual.
- Tujuh aset Figma sudah tersedia di `app/src/main/assets/figma`; build memeriksa kelengkapannya.
- Tidak ada data contoh yang disisipkan ke database, tidak ada fitur ekspor, notifikasi, pembayaran, atau integrasi POS tambahan.

## Menjalankan di Android Studio

1. Buka folder **ecoceipt**, bukan folder induk `papb`.
2. Gunakan Android Studio yang mendukung AGP 9.3.3, Gradle 9.5.0, dan SDK 37. Pilih **Embedded JDK** di pengaturan Gradle. SDK minimum aplikasi adalah Android 8 / API 26.
3. Unduh aset asli dari PowerShell yang terhubung internet:

   ```powershell
   cd C:\Users\izyan\Semester6\papb\ecoceipt
   & .\scripts\download-figma-assets.ps1
   ```

4. Sync Gradle. Android Studio akan membuat `local.properties` sesuai SDK komputer. Berkas ini tidak masuk Git.
5. Jalankan pada emulator/perangkat. Pilih **Continue as Guest** untuk mencoba data lokal; data tetap ada setelah aplikasi ditutup. Data tamu terpisah dari data akun dan tidak dipindahkan otomatis.

Perintah build/test setelah dependensi dan aset tersedia:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

APK akan berada di `app/build/outputs/apk/debug/app-debug.apk` **setelah build berhasil**.

## Mengaktifkan Firebase dan Gemini

1. Buat proyek Firebase milik kelompok di [Firebase Console](https://console.firebase.google.com/). Daftarkan aplikasi Android dengan package **com.ecoceipt.app**.
2. Unduh `google-services.json`, lalu taruh di **app/google-services.json**. File itu sudah diabaikan oleh Git. Jangan masukkan service-account key ke aplikasi.
3. Aktifkan **Authentication → Email/Password**. Fitur Sign In, Create Account, dan pemulihan password sudah dihubungkan ke Firebase Authentication.
4. Buat Cloud Firestore. Pasang isi `firestore.rules` melalui tab Rules. Data akun berada di `users/{uid}/products` dan `users/{uid}/receipts`; pengguna lain tidak memperoleh akses.
5. Aktifkan Firebase AI Logic dengan Gemini Developer API sesuai alur console. Kode memakai Firebase AI Logic untuk mengakses Gemini tanpa menyimpan secret Gemini API key di APK. Periksa persyaratan dan kuota yang ditampilkan console sebelum mengaktifkan layanan.
6. Untuk build debug, daftarkan token App Check debug perangkat yang muncul di Logcat pada Firebase Console. Build release memakai Play Integrity. Jangan menonaktifkan aturan akses Firestore untuk mengatasi error.
7. Model awal adalah `gemini-3.5-flash-lite`. Pastikan masih tersedia di proyek Firebase; model dapat diganti melalui properti Gradle `geminiModel` tanpa mengubah alur aplikasi:

   ```powershell
   .\gradlew.bat :app:assembleDebug -PgeminiModel=nama-model-yang-tersedia
   ```

8. Sync, build, lalu uji akun dan pemindaian nyata. Kode layanan tidak menggantikan pengaktifan layanan di console.

Dokumentasi resmi: [Firebase AI Logic Android](https://firebase.google.com/docs/ai-logic/get-started), [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

## Kesesuaian teknologi dengan PDF

Acuan: bagian 1.3, 2.3, 2.4, dan contoh implementasi pada Bab III.

| Teknologi pada PDF | Pemakaian di proyek |
|---|---|
| Kotlin dan Jetpack Compose | Seluruh kode aplikasi Android dan UI Compose |
| Material Design 3 | Komponen dan tema `androidx.compose.material3` |
| Google ML Kit Text Recognition | Ekstraksi teks gambar di `ReceiptScanner.recognize` |
| Gemini API | `ReceiptScanner.format` melalui Firebase AI Logic, backend Google AI |
| Firebase Firestore | `FirestoreRepository` untuk transaksi dan produk milik akun |
| ViewModel dan MutableStateFlow | `EcoceiptViewModel` untuk event dan state |
| Coroutine | Pemrosesan asinkron, IO, dan perhitungan domain |

CameraX mendukung pengambilan foto; Coil memuat gambar dan SVG; Firebase Authentication mendukung halaman akun; App Check mendukung akses layanan. JUnit dipakai untuk pengujian. Ini adalah dependensi pendukung, bukan pengganti teknologi inti PDF.

Room/SQLite disebut sebagai contoh pada Bab III, bukan persyaratan penyimpanan utama. Mode tamu menggunakan file JSON lokal; penyimpanan akun menggunakan Firestore sesuai rancangan. Integrasi Firestore dan Gemini sudah ada dalam kode, tetapi layanan belum dapat diverifikasi sebelum konfigurasi Firebase tersedia. PDF tidak menentukan versi SDK atau library tertentu.

## Pemetaan ketentuan PDF ke kode

| Ketentuan | Implementasi |
|---|---|
| Fungsi 1: ekstraksi OCR kamera/galeri dan hasil kosong | `data/ReceiptScanner.kt`, `ui/CameraScreen.kt` |
| Fungsi 2: penataan Gemini menjadi nama, jumlah, harga | `ReceiptScanner.format`, `data/ReceiptJson.kt` |
| Pemeriksaan dan koreksi sebelum simpan | `ReviewScreen`, `ItemSheet`; data invalid/unconfirmed ditolak oleh `Inventory.validate` |
| Penyimpanan Firebase Firestore | `data/EcoceiptRepository.kt`, `FirestoreRepository` |
| Fungsi 3: total, rata-rata harian, tren, perubahan periode, produk terlaris | `domain/SalesAnalysis.kt`, `ui/SalesScreens.kt` |
| Fungsi 4: ROP, status dan saran pengadaan | `StockRecommender`, `ui/StockScreens.kt` |
| Stok, lead time dan safety stock | `Product`, `ProductSheet`, validasi `Inventory.validateProduct` |
| Pemisahan nota penjualan dan pembelian | `ReceiptType.SALE/PURCHASE`; analisis hanya mengambil SALE |
| Event handling dan state | `EcoceiptViewModel`, `StateFlow`, `SavedStateHandle` untuk draft |
| Operasi data tidak memblokir UI | coroutine, Firebase Task.await, OCR/file lokal di Dispatchers.IO, analisis dan ROP di Dispatchers.Default |
| UI/UX Figma | warna, kartu, bottom navigation dan layar berdasarkan acuan; batas verifikasi di `docs/FIGMA.md` |

Kode sumber berada di `app/src/main/java/com/ecoceipt/app/`. Tidak menyalin source code proyek Devpost; proyek ini implementasi baru untuk rancangan kelompok.

## Aturan perhitungan

- Nilai uang adalah bilangan bulat rupiah (`Long`). Jumlah barang berupa unit/pcs bulat sesuai rancangan.
- Rata-rata restock = jumlah terjual selama **7 hari kalender terakhir termasuk hari ini / 7**. Hari tanpa transaksi tetap dihitung.
- ROP = `ceil(rata-rata × lead time + safety stock)`.
- Rekomendasi muncul jika `stok tersedia <= ROP`.
- Saran beli = `max(0, ceil(rata-rata × (lead time + 7) + safety stock) − stok tersedia)` saat perlu restock. Tambahan 7 hari adalah periode peninjauan yang ditetapkan implementasi karena PDF tidak merinci horizon pengadaan; penjelasan terlihat pada detail produk.
- Contoh 21 pcs terjual dalam 7 hari, lead time 4, cadangan 6, stok 4: rata-rata 3, ROP 18, saran beli 35. Nilai contoh 30 pada Figma tidak dipaksakan karena tidak diturunkan dari rumus yang ditetapkan.
- Stok/lead time/cadangan kosong dibedakan dari 0. Produk belum lengkap tidak dihitung. Produk tanpa penjualan dalam 7 hari diberi status belum ada penjualan, bukan perkiraan permintaan rekaan.
- Grafik menampilkan paling banyak 7 hari terakhir dalam periode terpilih. Ringkasan mencakup seluruh periode. Minggu dimulai Senin. Persentase membandingkan periode sebelumnya dengan jumlah hari yang sama; jika pembanding nol, tampilkan tidak tersedia.
- Nota baru yang dikonfirmasi memperbarui stok yang sudah diketahui. Jika stok awal belum diketahui, nilainya tetap kosong sampai pengguna memasukkan jumlah fisik saat ini. Lengkapi stok awal sebelum mencatat penjualan baru. Jangan memasukkan kembali nota lama yang sudah tercermin dalam stok saat ini tanpa penyesuaian stok fisik.
- Nama barang dicocokkan setelah normalisasi huruf dan spasi; ukuran barang tetap dibedakan. Koreksi penulisan OCR agar produk yang sama tidak menjadi produk berbeda.
- UUID transaksi mencegah penyimpanan ulang draft yang sama. Foto yang sama dipindai ulang sebagai draft baru tidak otomatis dideteksi duplikat.
- Penjualan dan perubahan stok disimpan atomik. Stok negatif ditolak. Form stok memeriksa revisi agar perubahan pengguna lain tidak tertimpa.

## Pengujian

Jalankan `gradlew :app:testDebugUnitTest` untuk 20 pengujian domain dan 4 parser JSON setelah dependensi tersedia. Alternatif untuk komputer yang sudah punya compiler Kotlin 2.2.10 dan JUnit di cache Gradle:

```powershell
python scripts/test-domain-offline.py
```

Ini hanya mengompilasi domain dan menjalankan JUnit asli; bukan pengganti build Android.

Uji perangkat yang masih harus dilakukan:

1. Login/daftar/reset password, akses akun terpisah, dan mode tamu setelah restart.
2. Kamera dengan izin diterima/ditolak, galeri, foto kosong/buram, batal saat OCR/AI, serta rotasi layar.
3. Gemini normal, timeout, respons invalid, dan koreksi item hingga transaksi valid.
4. Penjualan mengurangi stok; pembelian menambah stok; nota pembelian tidak masuk tren penjualan.
5. Isi stok awal 25, lead time 4, cadangan 6; catat total 21 unit penjualan dalam 7 hari. Stok menjadi 4, rata-rata 3, ROP 18, dan saran beli 35.
6. Tambah produk dengan data kosong vs nilai 0, filter riwayat, pembanding periode nol, dan penolakan stok tidak cukup.
7. Bandingkan tampilan pada lebar 393 dp dengan desain Figma dan perbaiki ketiga layar yang konteks detailnya belum tersedia.

Bab III–V laporan, screenshot hasil emulator, dan video demo belum diisi otomatis karena memerlukan hasil aplikasi yang benar-benar dijalankan.
