package com.robotdelivery.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DeliveryId(
    @Column(name = "delivery_id", nullable = false)
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
