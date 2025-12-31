package com.robotdelivery.application.query.vo

import java.time.Duration

data class DeliveryTimeEstimation(
    val estimatedPickupDuration: Duration,
    val estimatedDeliveryDuration: Duration,
) {
    val totalDuration: Duration
        get() = estimatedPickupDuration.plus(estimatedDeliveryDuration)
}

