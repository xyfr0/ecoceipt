package com.ecoceipt.app.data

import com.ecoceipt.app.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object ReceiptJson {
    fun item(item: ReceiptItem) = JSONObject().put("name", item.name).put("quantity", item.quantity)
        .put("unitPrice", item.unitPrice).put("needsReview", item.needsReview)
    fun item(json: JSONObject) = ReceiptItem(json.optString("name"), json.optInt("quantity"),
        json.optLong("unitPrice"), json.optBoolean("needsReview", true))
    fun receipt(receipt: Receipt) = JSONObject().put("id", receipt.id).put("date", receipt.date.toString())
        .put("type", receipt.type.name).put("createdAt", receipt.createdAt)
        .put("items", JSONArray(receipt.items.map(::item)))
    fun receipt(json: JSONObject) = Receipt(json.getString("id"), LocalDate.parse(json.getString("date")),
        ReceiptType.valueOf(json.getString("type")), json.getJSONArray("items").let { a ->
            (0 until a.length()).map { item(a.getJSONObject(it)) }
        }, json.optLong("createdAt"))
    fun product(product: Product) = JSONObject().put("id", product.id).put("name", product.name)
        .put("stock", product.stock ?: JSONObject.NULL).put("leadTimeDays", product.leadTimeDays ?: JSONObject.NULL)
        .put("safetyStock", product.safetyStock ?: JSONObject.NULL).put("revision", product.revision)
    fun product(json: JSONObject) = Product(json.getString("id"), json.getString("name"),
        if (json.isNull("stock")) null else json.getInt("stock"),
        if (json.isNull("leadTimeDays")) null else json.getInt("leadTimeDays"),
        if (json.isNull("safetyStock")) null else json.getInt("safetyStock"), json.optLong("revision"))

    fun parseAi(text: String): List<ReceiptItem> {
        val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val root = JSONObject(clean)
        val array = root.getJSONArray("items")
        require(array.length() in 1..100) { "AI tidak menghasilkan item yang dapat diperiksa. Isi nota secara manual." }
        return (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            val quantity = row.optDouble("quantity", Double.NaN)
            val price = row.optDouble("unitPrice", Double.NaN)
            ReceiptItem((row.opt("name") as? String)?.trim().orEmpty(),
                if (quantity.isFinite() && quantity % 1.0 == 0.0 && quantity in 1.0..1_000_000.0) quantity.toInt() else 0,
                if (price.isFinite() && price % 1.0 == 0.0 && price in 0.0..1_000_000_000.0) price.toLong() else -1,
                needsReview = true)
        }
    }
}
