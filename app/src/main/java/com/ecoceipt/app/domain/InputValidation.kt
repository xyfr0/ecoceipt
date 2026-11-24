package com.ecoceipt.app.domain

fun numberError(value: String, min: Long, max: Long, optional: Boolean = false): String? {
    if (value.isBlank()) return if (optional) null else "Wajib diisi."
    val number = value.toLongOrNull()
    return if (number == null || number !in min..max) "Masukkan bilangan bulat $min sampai $max." else null
}
