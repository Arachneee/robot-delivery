package com.robotdelivery.domain.delivery.event

import org.springframework.data.jpa.repository.JpaRepository

interface DeliveryEventHistoryRepository : JpaRepository<DeliveryEventHistory, Long> {
    fun findAllByDeliveryIdOrderByOccurredAtAsc(deliveryId: Long): List<DeliveryEventHistory>
}

