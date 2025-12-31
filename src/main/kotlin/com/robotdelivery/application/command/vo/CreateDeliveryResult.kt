package com.robotdelivery.application.command.vo

import com.robotdelivery.domain.common.vo.DeliveryId
import java.time.Duration

data class CreateDeliveryResult(
    val deliveryId: DeliveryId,
    val estimatedPickupDuration: Duration,
    val estimatedDeliveryDuration: Duration,
)

