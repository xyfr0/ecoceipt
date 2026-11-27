package com.ecoceipt.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

data class ProductSales(val id: String, val name: String, val quantity: Long, val revenue: Long)
data class DaySales(val date: LocalDate, val revenue: Long, val quantity: Long)
data class SalesSummary(
    val revenue: Long, val transactions: Int, val averageDailyQuantity: Double,
    val revenueChange: Double?, val transactionChange: Double?, val quantityChange: Double?,
    val bestSellers: List<ProductSales>, val trend: List<DaySales>
)

object SalesAnalysis {
    fun start(period: Period, today: LocalDate, receipts: List<Receipt>): LocalDate = when (period) {
        Period.ALL -> receipts.filter { it.type == ReceiptType.SALE && !it.date.isAfter(today) }
            .minOfOrNull { it.date } ?: today
        Period.TODAY -> today
        Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Period.MONTH -> today.withDayOfMonth(1)
    }

    fun calculate(receipts: List<Receipt>, period: Period, today: LocalDate = LocalDate.now()): SalesSummary {
        val from = start(period, today, receipts)
        val days = ChronoUnit.DAYS.between(from, today) + 1
        val sales = receipts.filter { it.type == ReceiptType.SALE && !it.date.isAfter(today) }
        val current = sales.filter { !it.date.isBefore(from) }
        val previous = sales.filter { it.date >= from.minusDays(days) && it.date < from }
        fun change(now: Long, before: Long): Double? = if (period == Period.ALL || before == 0L) null
            else (now - before) * 100.0 / before
        val quantity = current.sumOf { r -> r.items.sumOf { it.quantity.toLong() } }
        val previousQuantity = previous.sumOf { r -> r.items.sumOf { it.quantity.toLong() } }
        val products = current.flatMap { it.items }.groupBy { it.productId }.map { (id, items) ->
            ProductSales(id, items.first().name, items.sumOf { it.quantity.toLong() }, items.sumOf { it.subtotal })
        }.sortedByDescending { it.quantity }
        val chartStart = maxOf(from, today.minusDays(6))
        val trend = (0..ChronoUnit.DAYS.between(chartStart, today)).map { offset ->
            val date = chartStart.plusDays(offset)
            val day = current.filter { it.date == date }
            DaySales(date, day.sumOf { it.total }, day.sumOf { r -> r.items.sumOf { it.quantity.toLong() } })
        }
        return SalesSummary(current.sumOf { it.total }, current.size, quantity.toDouble() / days,
            change(current.sumOf { it.total }, previous.sumOf { it.total }),
            change(current.size.toLong(), previous.size.toLong()), change(quantity, previousQuantity), products, trend)
    }
}

data class StockRecommendation(
    val product: Product, val dailySales: List<Long>, val averageDailySales: Double,
    val reorderPoint: Long?, val suggestedPurchase: Long?, val hasSalesHistory: Boolean
) {
    val needsRestock: Boolean get() = reorderPoint != null && product.stock != null && product.stock <= reorderPoint
    val nearRestock: Boolean get() = reorderPoint != null && product.stock != null &&
        product.stock > reorderPoint &&
        product.stock <= maxOf(ceil(reorderPoint * 1.25).toLong(), reorderPoint + 2)
    val status: String get() = when {
        !product.complete -> "Data belum lengkap"
        !hasSalesHistory -> "Belum ada penjualan"
        needsRestock -> "Segera restock"
        nearRestock -> "Mendekati batas"
        else -> "Stok cukup"
    }
}

object StockRecommender {
    fun calculate(product: Product, receipts: List<Receipt>, today: LocalDate = LocalDate.now()): StockRecommendation {
        val sales = receipts.filter { it.type == ReceiptType.SALE && it.date in today.minusDays(6)..today }
        val daily = (6L downTo 0L).map { offset -> sales.filter { it.date == today.minusDays(offset) }
            .flatMap { it.items }.filter { it.productId == product.id }.sumOf { it.quantity.toLong() } }
        val average = daily.sum() / 7.0
        val hasSales = daily.sum() > 0
        if (!product.complete || !hasSales) return StockRecommendation(product, daily, average, null, null, hasSales)
        val rop = ceil(average * requireNotNull(product.leadTimeDays) + requireNotNull(product.safetyStock)).toLong()
        val target = ceil(average * (product.leadTimeDays + 7) + product.safetyStock).toLong()
        val buy = if (requireNotNull(product.stock) <= rop) (target - product.stock).coerceAtLeast(0) else 0
        return StockRecommendation(product, daily, average, rop, buy, true)
    }
}
