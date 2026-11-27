package com.ecoceipt.app.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ecoceipt.app.BuildConfig
import com.ecoceipt.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun EcoceiptApp(vm: EcoceiptViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var logoutDialog by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDate() }
    LaunchedEffect(s.message) { s.message?.let { snackbar.showSnackbar(it); vm.dismissMessage() } }
    BackHandler(s.screen != Screen.LOGIN && s.screen != Screen.HOME) { vm.back() }
    val nav = s.screen in listOf(Screen.HOME, Screen.HISTORY, Screen.ANALYSIS, Screen.STOCK, Screen.RECOMMENDATIONS, Screen.ACCOUNT)
    val subpage = s.screen in listOf(Screen.ANALYSIS, Screen.RECOMMENDATIONS)
    Scaffold(
        modifier = Modifier.fillMaxSize().background(if (s.screen == Screen.SCAN) Ink else Mint)
            .systemBarsPadding().imePadding(),
        containerColor = if (s.screen == Screen.SCAN) Ink else Mint,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (nav) BottomNav(s.screen, vm::navigate) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (s.screen) {
                Screen.LOGIN -> LoginScreen(s, vm)
                Screen.REGISTER -> RegisterScreen(s, vm)
                Screen.SCAN -> CameraScreen(s.busy, s.openGallery, vm::scanBatch, vm::back, vm::notify)
                else -> key(s.screen) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!nav || subpage) BackButton(vm::back)
                    s.dataError?.let { error -> Panel { Label(error, 12, Danger); Link("Coba lagi", vm::retryData) } }
                    when (s.screen) {
                        Screen.HOME -> HomeScreen(s, vm)
                        Screen.ACCOUNT -> AccountScreen(s, vm) { logoutDialog = true }
                        Screen.HISTORY -> HistoryScreen(s, vm)
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
        text = { Text(when {
            s.demo -> "Data akun uji tetap tersimpan di perangkat ini."
            s.guest -> "Data tamu tetap tersimpan di perangkat ini."
            else -> "Data akun tetap tersimpan dan dapat dibuka setelah masuk kembali."
        }) },
        confirmButton = { TextButton({ logoutDialog = false; vm.logout() }) { Text("Keluar") } },
        dismissButton = { TextButton({ logoutDialog = false }) { Text("Batal") } })
    if (s.pendingGuestImport) AlertDialog(onDismissRequest = vm::dismissGuestImport, title = { Text("Impor Data Tamu?") },
        text = { Text("Ditemukan data transaksi dan produk dari mode tamu di perangkat ini. Impor ke akun Anda sekarang?") },
        confirmButton = { TextButton({ vm.importGuestData() }) { Text("Impor") } },
        dismissButton = { TextButton(vm::dismissGuestImport) { Text("Nanti") } })
}

@Composable private fun LoginScreen(s: UiState, vm: EcoceiptViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val emailError = when {
        email.isBlank() -> if (BuildConfig.DEBUG) "Email atau username wajib diisi." else "Email wajib diisi."
        BuildConfig.DEBUG && email.trim() == "test" -> null
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Masukkan email yang valid."
        else -> null
    }
    val passwordError = if (password.isBlank()) "Password wajib diisi." else null
    AuthLayout("Sign In", "Masuk untuk mengelola nota dan stok usahamu.") {
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(if (BuildConfig.DEBUG) "Email atau username" else "Email") },
            placeholder = { Text(if (BuildConfig.DEBUG) "nama@email.com atau test" else "nama@email.com") },
            singleLine = true, enabled = !s.busy, shape = Round,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = submitted && emailError != null,
            supportingText = { if (submitted && emailError != null) Text(emailError) })
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, enabled = !s.busy, shape = Round,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = submitted && passwordError != null,
            supportingText = { if (submitted && passwordError != null) Text(passwordError) })
        Action("Sign In", enabled = !s.busy, framed = true) {
            submitted = true
            if (emailError == null && passwordError == null) vm.login(email.trim(), password)
        }
        Action("Create Account", enabled = !s.busy, secondary = true, framed = true) { vm.navigate(Screen.REGISTER) }
        if (!(BuildConfig.DEBUG && email.trim() == "test")) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Link("Forget password?", { vm.resetPassword(email) }, DarkTeal)
            }
        }
        if (BuildConfig.DEBUG) Label("Akun uji: test / test", 12, Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable private fun HomeScreen(s: UiState, vm: EcoceiptViewModel) {
    val summary = s.summaries[Period.TODAY] ?: SalesAnalysis.calculate(emptyList(), Period.TODAY, s.today)
    val weekSummary = s.summaries[Period.WEEK] ?: SalesAnalysis.calculate(emptyList(), Period.WEEK, s.today)
    val recommendations = s.recommendations
    Label("Ringkasan hari ini", 17, bold = true)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.weight(1f).heightIn(min = 86.dp)) {
            Label("Penjualan", 14); Label(rupiah(summary.revenue), 20, bold = true)
        }
        Panel(Modifier.weight(1f).heightIn(min = 86.dp)) {
            Label("Transaksi", 14); Label(summary.transactions.toString(), 20, bold = true)
        }
    }
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Label("Tren penjualan 7 hari", bold = true)
            Link("Detail", { vm.navigate(Screen.ANALYSIS) })
        }
        if (weekSummary.transactions == 0) Label("Belum ada penjualan minggu ini.", 12, Muted)
        Bars(
            weekSummary.trend.map { it.revenue },
            weekSummary.trend.map { it.date.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("id-ID"))) }
        )
        Label("Nilai penjualan per hari (Rp)", 10, Muted)
    }
    val restockCount = recommendations.count { it.needsRestock }
    val nearCount = recommendations.count { it.nearRestock }
    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp)
        .background(Color(0xFFC6E8E1), Round).clickable { vm.navigate(Screen.RECOMMENDATIONS) }
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("REKOMENDASI STOK", 15, DarkTeal, true)
            Label(
                when {
                    restockCount > 0 -> "$restockCount stok perlu segera restock"
                    nearCount > 0 -> "$nearCount stok mendekati batas pesan ulang"
                    else -> "Semua stok dalam kondisi aman"
                },
                13
            )
        }
        StockStatusIcon(hasDanger = restockCount > 0, hasWarning = nearCount > 0)
    }
    Label("Menu", 17, bold = true)
    val menu = listOf(Triple("Riwayat Transaksi", "Lihat dan cari nota", Screen.HISTORY),
        Triple("Data Stok", "Stok dan lead time", Screen.STOCK), Triple("Analisis Penjualan", "Tren dan terlaris", Screen.ANALYSIS),
        Triple("Rekomendasi Stok", "Saran belanja ulang", Screen.RECOMMENDATIONS))
    menu.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (title, subtitle, route) ->
                Panel(Modifier.weight(1f).heightIn(min = 82.dp).clickable { vm.navigate(route) }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(40.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                            val icon = when (route) {
                                Screen.HISTORY -> com.ecoceipt.app.R.drawable.ic_menu_receipt
                                Screen.STOCK -> com.ecoceipt.app.R.drawable.ic_menu_stock
                                Screen.ANALYSIS -> com.ecoceipt.app.R.drawable.ic_menu_analysis
                                else -> com.ecoceipt.app.R.drawable.ic_menu_recommendation
                            }
                            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp), tint = DarkTeal)
                        }
                        Column(Modifier.weight(1f)) { Label(title, 13, bold = true); Label(subtitle, 11, Muted) }
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Label("Transaksi terbaru", 17, bold = true); Link("Lihat semua", { vm.navigate(Screen.HISTORY) })
    }
    if (s.receipts.isEmpty()) Empty("Belum ada transaksi", "Pindai nota pertama untuk mulai mencatat penjualan.", com.ecoceipt.app.R.drawable.ic_history)
    s.receipts.take(3).forEach { ReceiptCard(it) }

}

@Composable private fun StockStatusIcon(hasDanger: Boolean, hasWarning: Boolean) {
    val circleColor = when {
        hasDanger -> Color(0xFFFDE8E6)
        hasWarning -> Color(0xFFFFF4DC)
        else -> Color(0xFFE6F7EC)
    }
    val badgeColor = when {
        hasDanger -> Danger
        hasWarning -> Warning
        else -> SafeGreen
    }
    Box(Modifier.size(50.dp)) {
        Box(
            Modifier.size(44.dp).align(Alignment.BottomEnd).background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(com.ecoceipt.app.R.drawable.ic_menu_recommendation),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Ink
            )
        }
        Canvas(Modifier.size(20.dp).align(Alignment.TopStart)) {
            val w = size.width
            val h = size.height
            if (hasDanger || hasWarning) {
                val triangle = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.94f, h * 0.88f)
                    lineTo(w * 0.06f, h * 0.88f)
                    close()
                }
                drawPath(triangle, color = badgeColor)
                drawPath(triangle, color = badgeColor, style = Stroke(width = w * 0.14f, join = StrokeJoin.Round))
                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.36f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.62f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
                drawCircle(Color.White, radius = w * 0.06f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.77f))
            } else {
                drawCircle(color = badgeColor, radius = w * 0.45f)
                val check = Path().apply {
                    moveTo(w * 0.28f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.72f, h * 0.36f)
                }
                drawPath(check, color = Color.White, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable private fun ProcessingScreen(s: UiState, vm: EcoceiptViewModel) {
    s.batchProgress?.let { (current, total) ->
        Panel(color = Teal) {
            Label("Memproses nota $current dari $total", 14, Color.White, true)
        }
    }
    Panel { AsyncImage(s.image, "Foto nota", Modifier.fillMaxWidth().height(180.dp).background(LightGray, Round)) }
    Panel {
        listOf("Teks nota dibaca (OCR)", "Menyusun data transaksi (AI)", "Memeriksa kelengkapan data").forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (index == s.scanStep) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Box(Modifier.size(24.dp).background(if (index < s.scanStep) Teal else LightGray, CircleShape), contentAlignment = Alignment.Center) {
                    if (index < s.scanStep) Label("\u2713", 13, Color.White, true)
                }
                Column { Label(label, bold = true); Label(if (index < s.scanStep) "Selesai" else if (index == s.scanStep) "Memproses\u2026" else "Menunggu", 12, Muted) }
            }
        }
    }
    Action("Batalkan", secondary = true, onClick = vm::cancelScan)
}

@Composable private fun FailureScreen(s: UiState, vm: EcoceiptViewModel) {
    s.scanError?.let { Panel { Label(it, 13, Danger) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(96.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Label("!", 44, DarkTeal, true) }
        Label(if (s.rawText.isBlank()) "Teks nota tidak terbaca" else "Penataan nota belum berhasil", 20, bold = true)
        Label("Coba ambil foto lagi, atau isi transaksi secara manual.", 14, Muted)
    }
    if (s.rawText.isNotBlank()) Panel { Label("Teks hasil OCR", bold = true); Label(s.rawText, 12, Muted) }
    Panel {
        Label("Tips agar nota terbaca", 13, bold = true)
        Label("\u2022 Foto di tempat yang terang\n\u2022 Pastikan nota rata dan tidak terlipat\n\u2022 Hindari bayangan dan gambar buram", 12, Muted)
    }
    Action("Ambil Foto Ulang") { vm.navigate(Screen.SCAN) }
    Action("Pilih dari Galeri", secondary = true, onClick = vm::chooseGallery)
    Action("Isi Manual", secondary = true, onClick = vm::manual)
}

@Composable private fun ReviewScreen(s: UiState, vm: EcoceiptViewModel) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showRaw by rememberSaveable { mutableStateOf(false) }
    if (s.drafts.size > 1) {
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Label("Batch ${s.drafts.size} nota", 13, bold = true)
                Link("Hapus nota ini", { vm.removeDraft(s.activeDraftIndex) }, Danger)
            }
            Filters(
                s.drafts.mapIndexed { i, entry ->
                    val ok = entry.receipt.items.isNotEmpty() && entry.receipt.items.all { it.valid && !it.needsReview }
                    "Nota ${i + 1}${if (ok) " ✓" else ""}"
                },
                "Nota ${s.activeDraftIndex + 1}${if (s.draft.items.isNotEmpty() && s.draft.items.all { it.valid && !it.needsReview }) " ✓" else ""}"
            ) { label ->
                val idx = label.removePrefix("Nota ").substringBefore(" ").toIntOrNull()?.minus(1)
                if (idx != null) vm.selectDraft(idx)
            }
        }
    }
    s.scanError?.let { Panel { Label(it, 12, Danger) } }
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
    Panel {
        Label("Rincian barang", 16, bold = true)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("Barang", 11, Muted, true, Modifier.weight(1f))
            Label("Qty", 11, Muted, true, Modifier.width(38.dp))
            Label("Harga", 11, Muted, true, Modifier.width(78.dp))
            Label("Subtotal", 11, Muted, true, Modifier.width(88.dp))
        }
        HorizontalDivider(color = LightGray)
        if (s.draft.items.isEmpty()) Label("Belum ada barang. Tambahkan item untuk melanjutkan.", 12, Muted)
        s.draft.items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Label(item.name.ifBlank { "Belum terbaca" }, 11,
                    if (item.name.isBlank()) Danger else Ink, true, Modifier.weight(1f))
                Label(item.quantity.takeIf { it > 0 }?.toString() ?: "—", 11, Ink,
                    modifier = Modifier.width(38.dp))
                Label(if (item.unitPrice >= 0) rupiah(item.unitPrice) else "—", 10, Ink,
                    modifier = Modifier.width(78.dp))
                Label(if (item.valid) rupiah(item.subtotal) else "—", 10, DarkTeal, true,
                    Modifier.width(88.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (item.needsReview || !item.valid) Badge("Periksa", true)
                Spacer(Modifier.weight(1f))
                Link("Ubah", { editing = index; showEditor = true })
                Link("Hapus", { vm.removeItem(index) }, Danger)
            }
            HorizontalDivider(color = LightGray)
        }
        KeyValue("Total", rupiah(s.draft.items.filter { it.valid }.sumOf { it.subtotal }), DarkTeal)
    }
    Action("+ Tambah Item", s.draft.items.size < 100, true) { editing = null; showEditor = true }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(s.draft.items.isNotEmpty() && s.draft.items.all { !it.needsReview && it.valid }, { checked ->
            if (checked) vm.confirmItems() else vm.setDraft(s.draft.copy(items = s.draft.items.map { it.copy(needsReview = true) }))
        })
        Label("Saya sudah memeriksa nama, jumlah, harga, jenis, dan tanggal nota.", 12)
    }
    val allBatchReady = s.drafts.size > 1 && s.drafts.all { it.receipt.items.isNotEmpty() && it.receipt.items.all { item -> item.valid && !item.needsReview } }
    if (s.drafts.size > 1) {
        Action("Simpan Semua (${s.drafts.size} Nota)", !s.busy && allBatchReady, onClick = vm::saveAllReceipts)
        Action("Simpan Nota Ini Saja", !s.busy && s.draft.items.isNotEmpty() && s.draft.items.all { it.valid && !it.needsReview }, secondary = true, onClick = vm::saveReceipt)
    } else {
        Action("Simpan Transaksi", !s.busy && s.draft.items.isNotEmpty() && s.draft.items.all { it.valid && !it.needsReview }, onClick = vm::saveReceipt)
    }
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
            Input("Nama barang", name, { name = it }, error = if (name.isBlank() || name.length > 120) "Isi nama barang (maksimal 120 karakter)." else null)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Input("Jumlah", qty, { qty = it }, true, error = numberError(qty, 1, 1_000_000)) }
                Box(Modifier.weight(1f)) { Input("Harga satuan (Rp)", price, { price = it }, true, error = numberError(price, 0, 1_000_000_000)) }
            }
            Panel(color = Mint) { KeyValue("Subtotal (otomatis)", if (value.valid) rupiah(value.subtotal) else "\u2014", DarkTeal) }
            Action("Simpan Perubahan", value.valid) { onSave(value) }
        }
    }
}

@Composable fun Input(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, enabled: Boolean = true, error: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(label, 12, Muted)
        OutlinedTextField(value, { text -> if (!numeric || text.all { it.isDigit() }) onChange(text) },
            Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, shape = Round,
            isError = error != null, supportingText = error?.let { message -> { Text(message) } },
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
