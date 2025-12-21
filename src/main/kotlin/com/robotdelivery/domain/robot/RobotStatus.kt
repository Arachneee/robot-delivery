package com.robotdelivery.domain.robot

enum class RobotStatus {
    READY,
    BUSY,
    OFF_DUTY,
    ;

    fun isAvailableForDelivery(): Boolean = this == READY

    fun canTransitionTo(nextStatus: RobotStatus): Boolean =
        when (this) {
            READY -> nextStatus in listOf(BUSY, OFF_DUTY)
            BUSY -> nextStatus == READY
            OFF_DUTY -> nextStatus == READY
        }
}
