package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.DeliveryId
import org.springframework.data.jpa.repository.JpaRepository

interface DeliveryRepository : JpaRepository<Delivery, Long> {
    fun findById(id: DeliveryId): Delivery? = findById(id.value).orElse(null)

    fun findAllByStatus(deliveryStatus: DeliveryStatus): List<Delivery>
}

fun DeliveryRepository.getById(id: DeliveryId): Delivery =
    findById(id) ?: throw IllegalArgumentException("배달을 찾을 수 없습니다: $id")
