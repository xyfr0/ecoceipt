package com.ecoceipt.app.data

import android.content.Context
import android.util.AtomicFile
import com.ecoceipt.app.domain.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface EcoceiptRepository {
    val products: Flow<List<Product>>
    val receipts: Flow<List<Receipt>>
    suspend fun saveProduct(product: Product)
    suspend fun saveReceipt(receipt: Receipt)
}

class LocalRepository(context: Context) : EcoceiptRepository {
    private val file = AtomicFile(File(context.filesDir, "guest-records.json"))
    private val mutex = Mutex()
    override val products = MutableStateFlow<List<Product>>(emptyList())
    override val receipts = MutableStateFlow<List<Receipt>>(emptyList())

    suspend fun load() = withContext(Dispatchers.IO) {
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
            val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
            val p = json.getJSONArray("products")
            val r = json.getJSONArray("receipts")
            products.value = (0 until p.length()).map { ReceiptJson.product(p.getJSONObject(it)) }
            receipts.value = (0 until r.length()).map { ReceiptJson.receipt(r.getJSONObject(it)) }
        }
    }
    private fun commit(p: List<Product>, r: List<Receipt>) {
        val bytes = JSONObject().put("products", JSONArray(p.map(ReceiptJson::product)))
            .put("receipts", JSONArray(r.map(ReceiptJson::receipt))).toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        products.value = p
        receipts.value = r
    }
    override suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        mutex.withLock {
            Inventory.validateProduct(product)
            val old = products.value.find { it.id == product.id }
            require(old == null || old.revision == product.revision) { "Stok berubah. Buka ulang produk sebelum menyimpan." }
            commit(products.value.filterNot { it.id == product.id } + product.copy(revision = product.revision + 1), receipts.value)
        }
    }
    override suspend fun saveReceipt(receipt: Receipt) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (receipts.value.none { it.id == receipt.id }) {
                commit(Inventory.apply(receipt, products.value), receipts.value + receipt)
            }
        }
    }
}

class FirestoreRepository(db: FirebaseFirestore, uid: String) : EcoceiptRepository {
    private val user = db.collection("users").document(uid)
    private val productCollection = user.collection("products")
    private val receiptCollection = user.collection("receipts")
    private val firestore = db
    override val products: Flow<List<Product>> = callbackFlow {
        val listener = productCollection.addSnapshotListener { snap, error ->
            if (error != null) close(error)
            else if (snap != null) try {
                trySend(snap.documents.map { ReceiptJson.product(JSONObject(requireNotNull(it.data))) })
            } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }
    override val receipts: Flow<List<Receipt>> = callbackFlow {
        val listener = receiptCollection.addSnapshotListener { snap, error ->
            if (error != null) close(error)
            else if (snap != null) try {
                trySend(snap.documents.map { ReceiptJson.receipt(JSONObject(requireNotNull(it.data))) })
            } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }
    override suspend fun saveProduct(product: Product) {
        Inventory.validateProduct(product)
        val ref = productCollection.document(product.id)
        firestore.runTransaction { tx ->
            val old = tx.get(ref)
            require(!old.exists() || old.getLong("revision") == product.revision) { "Stok berubah. Buka ulang produk sebelum menyimpan." }
            tx.set(ref, productMap(product.copy(revision = product.revision + 1)))
            Unit
        }.await()
    }
    override suspend fun saveReceipt(receipt: Receipt) {
        Inventory.validate(receipt)
        val ref = receiptCollection.document(receipt.id)
        firestore.runTransaction { tx ->
            if (!tx.get(ref).exists()) {
                val existing = receipt.items.map { it.productId }.distinct().mapNotNull { id ->
                    val doc = tx.get(productCollection.document(id))
                    doc.data?.let { ReceiptJson.product(JSONObject(it)) }
                }
                val updated = Inventory.apply(receipt, existing)
                updated.forEach { tx.set(productCollection.document(it.id), productMap(it)) }
                tx.set(ref, mapOf("id" to receipt.id, "date" to receipt.date.toString(), "type" to receipt.type.name,
                    "createdAt" to receipt.createdAt, "items" to receipt.items.map {
                        mapOf("name" to it.name, "quantity" to it.quantity, "unitPrice" to it.unitPrice, "needsReview" to false)
                    }))
            }
            Unit
        }.await()
    }
    private fun productMap(p: Product): Map<String, Any?> = mapOf("id" to p.id, "name" to p.name,
        "stock" to p.stock, "leadTimeDays" to p.leadTimeDays, "safetyStock" to p.safetyStock, "revision" to p.revision)
}
