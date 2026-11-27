package com.ecoceipt.app.domain

object Inventory {
    fun validate(receipt: Receipt) {
        require(receipt.id.isNotBlank()) { "ID transaksi tidak valid." }
        require(!receipt.date.isAfter(java.time.LocalDate.now())) { "Tanggal nota tidak boleh di masa depan." }
        require(receipt.items.size in 1..100) { "Nota harus berisi 1–100 item." }
        require(receipt.items.all { it.valid && !it.needsReview }) { "Periksa dan konfirmasi seluruh item terlebih dahulu." }
    }

    fun apply(receipt: Receipt, current: List<Product>): List<Product> =
        applyDelta(receipt, current, if (receipt.type == ReceiptType.SALE) -1L else 1L, validateReceipt = true)

    /** Reverts a previously saved receipt's stock effect without modifying products that have unknown stock. */
    fun revert(receipt: Receipt, current: List<Product>): List<Product> =
        applyDelta(receipt, current, if (receipt.type == ReceiptType.SALE) 1L else -1L, validateReceipt = false)

    private fun applyDelta(
        receipt: Receipt,
        current: List<Product>,
        sign: Long,
        validateReceipt: Boolean
    ): List<Product> {
        if (validateReceipt) validate(receipt)
        val result = current.associateBy { it.id }.toMutableMap()
        receipt.items.groupBy { it.productId }.forEach { (id, items) ->
            val old = result[id] ?: if (validateReceipt) Product(id, items.first().name.trim()) else return@forEach
            val delta = items.sumOf { it.quantity.toLong() } * sign
            val newStock = old.stock?.let {
                val next = it.toLong() + delta
                require(next in 0..1_000_000_000L) { "Stok ${old.name} tidak mencukupi atau jumlah melebihi batas. Periksa Data Stok." }
                next.toInt()
            }
            result[id] = old.copy(stock = newStock, revision = old.revision + 1)
        }
        return result.values.toList()
    }

    fun validateProduct(product: Product) {
        require(product.name.isNotBlank() && product.name.length <= 120) { "Nama produk wajib diisi (maksimal 120 karakter)." }
        require(product.id == productId(product.name)) { "Nama produk yang sudah tersimpan tidak dapat diubah." }
        require(product.stock == null || product.stock in 0..1_000_000_000) { "Stok harus 0–1.000.000.000." }
        require(product.leadTimeDays == null || product.leadTimeDays in 0..365) { "Lead time harus 0–365 hari." }
        require(product.safetyStock == null || product.safetyStock in 0..1_000_000_000) { "Stok cadangan tidak valid." }
    }
}
