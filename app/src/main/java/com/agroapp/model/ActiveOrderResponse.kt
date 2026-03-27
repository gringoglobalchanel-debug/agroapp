package com.agroapp.model

data class ActiveOrderResponse(
    val id: Int,
    val status: String,
    val total: Double,
    val driver_id: Int?,
    val delivery_lat: Double?,
    val delivery_lng: Double?
)