package com.robotdelivery.view.delivery.dto

data class CancelDeliveryResponse(
    val deliveryId: Long,
    val requiresReturn: Boolean,
    val message: String,
)

