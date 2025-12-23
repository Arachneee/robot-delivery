package com.robotdelivery.view.delivery.dto

data class ReassignRobotResponse(
    val deliveryId: Long,
    val previousRobotId: Long,
    val newRobotId: Long,
    val message: String,
)

