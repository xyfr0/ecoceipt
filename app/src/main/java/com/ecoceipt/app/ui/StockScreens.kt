package com.ecoceipt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecoceipt.app.domain.*
import java.time.LocalDate
import java.util.UUID

@Composable fun StockScreen(s: UiState, vm: EcoceiptViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var form by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    SearchField(query, { query = it }, "Cari nama produk")
    Panel {
        Label("Kelengkapan data", 12, Muted, true)
        Filters(listOf("Semua", "Lengkap", "Belum Lengkap"), filter) { filter = it }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Label("Daftar produk", bold = true); Link("+ Tambah Produk", { editing = null; form = true })
    }
    val products = s.products.filter { it.name.contains(query.trim(), true) && (filter == "Semua" || (filter == "Lengkap") == it.complete) }
    if (products.isEmpty()) {
        if (s.products.isEmpty()) Empty("Belum ada produk", "Tambahkan produk atau simpan nota. Isi stok tersedia sesuai jumlah saat ini.")
        else Empty("Produk tidak ditemukan", "Coba kata kunci lain atau ubah filter stok.")
    }
    products.forEach { product ->
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(product.name, bold = true, modifier = Modifier.weight(1f))
                Link(if (product.complete) "Ubah" else "Lengkapi", { editing = product; form = true })
                Link("Hapus", { deleting = product }, Danger)
            }
            if (!product.complete) Badge("Data belum lengkap")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label("Stok ${product.stock?.let { "$it pcs" } ?: "—"}", 12, Muted)
                Label("Lead time ${product.leadTimeDays?.let { "$it hari" } ?: "—"}", 12, Muted)
                Label("Cadangan ${product.safetyStock?.let { "$it pcs" } ?: "—"}", 12, Muted)
            }
        }
    }
    if (form) ProductSheet(editing, s.busy, { form = false }) { product -> vm.saveProduct(product) { form = false } }
    deleting?.let { product ->
        AlertDialog(onDismissRequest = { if (!s.busy) deleting = null },
            title = { Text("Hapus produk?") },
            text = { Text("${product.name} akan dihapus dari daftar stok dan rekomendasi. Riwayat transaksi tetap tersimpan. Jika produk ini ada di nota baru, produk akan muncul kembali.") },
            confirmButton = { TextButton(onClick = { vm.deleteProduct(product) { deleting = null } }, enabled = !s.busy) {
                Text("Hapus", color = Danger)
            } },
            dismissButton = { TextButton(onClick = { deleting = null }, enabled = !s.busy) { Text("Batal") } })
    }
}

@Composable fun RecommendationsScreen(s: UiState, vm: EcoceiptViewModel) {
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var explanation by remember { mutableStateOf(false) }
    val recommendations = s.recommendations
    Panel {
        Label("Status stok", 12, Muted, true)
        Filters(listOf("Semua", "Perlu Restock", "Cukup"), filter) { filter = it }
    }
    val missing = s.products.count { !it.complete }
    if (missing > 0) Panel(Modifier.clickable { vm.navigate(Screen.INCOMPLETE) }) {
        Label("$missing produk belum lengkap", 12, bold = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("Belum ikut dihitung dalam rekomendasi", 11, Muted)
            Label("Lengkapi", 12, DarkTeal, true)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Label("Rekomendasi", bold = true); Link("ⓘ Batas pesan ulang?", { explanation = true })
    }
    val shown = recommendations.filter { filter == "Semua" || if (filter == "Perlu Restock") it.needsRestock else it.reorderPoint != null && !it.needsRestock }
    if (shown.isEmpty()) Empty("Belum ada rekomendasi", "Lengkapi data stok dan catat penjualan agar kebutuhan barang dapat dihitung.", com.ecoceipt.app.R.drawable.ic_recommendations)
    shown.forEach { r ->
        Panel(Modifier.clickable { if (r.product.complete) vm.selectProduct(r.product.id) else vm.navigate(Screen.INCOMPLETE) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(r.product.name, bold = true, modifier = Modifier.weight(1f))
                val badgeColor = when {
                    r.needsRestock -> Danger
                    r.nearRestock -> Warning
                    else -> SafeGreen
                }
                val badgeBg = when {
                    r.needsRestock -> Color(0xFFFDE8E6)
                    r.nearRestock -> Color(0xFFFFF4DC)
                    else -> Mint
                }
                Text(r.status, color = badgeColor, fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                    modifier = Modifier.background(badgeBg, Round).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            if (!r.product.complete) Label("Belum ada: ${r.product.missing}", 12, Muted)
            else if (!r.hasSalesHistory) Label("Belum ada penjualan 7 hari terakhir. Rekomendasi belum dihitung.", 12, Muted)
            else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label("Stok ${r.product.stock} pcs", 12, Muted)
                    Label("Batas ${r.reorderPoint} pcs", 12, Muted)
                    Label("Beli ${r.suggestedPurchase} pcs", 12, Muted)
                }
                val ratio = (requireNotNull(r.product.stock).toFloat() / requireNotNull(r.reorderPoint).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(6.dp).background(LightGray, Round)) {
                    val barColor = when {
                        r.needsRestock -> Danger
                        r.nearRestock -> Warning
                        else -> SafeGreen
                    }
                    Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().background(barColor, Round))
                }
            }
        }
    }
    if (explanation) ExplanationSheet { explanation = false }
}

@Composable fun ProductScreen(s: UiState, vm: EcoceiptViewModel) {
    val product = s.products.find { it.id == s.selectedProduct }
    if (product == null) { Empty("Produk tidak ditemukan", "Kembali ke daftar stok."); return }
    val r = s.recommendations.find { it.product.id == product.id } ?: return
    var purchase by remember { mutableStateOf(false) }
    Badge(r.status, r.needsRestock)
    Panel {
        Label("Penjualan 7 hari terakhir", 13, bold = true)
        Bars(r.dailySales)
        Label("Rata-rata ${decimal(r.averageDailySales)} pcs per hari", 12, Muted)
    }
    Panel {
        Label("Rincian batas pesan ulang", 13, bold = true)
        KeyValue("Terjual per hari", "${decimal(r.averageDailySales)} pcs")
        KeyValue("Lama barang datang", "${product.leadTimeDays ?: "—"} hari")
        KeyValue("Stok cadangan", "${product.safetyStock ?: "—"} pcs")
        HorizontalDivider()
        KeyValue("Batas pesan ulang", r.reorderPoint?.let { "$it pcs" } ?: "Belum dihitung", DarkTeal)
        KeyValue("Stok saat ini", product.stock?.let { "$it pcs" } ?: "Belum diisi", if (r.needsRestock) Danger else Ink)
    }
    Panel {
        KeyValue("Saran pengadaan", r.suggestedPurchase?.let { "$it pcs" } ?: "Belum dihitung")
        Label("Target stok: kebutuhan selama lead time + 7 hari, ditambah stok cadangan, dikurangi stok tersedia. Dibulatkan ke atas.", 12, Muted)
    }
    Action("Catat Pembelian") { purchase = true }
    if (purchase) PurchaseSheet(product, r.suggestedPurchase, s.busy, { purchase = false }) { qty, price, date, id ->
        vm.recordPurchase(product, qty, price, date, id) { purchase = false }
    }
}

@Composable fun IncompleteScreen(s: UiState, vm: EcoceiptViewModel) {
    var editing by remember { mutableStateOf<Product?>(null) }
    Label("Data stok belum lengkap", 20, bold = true)
    Label("Kami butuh stok tersedia, lama barang datang, dan stok cadangan agar bisa menghitung kapan kamu perlu belanja.", 14, Muted)
    val missing = s.products.filter { !it.complete }
    if (missing.isEmpty()) Empty("Data sudah lengkap", "Kembali ke rekomendasi untuk melihat hasil perhitungan.")
    missing.forEach { p -> Panel(Modifier.clickable { editing = p }) { Label(p.name, 15, bold = true); Label("Belum ada: ${p.missing}", 12, Muted); Link("Lengkapi", { editing = p }) } }
    editing?.let { p -> ProductSheet(p, s.busy, { editing = null }) { vm.saveProduct(it) { editing = null } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ExplanationSheet(close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close, containerColor = Color.White) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("Apa itu batas pesan ulang?", 20, bold = true)
            Label("Jumlah stok minimum sebelum kamu perlu belanja lagi. Jika stok sudah sama atau di bawah angka ini, aplikasi menyarankan restock.", 14, Muted)
            Panel(color = Mint) { Label("Rata-rata terjual per hari", 13, DarkTeal, true); Label("Total penjualan 7 hari terakhir dibagi 7, termasuk hari tanpa penjualan.", 13) }
            Panel(color = Mint) { Label("Lama barang datang", 13, DarkTeal, true); Label("Berapa hari dari pesan sampai barang tiba (lead time).", 13) }
            Panel(color = Mint) { Label("Stok cadangan", 13, DarkTeal, true); Label("Jaga-jaga jika penjualan tiba-tiba naik (safety stock).", 13) }
            Panel(color = Mint) { Label("Batas pesan ulang = rata-rata × lead time + cadangan", 13, bold = true); Label("Contoh: 3 pcs/hari × 4 hari + 6 pcs = 18 pcs. Stok 4 pcs berarti perlu restock.", 13) }
            Action("Mengerti", onClick = close)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ProductSheet(product: Product?, busy: Boolean, close: () -> Unit, save: (Product) -> Unit) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var lead by remember { mutableStateOf(product?.leadTimeDays?.toString() ?: "") }
    var safety by remember { mutableStateOf(product?.safetyStock?.toString() ?: "") }
    val p = Product(product?.id ?: productId(name), name.trim(), stock.toIntOrNull(), lead.toIntOrNull(), safety.toIntOrNull(), product?.revision ?: 0)
    val valid = name.isNotBlank() && listOf(stock, lead, safety).all { it.isBlank() || it.toIntOrNull() != null } && runCatching { Inventory.validateProduct(p) }.isSuccess
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, containerColor = Color.White) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("Form Data Stok", 20, bold = true)
            Input("Nama produk", name, { name = it.take(120) }, enabled = product == null)
            Input("Stok tersedia saat ini (pcs)", stock, { stock = it }, true, enabled = !busy, error = numberError(stock, 0, 1000000000, optional = true))
            Input("Lama barang datang (hari)", lead, { lead = it }, true, enabled = !busy, error = numberError(lead, 0, 365, optional = true))
            Input("Stok cadangan (pcs)", safety, { safety = it }, true, enabled = !busy, error = numberError(safety, 0, 1000000000, optional = true))
            Label("Kosongkan data yang belum diketahui. Isi 0 jika nilainya memang nol. Stok tersedia adalah jumlah fisik saat ini.", 12, Muted)
            Action("Simpan Data", valid && !busy) { save(p) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PurchaseSheet(product: Product, suggested: Long?, busy: Boolean, close: () -> Unit,
    save: (Int, Long, LocalDate, String) -> Unit) {
    var qty by remember { mutableStateOf(suggested?.takeIf { it > 0 }?.toString() ?: "") }
    var price by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val item = ReceiptItem(product.name, qty.toIntOrNull() ?: 0, price.toLongOrNull() ?: -1, false)
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, containerColor = Color.White) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("Catat Pembelian", 20, bold = true); Label(product.name, 16, bold = true)
            Label("Catat hanya barang yang sudah diterima. Pembelian tidak dihitung sebagai penjualan.", 12, Muted)
            Input("Jumlah diterima (pcs)", qty, { qty = it }, true, enabled = !busy, error = numberError(qty, 1, 1000000, optional = false))
            Input("Harga beli per pcs (Rp)", price, { price = it }, true, enabled = !busy, error = numberError(price, 0, 1000000000, optional = false))
            DateField(date) { date = it }
            Panel(color = Mint) { KeyValue("Total", if (item.valid) rupiah(item.subtotal) else "—") }
            Action("Simpan Pembelian", item.valid && !busy) { save(item.quantity, item.unitPrice, date, id) }
        }
    }
}
