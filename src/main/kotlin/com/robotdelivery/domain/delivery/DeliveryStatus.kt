package com.robotdelivery.domain.delivery

import java.util.EnumSet

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

    fun requiresReturn(): Boolean = this in REQUIRES_RETURN_STATUSES

    fun isCancelable(): Boolean = this in CANCELABLE_STATUSES

    fun isUnassignable(): Boolean = this in UNASSIGNABLE_STATUSES

    fun isReassignable(): Boolean = this in REASSIGNABLE_STATUSES

    fun isTerminal(): Boolean = this in TERMINAL_STATUSES

    companion object {
        private val REQUIRES_RETURN_STATUSES: Set<DeliveryStatus> =
            EnumSet.of(PICKING_UP, DELIVERING, DELIVERY_ARRIVED, DROPPING_OFF)

        private val CANCELABLE_STATUSES: Set<DeliveryStatus> =
            EnumSet.of(PENDING, ASSIGNED, PICKUP_ARRIVED)

        private val UNASSIGNABLE_STATUSES: Set<DeliveryStatus> =
            EnumSet.of(ASSIGNED, PICKUP_ARRIVED, PICKING_UP)

        private val REASSIGNABLE_STATUSES: Set<DeliveryStatus> =
            EnumSet.of(ASSIGNED, PICKUP_ARRIVED, PICKING_UP, DELIVERING, DELIVERY_ARRIVED, DROPPING_OFF)

        private val TERMINAL_STATUSES: Set<DeliveryStatus> =
            EnumSet.of(COMPLETED, CANCELED, RETURN_COMPLETED)
    }
}
