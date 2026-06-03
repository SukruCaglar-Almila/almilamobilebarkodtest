package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SevkEmirModel(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val date: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class UrunModel(
    val id: String,
    val productName: String,
    val productCode: String,
    val quantity: Int,
    val status: String
)

@JsonClass(generateAdapter = true)
data class AltParcaModel(
    val id: String,
    val productId: String,
    val partName: String,
    val partCode: String,
    val isCompleted: Boolean,
    val imagePath: String? = null,
    val imageBase64: String? = null
)
