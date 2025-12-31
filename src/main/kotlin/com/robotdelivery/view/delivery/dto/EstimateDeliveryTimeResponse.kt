package com.robotdelivery.view.delivery.dto

data class EstimateDeliveryTimeResponse(
    val estimatedPickupSeconds: Long,
    val estimatedDeliverySeconds: Long,
    val totalEstimatedSeconds: Long,
)

