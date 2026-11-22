package com.ecoceipt.app.domain

import java.security.MessageDigest
import java.time.LocalDate
import java.util.Locale

enum class ReceiptType { SALE, PURCHASE }

data class ReceiptItem(
    val name: String = "", val quantity: Int = 0, val unitPrice: Long = 0,
    val needsReview: Boolean = true
) {
    val subtotal: Long get() = quantity.toLong() * unitPrice
    val valid: Boolean get() = name.isNotBlank() && name.length <= 120 &&
        quantity in 1..1_000_000 && unitPrice in 0..1_000_000_000L
    val productId: String get() = productId(name)
}

data class Receipt(
    val id: String = "", val date: LocalDate = LocalDate.now(),
    val type: ReceiptType = ReceiptType.SALE, val items: List<ReceiptItem> = emptyList(),
    val createdAt: Long = 0
) {
    val total: Long get() = items.sumOf { it.subtotal }
}

data class Product(
    val id: String = "", val name: String = "", val stock: Int? = null,
    val leadTimeDays: Int? = null, val safetyStock: Int? = null,
    val revision: Long = 0
) {
    val complete: Boolean get() = stock != null && leadTimeDays != null && safetyStock != null
    val missing: String get() = listOfNotNull(
        if (stock == null) "stok tersedia" else null,
        if (leadTimeDays == null) "lama barang datang" else null,
        if (safetyStock == null) "stok cadangan" else null
    ).joinToString(", ")
}

fun productId(name: String): String = MessageDigest.getInstance("SHA-256")
    .digest(name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

enum class Period(val label: String) {
    ALL("Semua"), TODAY("Hari Ini"), WEEK("Minggu Ini"), MONTH("Bulan Ini")
}
