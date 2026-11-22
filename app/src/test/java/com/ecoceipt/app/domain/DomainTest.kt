package com.ecoceipt.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DomainTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val rice = Product(productId("Beras 5 kg"), "Beras 5 kg", 4, 4, 6)
    private fun receipt(q: Int, daysAgo: Long = 0, type: ReceiptType = ReceiptType.SALE, name: String = rice.name) =
        Receipt("r-$q-$daysAgo-$type", today.minusDays(daysAgo), type, listOf(ReceiptItem(name, q, 65000, false)))

    @Test fun ropUsesSevenCalendarDaysIncludingZeroDays() {
        val result = StockRecommender.calculate(rice, listOf(receipt(21)), today)
        assertEquals(3.0, result.averageDailySales, 0.0001)
        assertEquals(18L, result.reorderPoint)
        assertEquals(35L, result.suggestedPurchase)
        assertTrue(result.needsRestock)
    }
    @Test fun purchasesNeverCountAsSales() {
        val records = listOf(receipt(14), receipt(500, type = ReceiptType.PURCHASE))
        assertEquals(2.0, StockRecommender.calculate(rice, records, today).averageDailySales, 0.0001)
        assertEquals(910000L, SalesAnalysis.calculate(records, Period.ALL, today).revenue)
    }
    @Test fun oldAndFutureRecordsAreOutsideRecommendationWindow() {
        val result = StockRecommender.calculate(rice, listOf(receipt(7, 6), receipt(99, 7), receipt(99, -1)), today)
        assertEquals(1.0, result.averageDailySales, 0.0001)
    }
    @Test fun zeroLeadAndSafetyAreCompleteNotMissing() {
        assertTrue(rice.copy(leadTimeDays = 0, safetyStock = 0).complete)
        assertFalse(rice.copy(leadTimeDays = null).complete)
    }
    @Test fun missingParametersAndNoHistoryDoNotFabricateRecommendation() {
        assertNull(StockRecommender.calculate(rice.copy(safetyStock = null), listOf(receipt(7)), today).reorderPoint)
        assertNull(StockRecommender.calculate(rice, emptyList(), today).suggestedPurchase)
    }
    @Test fun thresholdEqualityTriggersRestock() {
        val r = StockRecommender.calculate(rice.copy(stock = 18), listOf(receipt(21)), today)
        assertTrue(r.needsRestock)
    }
    @Test fun sufficientStockHasZeroSuggestedPurchase() {
        val r = StockRecommender.calculate(rice.copy(stock = 100), listOf(receipt(21)), today)
        assertFalse(r.needsRestock)
        assertEquals(0L, r.suggestedPurchase)
    }
    @Test fun fractionalThresholdIsRoundedUp() {
        assertEquals(7L, StockRecommender.calculate(rice, listOf(receipt(1)), today).reorderPoint)
    }
    @Test fun stockUpdatesCombineDuplicateLines() {
        val r = receipt(1).copy(items = listOf(ReceiptItem(rice.name, 1, 1, false), ReceiptItem("  BERAS   5 kg ", 2, 1, false)))
        assertEquals(1, Inventory.apply(r, listOf(rice)).single().stock)
    }
    @Test fun stockIncreasesOnlyOnPurchase() {
        assertEquals(9, Inventory.apply(receipt(5, type = ReceiptType.PURCHASE), listOf(rice)).single().stock)
    }
    @Test fun unknownOpeningStockStaysUnknown() {
        val p = Inventory.apply(receipt(2), emptyList()).single()
        assertNull(p.stock)
        assertFalse(p.complete)
    }
    @Test fun insufficientStockRejectsWholeTransactionWithoutMutatingInput() {
        val initial = listOf(rice)
        assertThrows(IllegalArgumentException::class.java) { Inventory.apply(receipt(5), initial) }
        assertEquals(4, initial.single().stock)
    }
    @Test fun unreviewedOrInvalidItemsCannotBeSaved() {
        assertThrows(IllegalArgumentException::class.java) { Inventory.validate(receipt(1).copy(items = listOf(ReceiptItem(rice.name, 1, 10, true)))) }
        assertThrows(IllegalArgumentException::class.java) { Inventory.validate(receipt(0)) }
        assertThrows(IllegalArgumentException::class.java) { Inventory.validate(receipt(1).copy(items = emptyList())) }
    }
    @Test fun negativeAndOverflowStockCannotBeStored() {
        assertThrows(IllegalArgumentException::class.java) { Inventory.validateProduct(rice.copy(stock = -1)) }
        assertThrows(IllegalArgumentException::class.java) { Inventory.apply(receipt(1, type = ReceiptType.PURCHASE), listOf(rice.copy(stock = 1_000_000_000))) }
    }
    @Test fun productMatchingNormalizesCaseAndWhitespaceButPreservesSize() {
        assertEquals(productId("Beras 5 kg"), productId("  BERAS   5 kg "))
        assertNotEquals(productId("Beras 5 kg"), productId("Beras 10 kg"))
    }
    @Test fun comparisonUsesEqualLengthCalendarPeriods() {
        val result = SalesAnalysis.calculate(listOf(receipt(10), receipt(5, 7)), Period.WEEK, today)
        assertEquals(100.0, result.revenueChange!!, 0.0001)
        assertEquals(100.0, result.quantityChange!!, 0.0001)
        assertEquals(7, result.trend.size)
    }
    @Test fun noPreviousSalesReturnsUndefinedChangeNotInfinity() {
        assertNull(SalesAnalysis.calculate(listOf(receipt(1)), Period.TODAY, today).revenueChange)
    }
    @Test fun allPeriodHasNoMisleadingPriorPeriodComparison() {
        assertNull(SalesAnalysis.calculate(listOf(receipt(1, 20), receipt(3)), Period.ALL, today).revenueChange)
    }
    @Test fun bestSellersRankByUnitsAndExcludePurchases() {
        val result = SalesAnalysis.calculate(listOf(receipt(1), receipt(5, name = "Gula"), receipt(100, type = ReceiptType.PURCHASE)), Period.TODAY, today)
        assertEquals("Gula", result.bestSellers.first().name)
        assertEquals(2, result.transactions)
    }
    @Test fun monetaryBoundsAvoidLongOverflow() {
        val item = ReceiptItem("Barang", 1_000_000, 1_000_000_000, false)
        val r = receipt(1).copy(items = List(100) { item })
        Inventory.validate(r)
        assertEquals(100_000_000_000_000_000L, r.total)
        assertFalse(item.copy(unitPrice = Long.MAX_VALUE).valid)
    }
}
