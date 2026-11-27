package com.ecoceipt.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ecoceipt.app.BuildConfig
import com.ecoceipt.app.data.*
import com.ecoceipt.app.domain.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class Screen { LOGIN, REGISTER, HOME, SCAN, PROCESSING, FAILURE, REVIEW, HISTORY, ANALYSIS, STOCK, RECOMMENDATIONS, PRODUCT, INCOMPLETE, ACCOUNT }
data class DraftEntry(
    val receipt: Receipt,
    val image: String? = null,
    val rawText: String = "",
    val error: String? = null
)
data class UiState(
    val screen: Screen = Screen.LOGIN, val backStack: List<Screen> = emptyList(),
    val products: List<Product> = emptyList(),
    val receipts: List<Receipt> = emptyList(), val busy: Boolean = false, val message: String? = null,
    val dataError: String? = null, val image: String? = null, val rawText: String = "", val scanStep: Int = 0,
    val scanError: String? = null,
    val draft: Receipt = Receipt(), val drafts: List<DraftEntry> = emptyList(), val activeDraftIndex: Int = 0,
    val batchProgress: Pair<Int, Int>? = null, val selectedProduct: String? = null,
    val guest: Boolean = false, val demo: Boolean = false, val user: String = "", val today: LocalDate = LocalDate.now(),
    val openGallery: Boolean = false, val pendingGuestImport: Boolean = false,
    val summaries: Map<Period, SalesSummary> = emptyMap(),
    val recommendations: List<StockRecommendation> = emptyList()
)

class EcoceiptViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private var repository: EcoceiptRepository? = null
    private var observer: Job? = null
    private var scanJob: Job? = null
    private val scanner = ReceiptScanner(application)
    val cloudConfigured get() = FirebaseApp.getApps(getApplication()).isNotEmpty()
    private val auth get() = FirebaseAuth.getInstance()

    init {
        val draft = saved.get<String>("draft")?.let { runCatching { ReceiptJson.receipt(JSONObject(it)) }.getOrNull() }
        if (draft != null) _state.update { it.copy(draft = draft, image = saved["image"], rawText = saved["rawText"] ?: "") }
        if (BuildConfig.DEBUG && saved.get<Boolean>("demo") == true) loginDemo()
        else if (saved.get<Boolean>("guest") == true) guest()
        else if (cloudConfigured && auth.currentUser != null) connectCloud()
    }
    private fun observe(repo: EcoceiptRepository, guest: Boolean, user: String, resetScreen: Boolean = true,
        pendingImport: Boolean = false, demo: Boolean = false) {
        observer?.cancel()
        repository = repo
        _state.update { it.copy(screen = if (!resetScreen) it.screen else if (it.draft.id.isNotBlank()) Screen.REVIEW else Screen.HOME,
            backStack = if (resetScreen) emptyList() else it.backStack,
            products = if (resetScreen) emptyList() else it.products,
            receipts = if (resetScreen) emptyList() else it.receipts,
            summaries = if (resetScreen) emptyMap() else it.summaries,
            recommendations = if (resetScreen) emptyList() else it.recommendations,
            guest = guest, demo = demo, user = user, dataError = null, pendingGuestImport = pendingImport) }
        observer = viewModelScope.launch {
            combine(repo.products, repo.receipts, state.map { it.today }.distinctUntilChanged()) { p, r, today ->
                withContext(Dispatchers.Default) {
                    val summaries = Period.entries.associateWith { SalesAnalysis.calculate(r, it, today) }
                    val recommendations = p.map { StockRecommender.calculate(it, r, today) }
                    UiState(products = p.sortedBy { it.name },
                        receipts = r.sortedWith(compareByDescending<Receipt> { it.date }.thenByDescending { it.createdAt }),
                        summaries = summaries, recommendations = recommendations)
                }
            }
                .catch { e -> _state.update { it.copy(dataError = e.message ?: "Data gagal dimuat. Coba lagi.") } }
                .collect { computed -> _state.update { it.copy(products = computed.products, receipts = computed.receipts,
                    summaries = computed.summaries, recommendations = computed.recommendations) } }
        }
    }
    fun guest() = action {
        val repo = LocalRepository(getApplication())
        repo.load()
        saved["guest"] = true
        saved["demo"] = false
        observe(repo, true, "Tamu")
    }
    private suspend fun connectDemo() {
        val repo = LocalRepository(getApplication(), "test-records.json")
        repo.load()
        if (cloudConfigured) auth.signOut()
        saved["guest"] = false
        saved["demo"] = true
        observe(repo, false, "test", demo = true)
    }
    private fun loginDemo() = action { connectDemo() }
    private fun connectCloud() {
        val user = auth.currentUser ?: return
        saved["guest"] = false
        saved["demo"] = false
        val hasGuestData = LocalRepository(getApplication()).hasData()
        observe(FirestoreRepository(FirebaseFirestore.getInstance(), user.uid), false, user.email ?: "Pengguna", pendingImport = hasGuestData)
    }
    fun dismissGuestImport() { _state.update { it.copy(pendingGuestImport = false) } }
    fun importGuestData() = action {
        val cloud = repository ?: return@action
        val local = LocalRepository(getApplication())
        local.load()
        local.products.value.forEach { product ->
            if (state.value.products.none { it.id == product.id }) {
                cloud.saveProduct(product.copy(revision = 0))
            }
        }
        local.receipts.value.forEach { receipt -> cloud.saveReceipt(receipt) }
        local.clear()
        _state.update { it.copy(pendingGuestImport = false, message = "Data tamu berhasil diimpor ke akun.") }
    }
    fun login(email: String, password: String, register: Boolean = false) = action {
        if (BuildConfig.DEBUG && !register && email.trim() == "test") {
            require(password == "test") { "Username atau password akun uji tidak sesuai." }
            connectDemo()
            return@action
        }
        check(cloudConfigured) { "Layanan akun belum tersedia. Periksa konfigurasi Firebase." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Masukkan alamat email yang valid." }
        require(password.isNotBlank()) { "Password wajib diisi." }
        require(!register || password.length >= 6) { "Password akun baru minimal 6 karakter." }
        if (register) auth.createUserWithEmailAndPassword(email.trim(), password).await()
        else auth.signInWithEmailAndPassword(email.trim(), password).await()
        connectCloud()
    }
    fun resetPassword(email: String) = action {
        check(cloudConfigured) { "Layanan akun belum tersedia." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Masukkan alamat email yang valid terlebih dahulu." }
        auth.sendPasswordResetEmail(email.trim()).await()
        notify("Jika akun tersedia, tautan pemulihan dikirim ke email tersebut.")
    }
    fun logout() {
        if (state.value.busy) return
        observer?.cancel(); repository = null
        if (cloudConfigured) auth.signOut()
        saved["guest"] = false
        saved["demo"] = false
        clearDraft()
        _state.value = UiState()
    }
    fun refreshDate() { _state.update { it.copy(today = LocalDate.now()) } }
    fun navigate(screen: Screen) {
        if (state.value.busy) return
        _state.update { s ->
            if (s.screen == screen) s.copy(openGallery = false)
            else {
                val nextStack = when {
                    screen == Screen.HOME || screen == Screen.LOGIN -> emptyList()
                    s.screen in listOf(Screen.LOGIN, Screen.REGISTER, Screen.PROCESSING, Screen.FAILURE) -> s.backStack
                    else -> (s.backStack + s.screen).takeLast(12)
                }
                s.copy(screen = screen, backStack = nextStack, openGallery = false)
            }
        }
    }
    fun chooseGallery() {
        if (state.value.busy) return
        navigate(Screen.SCAN)
        _state.update { it.copy(openGallery = true) }
    }
    fun selectProduct(id: String) {
        if (state.value.busy) return
        _state.update { s ->
            val nextStack = if (s.screen == Screen.PRODUCT) s.backStack else (s.backStack + s.screen).takeLast(12)
            s.copy(selectedProduct = id, screen = Screen.PRODUCT, backStack = nextStack)
        }
    }
    fun notify(message: String) { _state.update { it.copy(message = message) } }
    fun dismissMessage() { _state.update { it.copy(message = null) } }
    fun retryData() {
        val repo = repository ?: return
        observe(repo, state.value.guest, state.value.user, resetScreen = false, demo = state.value.demo)
    }
    fun back() {
        if (state.value.screen == Screen.PROCESSING) { cancelScan(); return }
        if (state.value.busy) return
        _state.update { s ->
            if (s.screen == Screen.REGISTER) s.copy(screen = Screen.LOGIN, backStack = emptyList(), openGallery = false)
            else {
                val target = s.backStack.lastOrNull() ?: Screen.HOME
                s.copy(screen = target, backStack = s.backStack.dropLast(1), openGallery = false)
            }
        }
    }
    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notify(userMessage(e)) }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    private fun userMessage(error: Exception): String = when (error) {
        is com.google.firebase.FirebaseNetworkException -> "Tidak dapat terhubung. Periksa koneksi internet dan coba lagi."
        is com.google.firebase.FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
        is com.google.firebase.auth.FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Masukkan alamat email yang valid."
            "ERROR_WEAK_PASSWORD" -> "Password terlalu lemah. Gunakan minimal 6 karakter."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar. Silakan masuk atau gunakan lupa password."
            "ERROR_USER_DISABLED" -> "Akun ini dinonaktifkan."
            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> "Email atau password tidak sesuai. Periksa kembali atau gunakan lupa password."
            else -> "Layanan akun belum dapat memproses permintaan. Coba lagi nanti."
        }
        else -> error.message ?: "Proses gagal. Coba lagi."
    }
    fun scan(uri: Uri) = scanBatch(listOf(uri))

    fun scanBatch(uris: List<Uri>) {
        if (state.value.busy || uris.isEmpty()) return
        scanJob?.cancel()
        clearDraft()
        scanJob = viewModelScope.launch {
            val entries = mutableListOf<DraftEntry>()
            var singleFailure: Pair<String, String>? = null
            _state.update {
                it.copy(
                    image = null, rawText = "", scanError = null,
                    scanStep = 0, batchProgress = if (uris.size > 1) 1 to uris.size else null,
                    screen = Screen.PROCESSING, busy = true
                )
            }
            try {
                val localUris = uris.map { scanner.cacheImage(it) }
                saved["image"] = localUris.first().toString()
                _state.update { it.copy(image = localUris.first().toString()) }
                for ((index, uri) in localUris.withIndex()) {
                    _state.update {
                        it.copy(
                            image = uri.toString(), rawText = "", scanStep = 0,
                            batchProgress = if (uris.size > 1) (index + 1) to uris.size else null
                        )
                    }
                    var raw = ""
                    try {
                        raw = scanner.recognize(uri)
                        saved["rawText"] = raw
                        _state.update { it.copy(rawText = raw, scanStep = 1) }
                        check(scanner.aiAvailable) { "Teks sudah terbaca, tetapi layanan AI belum tersedia. Gunakan Isi Manual untuk memeriksa teks dan melengkapi item." }
                        val items = withTimeout(90_000) { scanner.format(raw, uri) }
                        _state.update { it.copy(scanStep = 2) }
                        entries += DraftEntry(newDraft().copy(items = items), uri.toString(), raw)
                    } catch (e: TimeoutCancellationException) {
                        val msg = "AI terlalu lama merespons. Coba lagi atau isi manual."
                        if (uris.size == 1) { singleFailure = raw to msg; break }
                        entries += DraftEntry(newDraft(), uri.toString(), raw, msg)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val msg = e.message ?: "Nota gagal diproses."
                        if (uris.size == 1) { singleFailure = raw to msg; break }
                        entries += DraftEntry(newDraft(), uri.toString(), raw, msg)
                    }
                }
                if (singleFailure != null) {
                    _state.update { it.copy(screen = Screen.FAILURE, rawText = singleFailure.first, scanError = singleFailure.second, batchProgress = null) }
                } else if (entries.isNotEmpty()) {
                    val first = entries.first()
                    setDraftList(entries, 0)
                    _state.update { it.copy(screen = Screen.REVIEW, batchProgress = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(screen = Screen.FAILURE, rawText = "",
                    scanError = e.message ?: "Foto nota tidak dapat dibuka. Pilih ulang dari galeri.") }
            } finally {
                _state.update { it.copy(busy = false, batchProgress = null) }
            }
        }
    }
    fun cancelScan() {
        scanJob?.cancel(); scanJob = null
        _state.update { it.copy(busy = false, batchProgress = null, screen = Screen.HOME) }
    }
    private fun newDraft() = Receipt(UUID.randomUUID().toString(), LocalDate.now(), createdAt = System.currentTimeMillis())
    fun manual() {
        val entry = DraftEntry(newDraft(), state.value.image, state.value.rawText)
        setDraftList(listOf(entry), 0)
        _state.update { it.copy(screen = Screen.REVIEW) }
    }
    private fun setDraftList(entries: List<DraftEntry>, index: Int) {
        val safeIndex = index.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
        val active = entries.getOrNull(safeIndex)
        _state.update {
            it.copy(
                drafts = entries,
                activeDraftIndex = safeIndex,
                draft = active?.receipt ?: Receipt(),
                image = active?.image,
                rawText = active?.rawText ?: "",
                scanError = active?.error
            )
        }
        if (active != null) saved["draft"] = ReceiptJson.receipt(active.receipt).toString()
    }
    fun selectDraft(index: Int) {
        if (index in state.value.drafts.indices) setDraftList(state.value.drafts, index)
    }
    fun removeDraft(index: Int) {
        val remaining = state.value.drafts.filterIndexed { i, _ -> i != index }
        if (remaining.isEmpty()) {
            clearDraft()
            _state.update { it.copy(screen = Screen.HOME) }
        } else {
            setDraftList(remaining, index.coerceAtMost(remaining.lastIndex))
        }
    }
    fun setDraft(draft: Receipt) {
        _state.update { s ->
            val updatedList = if (s.drafts.isEmpty()) {
                listOf(DraftEntry(draft, s.image, s.rawText, s.scanError))
            } else {
                s.drafts.mapIndexed { i, entry -> if (i == s.activeDraftIndex) entry.copy(receipt = draft) else entry }
            }
            s.copy(draft = draft, drafts = updatedList)
        }
        saved["draft"] = ReceiptJson.receipt(draft).toString()
    }
    fun updateItem(index: Int?, item: ReceiptItem) {
        val items = state.value.draft.items.toMutableList()
        if (index == null) items.add(item) else items[index] = item
        setDraft(state.value.draft.copy(items = items))
    }
    fun removeItem(index: Int) { setDraft(state.value.draft.copy(items = state.value.draft.items.filterIndexed { i, _ -> i != index })) }
    fun confirmItems() { setDraft(state.value.draft.copy(items = state.value.draft.items.map { if (it.valid) it.copy(needsReview = false) else it })) }
    fun saveReceipt() = action {
        val draft = state.value.draft
        require(!draft.date.isAfter(LocalDate.now())) { "Tanggal nota tidak boleh di masa depan." }
        requireNotNull(repository).saveReceipt(draft)
        val remaining = state.value.drafts.filterIndexed { i, _ -> i != state.value.activeDraftIndex }
        if (remaining.isEmpty()) {
            clearDraft()
            _state.update { it.copy(screen = Screen.HISTORY, message = "Transaksi tersimpan.") }
        } else {
            setDraftList(remaining, state.value.activeDraftIndex.coerceAtMost(remaining.lastIndex))
            notify("Nota tersimpan. Sisa ${remaining.size} nota untuk diperiksa.")
        }
    }
    fun saveAllReceipts() = action {
        val repo = requireNotNull(repository)
        val pending = state.value.drafts.ifEmpty { listOf(DraftEntry(state.value.draft, state.value.image, state.value.rawText)) }
        pending.forEach { require(!it.receipt.date.isAfter(LocalDate.now())) { "Tanggal nota tidak boleh di masa depan." } }
        var savedCount = 0
        val remaining = pending.toMutableList()
        for (entry in pending) {
            try {
                repo.saveReceipt(entry.receipt)
                remaining.remove(entry)
                savedCount++
            } catch (e: Exception) {
                setDraftList(remaining, 0)
                throw IllegalArgumentException("$savedCount nota tersimpan, gagal pada nota berikutnya: ${e.message}")
            }
        }
        clearDraft()
        _state.update { it.copy(screen = Screen.HISTORY, message = "$savedCount transaksi tersimpan.") }
    }
    private fun clearDraft() {
        saved.remove<String>("draft"); saved.remove<String>("image"); saved.remove<String>("rawText")
        _state.update { it.copy(draft = Receipt(), drafts = emptyList(), activeDraftIndex = 0, image = null, rawText = "", scanError = null) }
    }
    fun saveProduct(product: Product, onSaved: () -> Unit) = action {
        requireNotNull(repository).saveProduct(product)
        onSaved()
    }
    fun deleteProduct(product: Product, onDeleted: () -> Unit) = action {
        requireNotNull(repository).deleteProduct(product)
        onDeleted()
        notify("Produk ${product.name} dihapus dari stok.")
    }
    fun deleteReceipt(receipt: Receipt, onDeleted: () -> Unit = {}) = action {
        requireNotNull(repository).deleteReceipt(receipt)
        onDeleted()
        notify("Transaksi dihapus dan stok diperbarui.")
    }
    fun recordPurchase(product: Product, quantity: Int, price: Long, date: LocalDate, id: String, onSaved: () -> Unit) = action {
        require(!date.isAfter(LocalDate.now())) { "Tanggal pembelian tidak boleh di masa depan." }
        val receipt = Receipt(id, date, ReceiptType.PURCHASE,
            listOf(ReceiptItem(product.name, quantity, price, false)), System.currentTimeMillis())
        requireNotNull(repository).saveReceipt(receipt)
        onSaved(); notify("Pembelian dicatat. Stok yang sudah diisi diperbarui otomatis.")
    }
}
