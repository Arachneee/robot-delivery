package com.robotdelivery.application.command

import com.robotdelivery.domain.delivery.DeliveryStatus

data class ChangeStatusResult(
    val previousStatus: DeliveryStatus,
    val currentStatus: DeliveryStatus,
)

