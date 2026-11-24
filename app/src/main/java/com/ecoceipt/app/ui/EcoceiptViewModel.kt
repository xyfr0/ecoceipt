package com.ecoceipt.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
data class UiState(
    val screen: Screen = Screen.LOGIN, val products: List<Product> = emptyList(),
    val receipts: List<Receipt> = emptyList(), val busy: Boolean = false, val message: String? = null,
    val dataError: String? = null, val image: String? = null, val rawText: String = "", val scanStep: Int = 0,
    val draft: Receipt = Receipt(), val selectedProduct: String? = null,
    val guest: Boolean = false, val user: String = "", val today: LocalDate = LocalDate.now(),
    val openGallery: Boolean = false,
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
        if (saved.get<Boolean>("guest") == true) guest()
        else if (cloudConfigured && auth.currentUser != null) connectCloud()
    }
    private fun observe(repo: EcoceiptRepository, guest: Boolean, user: String, resetScreen: Boolean = true) {
        observer?.cancel()
        repository = repo
        _state.update { it.copy(screen = if (!resetScreen) it.screen else if (it.draft.id.isNotBlank()) Screen.REVIEW else Screen.HOME,
            products = if (resetScreen) emptyList() else it.products,
            receipts = if (resetScreen) emptyList() else it.receipts,
            summaries = if (resetScreen) emptyMap() else it.summaries,
            recommendations = if (resetScreen) emptyList() else it.recommendations,
            guest = guest, user = user, dataError = null) }
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
        observe(repo, true, "Tamu")
    }
    private fun connectCloud() {
        val user = auth.currentUser ?: return
        saved["guest"] = false
        observe(FirestoreRepository(FirebaseFirestore.getInstance(), user.uid), false, user.email ?: "Pengguna")
    }
    fun login(email: String, password: String, register: Boolean = false) = action {
        check(cloudConfigured) { "Layanan akun belum tersedia. Kamu dapat melanjutkan sebagai tamu." }
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
        clearDraft()
        _state.value = UiState()
    }
    fun refreshDate() { _state.update { it.copy(today = LocalDate.now()) } }
    fun navigate(screen: Screen) { if (!state.value.busy) _state.update { it.copy(screen = screen, openGallery = false) } }
    fun chooseGallery() { if (!state.value.busy) _state.update { it.copy(screen = Screen.SCAN, openGallery = true) } }
    fun selectProduct(id: String) { _state.update { it.copy(selectedProduct = id, screen = Screen.PRODUCT) } }
    fun notify(message: String) { _state.update { it.copy(message = message) } }
    fun dismissMessage() { _state.update { it.copy(message = null) } }
    fun retryData() {
        val repo = repository ?: return
        observe(repo, state.value.guest, state.value.user, resetScreen = false)
    }
    fun back() {
        if (state.value.screen == Screen.PROCESSING) { cancelScan(); return }
        if (state.value.busy) return
        navigate(when (state.value.screen) {
            Screen.REGISTER -> Screen.LOGIN
            Screen.PRODUCT, Screen.INCOMPLETE -> Screen.RECOMMENDATIONS
            else -> Screen.HOME
        })
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
    fun scan(uri: Uri) {
        if (state.value.busy) return
        scanJob?.cancel()
        clearDraft()
        saved["image"] = uri.toString()
        scanJob = viewModelScope.launch {
            _state.update { it.copy(image = uri.toString(), rawText = "", scanStep = 0, screen = Screen.PROCESSING, busy = true) }
            try {
                val raw = scanner.recognize(uri)
                saved["rawText"] = raw
                _state.update { it.copy(rawText = raw, scanStep = 1) }
                check(cloudConfigured) { "Teks sudah terbaca, tetapi layanan AI belum tersedia. Gunakan Isi Manual untuk memeriksa teks dan melengkapi item." }
                val items = withTimeout(90_000) { scanner.format(raw) }
                _state.update { it.copy(scanStep = 2) }
                setDraft(newDraft().copy(items = items))
                _state.update { it.copy(screen = Screen.REVIEW) }
            } catch (e: TimeoutCancellationException) {
                _state.update { it.copy(screen = Screen.FAILURE, message = "AI terlalu lama merespons. Coba lagi atau isi manual.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.update { it.copy(screen = Screen.FAILURE, message = e.message ?: "Nota gagal diproses.") }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun cancelScan() {
        scanJob?.cancel(); scanJob = null
        _state.update { it.copy(busy = false, screen = Screen.HOME) }
    }
    private fun newDraft() = Receipt(UUID.randomUUID().toString(), LocalDate.now(), createdAt = System.currentTimeMillis())
    fun manual() { setDraft(newDraft()); _state.update { it.copy(screen = Screen.REVIEW) } }
    fun setDraft(draft: Receipt) {
        _state.update { it.copy(draft = draft) }
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
        clearDraft()
        _state.update { it.copy(screen = Screen.HISTORY, message = "Transaksi tersimpan.") }
    }
    private fun clearDraft() {
        saved.remove<String>("draft"); saved.remove<String>("image"); saved.remove<String>("rawText")
        _state.update { it.copy(draft = Receipt(), image = null, rawText = "") }
    }
    fun saveProduct(product: Product, onSaved: () -> Unit) = action {
        requireNotNull(repository).saveProduct(product)
        onSaved()
    }
    fun recordPurchase(product: Product, quantity: Int, price: Long, date: LocalDate, id: String, onSaved: () -> Unit) = action {
        require(!date.isAfter(LocalDate.now())) { "Tanggal pembelian tidak boleh di masa depan." }
        val receipt = Receipt(id, date, ReceiptType.PURCHASE,
            listOf(ReceiptItem(product.name, quantity, price, false)), System.currentTimeMillis())
        requireNotNull(repository).saveReceipt(receipt)
        onSaved(); notify("Pembelian dicatat. Stok yang sudah diisi diperbarui otomatis.")
    }
}
