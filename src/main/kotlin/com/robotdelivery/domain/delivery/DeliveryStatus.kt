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
    RETURN_COMPLETED,
    ;

    fun canTransitionTo(nextStatus: DeliveryStatus): Boolean =
        when (this) {
            PENDING -> nextStatus in listOf(ASSIGNED, CANCELED)
            ASSIGNED -> nextStatus in listOf(PICKUP_ARRIVED, CANCELED, PENDING)
            PICKUP_ARRIVED -> nextStatus in listOf(PICKING_UP, CANCELED, PENDING)
            PICKING_UP -> nextStatus in listOf(DELIVERING, RETURNING, PENDING)
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

    fun requiresReturn(): Boolean = this in listOf(PICKING_UP, DELIVERING, DELIVERY_ARRIVED, DROPPING_OFF)

    fun isCancelable(): Boolean = this in listOf(PENDING, ASSIGNED, PICKUP_ARRIVED)

    fun isUnassignable(): Boolean = this in listOf(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKUP_ARRIVED, DeliveryStatus.PICKING_UP)

    fun isReassignable(): Boolean = this in listOf(ASSIGNED, PICKUP_ARRIVED, PICKING_UP, DELIVERING, DELIVERY_ARRIVED, DROPPING_OFF)
}
