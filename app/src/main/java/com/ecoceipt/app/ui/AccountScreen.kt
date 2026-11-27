package com.ecoceipt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ecoceipt.app.BuildConfig
import com.ecoceipt.app.R

@Composable fun AccountScreen(s: UiState, vm: EcoceiptViewModel, logout: () -> Unit) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(56.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_account), null, Modifier.size(28.dp), tint = DarkTeal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(if (s.guest) "Akun Tamu" else if (s.demo) "Akun Uji" else "Akun Saya", 20, bold = true)
                Label(when {
                    s.guest -> "Data tersimpan di perangkat ini"
                    s.demo -> "test - Data tersimpan di perangkat ini"
                    else -> s.user
                }, 13, Muted)
            }
        }
    }
    Panel {
        Label("AKUN", 12, Muted, true)
        AccountOption("Informasi akun", "Status akun dan penyimpanan data") { dialog = "account" }
        if (!s.guest && !s.demo) {
            HorizontalDivider(color = LightGray)
            AccountOption("Ubah password", "Kirim tautan pemulihan ke email akun") { dialog = "password" }
        }
    }
    Panel {
        Label("BANTUAN & INFORMASI", 12, Muted, true)
        AccountOption("Bantuan", "Panduan penggunaan dan pertanyaan umum") { dialog = "help" }
        HorizontalDivider(color = LightGray)
        AccountOption("Tentang Ecoceipt", "Informasi aplikasi dan versi") { dialog = "about" }
    }
    if (s.guest) {
        Label("Masuk atau daftar untuk menggunakan akun. Data tamu tetap tersimpan terpisah di perangkat ini.", 12, Muted)
        Action("Masuk / Daftar Akun", enabled = !s.busy, onClick = vm::logout)
    } else {
        Action("Keluar dari Akun", enabled = !s.busy, secondary = true, framed = true, onClick = logout)
    }

    dialog?.let { page ->
        AlertDialog(onDismissRequest = { dialog = null }, title = {
            Text(when (page) {
                "account" -> "Informasi Akun"
                "password" -> "Ubah Password"
                "help" -> "Bantuan"
                else -> "Tentang Ecoceipt"
            })
        }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    "account" -> {
                        Label(if (s.guest) "Status: Tamu" else if (s.demo) "Username: test (akun uji)" else "Email: ${s.user}", bold = true)
                        Label(when {
                            s.guest -> "Nota dan stok disimpan di perangkat ini. Keluar tidak menghapus data tamu. Data ini terpisah dari data akun."
                            s.demo -> "Nota dan stok akun uji tersimpan di perangkat ini, terpisah dari data tamu dan akun Firebase."
                            else -> "Nota dan stok disimpan pada akunmu. Masuk dengan akun yang sama untuk membuka kembali data."
                        }, 13, Muted)
                    }
                    "password" -> Label("Kirim tautan untuk mengubah password ke ${s.user}? Periksa kotak masuk dan folder spam setelah tautan dikirim.", 14)
                    "help" -> {
                        Label("Bagaimana mencatat nota?", bold = true)
                        Label("Buka Beranda > Mulai Pindai. Foto nota atau pilih dari galeri, periksa hasil pembacaan, lalu simpan transaksi.", 13, Muted)
                        Label("Bagaimana mengatur stok?", bold = true)
                        Label("Buka Stok untuk menambah produk dan mengisi stok tersedia, lead time, serta stok cadangan.", 13, Muted)
                        Label("Mengapa rekomendasi belum muncul?", bold = true)
                        Label("Lengkapi data produk dan catat penjualan. Rekomendasi menggunakan riwayat penjualan 7 hari terakhir.", 13, Muted)
                        Label("Nota tidak terbaca?", bold = true)
                        Label("Gunakan pencahayaan yang cukup dan pastikan nota tidak terlipat. Kamu juga bisa memilih Isi Manual setelah pemindaian gagal.", 13, Muted)
                    }
                    else -> {
                        Image(painterResource(R.drawable.ecoceipt_logo), "Logo Ecoceipt", Modifier.size(112.dp)
                            .align(Alignment.CenterHorizontally))
                        Label("Catat nota, pantau penjualan, dan kelola kebutuhan stok usahamu.", 13, Muted)
                        Label("Versi ${BuildConfig.VERSION_NAME}", 12, Muted)
                    }
                }
            }
        }, confirmButton = {
            TextButton(onClick = {
                if (page == "password") vm.resetPassword(s.user)
                dialog = null
            }) { Text(if (page == "password") "Kirim Tautan" else "Tutup") }
        }, dismissButton = {
            if (page == "password") TextButton(onClick = { dialog = null }) { Text("Batal") }
        })
    }
}

@Composable private fun AccountOption(title: String, description: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = Round,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(title, 14, bold = true)
            Label(description, 12, Muted)
        }
        Label("›", 24, Muted, modifier = Modifier.padding(start = 8.dp))
    }
}
