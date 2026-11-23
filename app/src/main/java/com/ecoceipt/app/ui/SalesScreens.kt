package com.ecoceipt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecoceipt.app.domain.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun ReceiptCard(receipt: Receipt, onClick: (() -> Unit)? = null) {
    Panel(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label("${if (receipt.type == ReceiptType.SALE) "Penjualan" else "Pembelian"} · ${receipt.items.size} item", 13, bold = true)
                Label(receipt.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))), 11, Muted)
            }
            Label(rupiah(receipt.total), 13, DarkTeal, true)
        }
    }
}

@Composable fun HistoryScreen(s: UiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var period by rememberSaveable { mutableStateOf(Period.ALL) }
    var selected by remember { mutableStateOf<Receipt?>(null) }
    Input("Cari nama barang", query, { query = it })
    Filters(Period.entries.map { it.label }, period.label) { label -> period = Period.entries.first { it.label == label } }
    Filters(listOf("Semua", "Penjualan", "Pembelian"), filter) { filter = it }
    val start = if (period == Period.ALL) java.time.LocalDate.MIN else SalesAnalysis.start(period, s.today, s.receipts)
    val receipts = s.receipts.filter { r ->
        !r.date.isBefore(start) && !r.date.isAfter(s.today) &&
        (filter == "Semua" || (filter == "Penjualan") == (r.type == ReceiptType.SALE)) &&
        (query.isBlank() || r.items.any { it.name.contains(query, ignoreCase = true) })
    }
    Label("${receipts.size} transaksi", 12, Muted)
    if (receipts.isEmpty()) Empty("Transaksi tidak ditemukan", "Nota yang sudah disimpan akan muncul di sini. Coba ubah pencarian atau periode.")
    receipts.forEach { receipt -> ReceiptCard(receipt) { selected = receipt } }
    selected?.let { receipt ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Detail Transaksi") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("${receipt.date} · ${if (receipt.type == ReceiptType.SALE) "Penjualan" else "Pembelian"}", 12, Muted)
                receipt.items.forEach { Label("${it.name}\n${it.quantity} × ${rupiah(it.unitPrice)} = ${rupiah(it.subtotal)}", 13) }
                HorizontalDivider(); KeyValue("Total", rupiah(receipt.total), DarkTeal)
            }
        }, confirmButton = { TextButton({ selected = null }) { Text("Tutup") } })
    }
}

@Composable fun AnalysisScreen(s: UiState, vm: EcoceiptViewModel) {
    var period by rememberSaveable { mutableStateOf(Period.ALL) }
    val analysis = s.summaries[period] ?: SalesAnalysis.calculate(emptyList(), period, s.today)
    val count = s.recommendations.count { it.needsRestock }
    Filters(Period.entries.map { it.label }, period.label) { label -> period = Period.entries.first { it.label == label } }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Panel(Modifier.weight(1f).heightIn(min = 143.dp)) {
            AsyncImage("file:///android_asset/figma/53454.png", null, Modifier.size(40.dp))
            Label("Total Penjualan", 10, Muted); Label(rupiah(analysis.revenue), 20)
            Change(analysis.revenueChange)
        }
        Panel(Modifier.weight(1f).heightIn(min = 143.dp)) {
            AsyncImage("file:///android_asset/figma/7d5c6.png", null, Modifier.size(40.dp))
            Label("Total Transaksi", 10, Muted); Label(analysis.transactions.toString(), 20)
            Change(analysis.transactionChange)
        }
    }
    Panel {
        Label("TREN PENJUALAN")
        if (analysis.transactions == 0) Label("Belum ada penjualan pada periode ini.", 12, Muted)
        Bars(analysis.trend.map { it.revenue }, analysis.trend.map { it.date.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("id-ID"))) })
        Label("Nilai penjualan per hari (Rp) · paling banyak 7 hari terakhir", 10, Muted)
        Label("Rata-rata ${decimal(analysis.averageDailyQuantity)} pcs/hari", 12, Muted)
        analysis.quantityChange?.let { Label("Jumlah terjual ${if (it >= 0) "+" else ""}${decimal(it)}% dari periode sebelumnya", 11, Muted) }
    }
    Panel {
        Label("PRODUK TERLARIS")
        val quantity = analysis.bestSellers.sumOf { it.quantity }.coerceAtLeast(1)
        if (analysis.bestSellers.isEmpty()) Label("Belum ada produk terjual.", 12, Muted)
        analysis.bestSellers.take(5).forEachIndexed { index, product ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label("${index + 1}", 12, Teal, true, Modifier.width(24.dp))
                Column(Modifier.weight(1f)) { Label(product.name, 12, bold = true); Label("${product.quantity} terjual", 10, Muted) }
                Label("${decimal(product.quantity * 100.0 / quantity)}%", 11, Teal)
            }
        }
    }
    Panel(Modifier.clickable { vm.navigate(Screen.RECOMMENDATIONS) }, Teal) {
        Label("REKOMENDASI STOK", color = Color.White)
        Label("$count stok perlu segera restock", 12, Color.White, true)
    }
}

@Composable private fun Change(value: Double?) {
    Label(if (value == null) "Belum ada pembanding" else "${if (value >= 0) "↑" else "↓"} ${decimal(kotlin.math.abs(value))}% dari periode lalu", 10,
        if (value != null && value < 0) Danger else Teal)
}

@Composable fun Bars(values: List<Long>, labels: List<String> = emptyList()) {
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1L
    Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { index, value ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.fillMaxWidth().height((value.toFloat() / max * 82).coerceAtLeast(1f).dp)
                    .background(Teal, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                if (labels.isNotEmpty()) Label(labels[index], 9, Muted)
            }
        }
    }
}
