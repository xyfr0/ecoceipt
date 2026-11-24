package com.ecoceipt.app.data

import org.junit.Assert.*
import org.junit.Test

class ReceiptJsonTest {
    @Test fun missingOrNonTextNameMustBeCorrected() {
        for (name in listOf("null", "123", "{}")) {
            val item = ReceiptJson.parseAi("""{"items":[{"name":$name,"quantity":1,"unitPrice":100}]}""").single()
            assertFalse(item.valid)
            assertTrue(item.needsReview)
        }
    }
    @Test fun longProductNameIsNotSilentlyTruncated() {
        val name = "A".repeat(121)
        val item = ReceiptJson.parseAi("""{"items":[{"name":"$name","quantity":1,"unitPrice":100}]}""").single()
        assertEquals(name, item.name)
        assertFalse(item.valid)
    }
    @Test fun parsesItemsButRequiresHumanReview() {
        val item = ReceiptJson.parseAi("""{"items":[{"name":"Beras 5 kg","quantity":2,"unitPrice":65000}]}""").single()
        assertEquals(130000L, item.subtotal)
        assertTrue(item.needsReview)
    }
    @Test fun ambiguousNumbersRemainInvalidRatherThanBeingGuessed() {
        val item = ReceiptJson.parseAi("""{"items":[{"name":"Beras","quantity":null,"unitPrice":null}]}""").single()
        assertFalse(item.valid)
    }
    @Test fun fractionalQuantityCannotSilentlyTruncate() {
        val item = ReceiptJson.parseAi("""{"items":[{"name":"Beras","quantity":1.5,"unitPrice":12000}]}""").single()
        assertFalse(item.valid)
    }
    @Test fun emptyResponseIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ReceiptJson.parseAi("""{"items":[]}""") }
    }
}
