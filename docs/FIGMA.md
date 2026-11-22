# Referensi UI Ecoceipt

Sumber: https://www.figma.com/design/C5VDJqCqyTIE0QLUmkivXz/PROJECT-AKHIR-PRAKTIKUM?node-id=0-1

Desain memakai latar #DDF3EF, header #0F6865, tombol/chip #168C84, teks #263238, kartu putih dengan sudut 12 dp. Lebar referensi 393 px. Layout Compose dibuat responsif dan dapat digulir; tombol diberi area sentuh yang memadai. Grafik dan angka berasal dari data aplikasi.

| Layar | Node Figma | Acuan tersedia |
|---|---|---|
| Beranda | 2129:528 | Konteks lengkap dan screenshot |
| Login | 2022:463 | Konteks lengkap dan screenshot |
| Pindai | 2022:464 | Konteks lengkap dan screenshot |
| Analisis | 2022:474 | Konteks lengkap dan screenshot |
| Data Stok | 2129:608 | Konteks lengkap dan screenshot |
| Rekomendasi | 2108:2 | Konteks lengkap dan screenshot |
| Penjelasan ROP | 2108:117 | Konteks lengkap dan screenshot |
| Detail Produk | 2088:785 | Konteks lengkap dan screenshot |
| Data Belum Lengkap | 2088:760 | Konteks lengkap dan screenshot |
| Pemrosesan OCR/AI | 2111:290 | Konteks lengkap dan screenshot |
| Koreksi | 2111:323 | Konteks lengkap dan screenshot |
| Ubah Item | 2111:401 | Konteks lengkap dan screenshot |
| Pemindaian Gagal | 2111:503 | Konteks lengkap dan screenshot |
| Riwayat | 2022:472 | Detail terhalang batas pemakaian Figma; UI sementara berdasarkan kebutuhan PDF |
| Catat Pembelian | 2088:828 | Detail terhalang batas pemakaian Figma; form berdasarkan kebutuhan PDF |
| Form Data Stok | 2108:255 | Detail terhalang batas pemakaian Figma; form berdasarkan kebutuhan PDF |

## Aset asli

Unduhan dari lingkungan pengerjaan gagal karena DNS/jaringan dibatasi. Tidak ada gambar pengganti yang dibuat. Jalankan `scripts/download-figma-assets.ps1` di PowerShell dengan akses jaringan sebelum build. URL ekspor bersifat sementara (sekitar 7 hari sejak diperoleh); jika kedaluwarsa, ekspor kembali dari file Figma yang sama dan simpan dengan nama berikut.

| Berkas di app/src/main/assets/figma | Penggunaan | Node sumber |
|---|---|---|
| 8d105.png | Logo header 57 × 57 dp | 2104:137 / 2108:253 |
| 53454.png | Ikon total penjualan | 2022:474 |
| 7d5c6.png | Ikon total transaksi | 2022:474 |
| c7474.svg | Empat ikon menu Beranda, 28 × 28 dp | 2129:555 |
| 60c30.svg | Tombol ambil foto, 90 × 90 dp | 2045:18 |
| d2a24.svg | Kontrol lampu kilat, 70 × 70 dp | 2045:19 |
| eb40c.svg | Kontrol galeri, 40 × 40 dp | 2045:21 |

Kotak abu-abu pada Login adalah elemen desain asli, bukan logo buatan. Font Inter belum tersedia lokal; versi kode ini memakai font sans Android. Kecocokan tampilan, ukuran ikon analisis, seluruh aset, dan ketiga layar yang detailnya belum terbaca masih perlu diverifikasi pada emulator. Belum ada klaim kecocokan pixel-perfect.
