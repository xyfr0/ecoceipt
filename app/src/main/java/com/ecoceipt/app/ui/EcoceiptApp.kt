package com.ecoceipt.app.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ecoceipt.app.domain.*
import java.time.LocalDate

@Composable fun EcoceiptApp(vm: EcoceiptViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var logoutDialog by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDate() }
    LaunchedEffect(s.message) { s.message?.let { snackbar.showSnackbar(it); vm.dismissMessage() } }
    BackHandler(s.screen != Screen.LOGIN && s.screen != Screen.HOME) { vm.back() }
    val nav = s.screen in listOf(Screen.HOME, Screen.HISTORY, Screen.ANALYSIS, Screen.STOCK, Screen.RECOMMENDATIONS, Screen.ACCOUNT)
    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(), containerColor = Mint,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (nav) BottomNav(s.screen, vm::navigate) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (s.screen) {
                Screen.LOGIN -> LoginScreen(s, vm)
                Screen.SCAN -> CameraScreen(s.busy, s.openGallery, vm::scan, vm::back, vm::notify)
                else -> key(s.screen) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!nav) BackButton(vm::back)
                    s.dataError?.let { error -> Panel { Label(error, 12, Danger); Link("Coba lagi", vm::retryData) } }
                    when (s.screen) {
                        Screen.HOME -> HomeScreen(s, vm)
                        Screen.ACCOUNT -> AccountScreen(s, vm) { logoutDialog = true }
                        Screen.HISTORY -> HistoryScreen(s)
                        Screen.ANALYSIS -> AnalysisScreen(s, vm)
                        Screen.STOCK -> StockScreen(s, vm)
                        Screen.RECOMMENDATIONS -> RecommendationsScreen(s, vm)
                        Screen.PRODUCT -> ProductScreen(s, vm)
                        Screen.INCOMPLETE -> IncompleteScreen(s, vm)
                        Screen.PROCESSING -> ProcessingScreen(s, vm)
                        Screen.FAILURE -> FailureScreen(s, vm)
                        Screen.REVIEW -> ReviewScreen(s, vm)
                        else -> Unit
                    }
                    Spacer(Modifier.height(8.dp))
                } }
            }
            if (s.busy && s.screen != Screen.PROCESSING) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    if (logoutDialog) AlertDialog(onDismissRequest = { logoutDialog = false }, title = { Text("Keluar?") },
        text = { Text(if (s.guest) "Data tamu tetap tersimpan di perangkat ini." else "Data akun tetap tersimpan dan dapat dibuka setelah masuk kembali.") },
        confirmButton = { TextButton({ logoutDialog = false; vm.logout() }) { Text("Keluar") } },
        dismissButton = { TextButton({ logoutDialog = false }) { Text("Batal") } })
}

@Composable private fun LoginScreen(s: UiState, vm: EcoceiptViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(horizontal = 55.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(90.dp))
        AsyncImage("file:///android_asset/figma/8d105.png", "Ecoceipt", Modifier.size(180.dp))
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), placeholder = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFF3F6F6), focusedContainerColor = Color.White))
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), placeholder = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFF3F6F6), focusedContainerColor = Color.White))
        Spacer(Modifier.height(36.dp))
        Action("Sign In", !s.busy, secondary = true, framed = true) { vm.login(email, password) }
        Spacer(Modifier.height(8.dp))
        Action("Create Account", !s.busy, framed = true) { vm.login(email, password, true) }
        Link("Forget password?", { vm.resetPassword(email) }, DarkTeal)
        Spacer(Modifier.height(48.dp))
        Link("Continue as Guest", vm::guest, DarkTeal)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable private fun HomeScreen(s: UiState, vm: EcoceiptViewModel) {
    val summary = s.summaries[Period.TODAY] ?: SalesAnalysis.calculate(emptyList(), Period.TODAY, s.today)
    val recommendations = s.recommendations
    Panel(color = Teal) {
        Label("Pindai Nota", 16, Color.White, true)
        Label("Foto nota, data transaksi tersusun otomatis", 12, Color.White)
        Action("Mulai Pindai", secondary = true) { vm.navigate(Screen.SCAN) }
    }
    Label("RINGKASAN HARI INI")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.weight(1f)) { Label("Penjualan", 12, Muted); Label(rupiah(summary.revenue), 16, bold = true) }
        Panel(Modifier.weight(1f)) { Label("Transaksi", 12, Muted); Label(summary.transactions.toString(), 16, bold = true) }
    }
    Panel(Modifier.clickable { vm.navigate(Screen.RECOMMENDATIONS) }, Teal) {
        Label("REKOMENDASI STOK", color = Color.White)
        Label("${recommendations.count { it.needsRestock }} stok perlu segera restock", 12, Color.White, true)
    }
    Label("MENU")
    val menu = listOf(Triple("Riwayat Transaksi", "Lihat dan cari nota", Screen.HISTORY),
        Triple("Data Stok", "Stok dan lead time", Screen.STOCK), Triple("Analisis Penjualan", "Tren dan terlaris", Screen.ANALYSIS),
        Triple("Rekomendasi Stok", "Saran belanja ulang", Screen.RECOMMENDATIONS))
    menu.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (title, subtitle, route) ->
                Panel(Modifier.weight(1f).clickable { vm.navigate(route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage("file:///android_asset/figma/c7474.svg", null, Modifier.size(28.dp))
                        Column { Label(title, 12, bold = true); Label(subtitle, 11, Muted) }
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Label("TRANSAKSI TERBARU"); Link("Lihat semua", { vm.navigate(Screen.HISTORY) })
    }
    if (s.receipts.isEmpty()) Empty("Belum ada transaksi", "Pindai nota pertama untuk mulai mencatat penjualan.")
    s.receipts.take(3).forEach { ReceiptCard(it) }

}

@Composable private fun ProcessingScreen(s: UiState, vm: EcoceiptViewModel) {
    Panel { AsyncImage(s.image, "Foto nota", Modifier.fillMaxWidth().height(180.dp).background(LightGray, Round)) }
    Panel {
        listOf("Teks nota dibaca (OCR)", "Menyusun data transaksi (AI)", "Memeriksa kelengkapan data").forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (index == s.scanStep) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Box(Modifier.size(24.dp).background(if (index < s.scanStep) Teal else LightGray, CircleShape), contentAlignment = Alignment.Center) {
                    if (index < s.scanStep) Label("âœ“", 13, Color.White, true)
                }
                Column { Label(label, bold = true); Label(if (index < s.scanStep) "Selesai" else if (index == s.scanStep) "Memprosesâ€¦" else "Menunggu", 12, Muted) }
            }
        }
    }
    Action("Batalkan", secondary = true, onClick = vm::cancelScan)
}

@Composable private fun FailureScreen(s: UiState, vm: EcoceiptViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(96.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Label("!", 44, DarkTeal, true) }
        Label(if (s.rawText.isBlank()) "Teks nota tidak terbaca" else "Penataan nota belum berhasil", 20, bold = true)
        Label("Coba ambil foto lagi, atau isi transaksi secara manual.", 14, Muted)
    }
    if (s.rawText.isNotBlank()) Panel { Label("Teks hasil OCR", bold = true); Label(s.rawText, 12, Muted) }
    Panel {
        Label("Tips agar nota terbaca", 13, bold = true)
        Label("â€¢ Foto di tempat yang terang\nâ€¢ Pastikan nota rata dan tidak terlipat\nâ€¢ Hindari bayangan dan gambar buram", 12, Muted)
    }
    Action("Ambil Foto Ulang") { vm.navigate(Screen.SCAN) }
    Action("Pilih dari Galeri", secondary = true, onClick = vm::chooseGallery)
    Action("Isi Manual", secondary = true, onClick = vm::manual)
}

@Composable private fun ReviewScreen(s: UiState, vm: EcoceiptViewModel) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showRaw by rememberSaveable { mutableStateOf(false) }
    Panel {
        Label("Periksa hasil pembacaan AI sebelum disimpan", 12, bold = true)
        Label("${s.draft.items.count { it.needsReview || !it.valid }} item perlu diperiksa", 11, Danger)
        if (s.rawText.isNotBlank()) Link(if (showRaw) "Sembunyikan teks nota" else "Lihat teks nota", { showRaw = !showRaw })
        if (showRaw) Label(s.rawText, 12, Muted)
    }
    Label("Jenis nota", 12, Muted)
    Filters(listOf("Nota Penjualan", "Nota Pembelian"), if (s.draft.type == ReceiptType.SALE) "Nota Penjualan" else "Nota Pembelian") {
        vm.setDraft(s.draft.copy(type = if (it == "Nota Penjualan") ReceiptType.SALE else ReceiptType.PURCHASE))
    }
    DateField(s.draft.date) { vm.setDraft(s.draft.copy(date = it)) }
    s.draft.items.forEachIndexed { index, item ->
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(item.name.ifBlank { "Nama belum terbaca" }, bold = true, modifier = Modifier.weight(1f))
                Link("Ubah", { editing = index; showEditor = true })
                Link("Hapus", { vm.removeItem(index) }, Danger)
            }
            if (item.needsReview || !item.valid) Badge("Periksa", true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label("Jumlah ${item.quantity}", 12, Muted)
                Label("Harga ${if (item.unitPrice < 0) "â€”" else rupiah(item.unitPrice)}", 12, Muted)
                Label(if (item.valid) rupiah(item.subtotal) else "Belum valid", 12, bold = true)
            }
        }
    }
    Action("+ Tambah Item", s.draft.items.size < 100, true) { editing = null; showEditor = true }
    Panel { KeyValue("Total", rupiah(s.draft.items.filter { it.valid }.sumOf { it.subtotal }), DarkTeal) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(s.draft.items.isNotEmpty() && s.draft.items.all { !it.needsReview && it.valid }, { checked ->
            if (checked) vm.confirmItems() else vm.setDraft(s.draft.copy(items = s.draft.items.map { it.copy(needsReview = true) }))
        })
        Label("Saya sudah memeriksa nama, jumlah, harga, jenis, dan tanggal nota.", 12)
    }
    Action("Simpan Transaksi", !s.busy && s.draft.items.isNotEmpty() && s.draft.items.all { it.valid && !it.needsReview }, onClick = vm::saveReceipt)
    Action("Pindai Ulang", secondary = true) { vm.navigate(Screen.SCAN) }
    if (showEditor) ItemSheet(editing?.let { s.draft.items.getOrNull(it) } ?: ReceiptItem(),
        onDismiss = { showEditor = false }, onSave = { vm.updateItem(editing, it); showEditor = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ItemSheet(item: ReceiptItem, onDismiss: () -> Unit, onSave: (ReceiptItem) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.quantity.takeIf { it > 0 }?.toString() ?: "") }
    var price by remember { mutableStateOf(item.unitPrice.takeIf { it >= 0 }?.toString() ?: "") }
    val value = ReceiptItem(name.trim(), qty.toIntOrNull() ?: 0, price.toLongOrNull() ?: -1, false)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("Ubah Item", 20, bold = true)
            Input("Nama barang", name, { name = it.take(120) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Input("Jumlah", qty, { qty = it }, true) }
                Box(Modifier.weight(1f)) { Input("Harga satuan (Rp)", price, { price = it }, true) }
            }
            Panel(color = Mint) { KeyValue("Subtotal (otomatis)", if (value.valid) rupiah(value.subtotal) else "â€”", DarkTeal) }
            Action("Simpan Perubahan", value.valid) { onSave(value) }
        }
    }
}

@Composable fun Input(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(label, 12, Muted)
        OutlinedTextField(value, { text -> if (!numeric || text.all { it.isDigit() }) onChange(text) },
            Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, shape = Round,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text))
    }
}
@Composable fun DateField(date: LocalDate, onChange: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Label("Tanggal nota: $date", 12, Muted)
        Link("Ubah tanggal", {
            DatePickerDialog(context, { _, year, month, day -> onChange(LocalDate.of(year, month + 1, day)) },
                date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        })
    }
}
