package com.robotdelivery.domain.delivery

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKUP_ARRIVED,
    PICKING_UP,
    DELIVERING,
    DELIVERY_ARRIVED,
    DROPPING_OFF,
    COMPLETED,
    CANCELED,
    RETURNING,
    RETURN_ARRIVED,
    RETURNING_OFF,
    RETURN_COMPLETED;

    fun canTransitionTo(nextStatus: DeliveryStatus): Boolean {
        return when (this) {
            PENDING -> nextStatus in listOf(ASSIGNED, CANCELED)
            ASSIGNED -> nextStatus in listOf(PICKUP_ARRIVED, CANCELED)
            PICKUP_ARRIVED -> nextStatus in listOf(PICKING_UP, CANCELED)
            PICKING_UP -> nextStatus in listOf(DELIVERING, RETURNING)
            DELIVERING -> nextStatus in listOf(DELIVERY_ARRIVED, RETURNING)
            DELIVERY_ARRIVED -> nextStatus in listOf(DROPPING_OFF, RETURNING)
            DROPPING_OFF -> nextStatus in listOf(COMPLETED, RETURNING)
            COMPLETED -> false
            CANCELED -> false
            RETURNING -> nextStatus == RETURN_ARRIVED
            RETURN_ARRIVED -> nextStatus == RETURNING_OFF
            RETURNING_OFF -> nextStatus == RETURN_COMPLETED
            RETURN_COMPLETED -> false
        }
    }

    fun isCancelable(): Boolean {
        return this in listOf(PENDING, ASSIGNED, PICKUP_ARRIVED)
    }

    fun requiresReturn(): Boolean {
        return this in listOf(PICKING_UP, DELIVERING, DELIVERY_ARRIVED, DROPPING_OFF)
    }
}
