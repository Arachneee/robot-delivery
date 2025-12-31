package com.robotdelivery.application.command.vo

import com.robotdelivery.domain.delivery.vo.DeliveryStatus

data class ChangeStatusResult(
    val previousStatus: DeliveryStatus,
    val currentStatus: DeliveryStatus,
)
