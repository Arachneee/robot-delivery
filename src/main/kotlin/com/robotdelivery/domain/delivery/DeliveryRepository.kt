package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.DeliveryId

interface DeliveryRepository {
    fun findById(id: DeliveryId): Delivery?

    fun findPendingDeliveries(): List<Delivery>

    fun save(delivery: Delivery): Delivery
}

