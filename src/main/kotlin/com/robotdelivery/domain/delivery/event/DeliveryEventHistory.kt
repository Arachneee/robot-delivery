package com.robotdelivery.domain.delivery.event

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "delivery_event_histories")
class DeliveryEventHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(nullable = false)
    val deliveryId: Long,
    @Column(nullable = false)
    val eventType: String,
    @Column(nullable = true)
    val robotId: Long? = null,
    @Column(nullable = false)
    val occurredAt: LocalDateTime,
) {
    companion object {
        fun from(event: DeliveryEvent): DeliveryEventHistory =
            DeliveryEventHistory(
                deliveryId = event.deliveryId.value,
                eventType = event::class.simpleName ?: "Unknown",
                robotId = event.robotId?.value,
                occurredAt = event.occurredAt,
            )
    }
}
